/* AI 运行审计：优先读取后端持久化 AI Run，护栏目录只展示策略覆盖。 */

const auditState = { runs: [], keyword: "", outcome: "" };

function normalizeRuns(payload) {
  if (Array.isArray(payload)) return payload;
  return payload?.items || payload?.content || [];
}

function runId(run) {
  return run.requestId || run.runId || run.id;
}

function runOutcome(run) {
  if (["FAILED", "REJECTED"].includes(run.status)) return "ERROR";
  if (run.status === "RUNNING") return "RUNNING";
  if (run.status === "CACHED") return "CACHED";
  if (run.outcome) return run.outcome;
  if (run.error) return "ERROR";
  if (run.handoffRequired || run.handoff) return "HANDOFF";
  return "AGENT_REVIEW";
}

function outcomePill(outcome) {
  const labels = { AUTO_REPLY: "回复建议", AGENT_REVIEW: "建议已生成", RUNNING: "执行中", CACHED: "缓存命中", HANDOFF: "转人工", ERROR: "异常" };
  const colors = { AUTO_REPLY: "green", AGENT_REVIEW: "blue", RUNNING: "blue", CACHED: "green", HANDOFF: "orange", ERROR: "red" };
  return pill(labels[outcome] || outcome, colors[outcome] || "neutral");
}

function filteredRuns() {
  const keyword = auditState.keyword.trim().toLowerCase();
  return auditState.runs.filter(run => {
    const searchable = [runId(run), run.conversationId, run.sessionId, run.intent, run.model, run.provider].filter(Boolean).join(" ").toLowerCase();
    return (!keyword || searchable.includes(keyword)) && (!auditState.outcome || runOutcome(run) === auditState.outcome);
  });
}

function renderRunMetrics() {
  const runs = auditState.runs;
  const withLatency = runs.filter(run => Number.isFinite(Number(run.elapsedMs || run.latencyMs)));
  const average = withLatency.length ? Math.round(withLatency.reduce((sum, run) => sum + Number(run.elapsedMs || run.latencyMs), 0) / withLatency.length) : null;
  const guardrailHits = runs.filter(run => (run.guardrails || run.guardrailHits || run.guardrailSummaries || []).length > 0).length;
  const handoffs = runs.filter(run => runOutcome(run) === "HANDOFF").length;
  $("#auditStatRuns").textContent = String(runs.length);
  $("#auditStatLatency").textContent = average === null ? "--" : `${average} ms`;
  $("#auditStatGuardrail").textContent = String(guardrailHits);
  $("#auditStatHandoff").textContent = String(handoffs);
}

function renderRuns() {
  const runs = filteredRuns();
  $("#auditResultCount").textContent = `${runs.length} 条`;
  if (!runs.length) {
    $("#auditRunList").innerHTML = emptyState("还没有可查询的 AI Run", "在会话工作台生成一次 AI 建议后，requestId 和证据链会保存到这里");
    return;
  }
  $("#auditRunList").innerHTML = runs.map(run => {
    const outcome = runOutcome(run);
    const guardrails = run.guardrails || run.guardrailHits || run.guardrailSummaries || [];
    return `<button class="audit-run" type="button" data-run-id="${escapeHtml(runId(run))}">
      <code>${escapeHtml(runId(run))}</code>
      <span><strong>${escapeHtml(run.intent || "UNKNOWN")}</strong><br>${escapeHtml(run.conversationId || run.sessionId || "--")}</span>
      <span>${escapeHtml(run.model || run.modelRuntime || run.provider || "--")}</span>
      <span>${run.elapsedMs ?? run.latencyMs ?? "--"} ms</span>
      <span>${outcomePill(outcome)}${guardrails.length ? `<small class="text-danger" style="display:block;margin-top:3px">${guardrails.length} 项护栏</small>` : ""}</span>
      ${icon("chevron", 14)}
    </button>`;
  }).join("");
  $$('[data-run-id]').forEach(button => button.addEventListener("click", () => openRun(button.dataset.runId)));
}

function renderRuntimeCard(runtime) {
  $("#auditRuntime").innerHTML = `<div class="detail-list">
    <div class="detail-row"><span>Provider</span><strong>${escapeHtml(runtime?.provider || "--")}</strong></div>
    <div class="detail-row"><span>Model</span><strong>${escapeHtml(runtime?.model || "--")}</strong></div>
    <div class="detail-row"><span>Base URL</span><strong class="mono">${escapeHtml(runtime?.baseUrl || "--")}</strong></div>
    <div class="detail-row"><span>业务存储</span><strong>${escapeHtml(runtime?.storage || "--")}</strong></div>
    <div class="detail-row"><span>配置状态</span><strong class="${runtime?.configured === false ? "text-danger" : "text-success"}">${runtime?.configured === false ? "未就绪" : "可调用"}</strong></div>
  </div>`;
}

function renderGuardrails(items) {
  $("#guardrailCoverage").textContent = `${items.length} 类`;
  if (!items.length) {
    $("#guardrailList").innerHTML = emptyState("没有护栏目录", "后端暂未返回治理策略");
    return;
  }
  $("#guardrailList").innerHTML = items.map(item => `
    <article class="guardrail-card"><header><strong>${escapeHtml(item.problem || item.name || item.type)}</strong>${pill("已启用", "green")}</header><p>${escapeHtml(item.solution || item.strategy || item.description || "由 Java 风险策略在模型回复前执行")}</p></article>`).join("");
}

function renderRunDetail(run) {
  const sources = run.sources || run.sourceSummaries || [];
  const traces = run.toolTraces || run.tools || run.toolSummaries || [];
  const guardrails = run.guardrails || run.guardrailHits || run.guardrailSummaries || [];
  const outcome = runOutcome(run);
  const hasError = outcome === "ERROR";
  const hasHandoff = outcome === "HANDOFF";
  const hasGuardrails = guardrails.length > 0;
  const hasSources = sources.length > 0;
  const hasTraces = traces.length > 0;

  // Build trace timeline steps
  const traceSteps = [];
  traceSteps.push({ title: "接收请求", body: `意图: ${escapeHtml(run.intent || "UNKNOWN")} · 置信度 ${(run.intentConfidence ?? run.confidence) === undefined ? "--" : `${Math.round((run.intentConfidence ?? run.confidence) * 100)}%`}`, status: "success" });
  if (hasSources) {
    traceSteps.push({ title: "知识检索", body: `命中 ${sources.length} 条知识证据`, status: "success" });
  }
  if (hasTraces) {
    traceSteps.push({ title: "工具调用", body: `执行 ${traces.length} 个业务工具`, status: "success" });
  }
  if (hasGuardrails) {
    traceSteps.push({ title: "风险护栏", body: `命中 ${guardrails.length} 项治理策略`, status: hasHandoff ? "warning" : "success" });
  }
  if (hasHandoff) {
    traceSteps.push({ title: "转人工处理", body: "Java 护栏要求人工介入", status: "warning" });
  } else if (hasError) {
    traceSteps.push({ title: "运行异常", body: "模型调用或执行过程中出现错误", status: "error" });
  } else if (outcome === "RUNNING") {
    traceSteps.push({ title: "执行中", body: "尚未收到终态；长期不结束时需检查进程或审计写入", status: "active" });
  } else {
    traceSteps.push({ title: "生成建议", body: "建议仍需坐席确认后发送", status: "active" });
  }

  const traceHtml = traceSteps.map((step, index) => `
    <div class="trace-step ${step.status}${index === traceSteps.length - 1 ? " active" : ""}">
      <div class="trace-step-header">
        <span class="trace-step-title">${escapeHtml(step.title)}</span>
        ${index === traceSteps.length - 1 ? '<span class="trace-step-time">最终</span>' : ""}
      </div>
      <div class="trace-step-body">${escapeHtml(step.body)}</div>
    </div>
  `).join("");

  $("#runModalTitle").textContent = runId(run);
  $("#runModalSubtitle").textContent = `${run.conversationId || run.sessionId || "--"} · ${formatDate(run.createdAt || run.receivedAt, true)}`;
  $("#runModalBody").innerHTML = `
    <div class="definition-grid">
      <div class="definition-item"><span>运行状态</span><strong>${escapeHtml(run.status || "legacy")}</strong></div>
      <div class="definition-item"><span>错误码</span><strong>${escapeHtml(run.errorCode || "--")}</strong></div>
      <div class="definition-item"><span>Prompt 版本</span><strong>${escapeHtml(run.promptVersion || "--")}</strong></div>
      <div class="definition-item"><span>输入 / 输出 Token</span><strong>${run.inputTokens ?? "未知"} / ${run.outputTokens ?? "未知"}</strong></div>
      <div class="definition-item"><span>模型</span><strong>${escapeHtml(run.model || run.modelRuntime || run.provider || "--")}</strong></div>
      <div class="definition-item"><span>耗时</span><strong>${run.elapsedMs ?? run.latencyMs ?? "--"} ms</strong></div>
      <div class="definition-item"><span>识别意图</span><strong>${escapeHtml(run.intent || "UNKNOWN")}</strong></div>
      <div class="definition-item"><span>置信度</span><strong>${(run.intentConfidence ?? run.confidence) === undefined ? "--" : `${Math.round((run.intentConfidence ?? run.confidence) * 100)}%`}</strong></div>
      <div class="definition-item"><span>最终结果</span><strong>${outcomePill(outcome)}</strong></div>
      <div class="definition-item"><span>队列深度</span><strong>${run.queueDepth ?? "--"}</strong></div>
    </div>
    <hr style="border:0;border-top:1px solid var(--ink-100);margin:17px 0">
    <h4 style="font-size:11px;margin-bottom:8px">处理链路</h4>
    <div class="trace-timeline">${traceHtml}</div>
    <hr style="border:0;border-top:1px solid var(--ink-100);margin:17px 0">
    <h4 style="font-size:11px;margin-bottom:8px">知识证据（${sources.length}）</h4>
    ${sources.length ? sources.map(source => typeof source === "string" ? `<div class="retrieval-result"><p style="margin-top:0">${escapeHtml(source)}</p></div>` : `<div class="retrieval-result"><div class="retrieval-result-head"><strong>${escapeHtml(source.title)}</strong>${pill(source.version || "--", "violet")}</div><p>${escapeHtml(source.text || source.content || "")}</p></div>`).join("") : '<p class="subtle">没有知识证据</p>'}
    <h4 style="font-size:11px;margin:16px 0 8px">工具调用（${traces.length}）</h4>
    ${traces.length ? `<div class="detail-list">${traces.map(trace => typeof trace === "string" ? `<div class="detail-row"><span>业务工具</span><strong>${escapeHtml(trace)}</strong></div>` : `<div class="detail-row"><span>${escapeHtml(trace.toolName || trace.tool)}</span><strong>${escapeHtml(trace.result || trace.summary)}</strong></div>`).join("")}</div>` : '<p class="subtle">没有工具调用</p>'}
    <h4 style="font-size:11px;margin:16px 0 8px">风险护栏（${guardrails.length}）</h4>
    ${guardrails.length ? `<div class="guardrail-list">${guardrails.map(item => typeof item === "string" ? `<div class="guardrail-card"><header><strong>风险策略</strong>${pill("已处理", "green")}</header><p>${escapeHtml(item)}</p></div>` : `<div class="guardrail-card"><header><strong>${escapeHtml(item.problem || item.type)}</strong>${item.requiresHandoff ? pill("需人工", "red") : pill("已处理", "green")}</header><p>${escapeHtml(item.strategy || item.evidence || "")}</p></div>`).join("")}</div>` : '<p class="subtle">本次运行未命中护栏</p>'}`;
}

async function openRun(id) {
  let run = auditState.runs.find(item => runId(item) === id);
  try {
    const detail = await request(`/api/support/ai-runs/${encodeURIComponent(id)}`);
    if (detail) run = detail;
  } catch { /* 列表数据已经足够展示，详情接口不是硬依赖 */ }
  renderRunDetail(run);
  $("#runModal").classList.add("open");
}

function closeRunModal() { $("#runModal").classList.remove("open"); }

async function loadAudit() {
  const button = $("#refreshAudit");
  setBusy(button, true, "同步中");
  const [runsResult, guardrailsResult, runtimeResult] = await Promise.allSettled([
    request("/api/support/ai-runs"),
    request("/api/support/production/issues"),
    request("/api/support/runtime")
  ]);
  auditState.runs = runsResult.status === "fulfilled" ? normalizeRuns(runsResult.value) : [];
  if (!auditState.runs.length) {
    const lastDecision = loadLastDecision();
    if (lastDecision?.requestId) auditState.runs = [{ ...lastDecision, createdAt: lastDecision.receivedAt }];
  }
  renderRunMetrics();
  renderRuns();
  renderGuardrails(guardrailsResult.status === "fulfilled" ? guardrailsResult.value : []);
  renderRuntimeCard(runtimeResult.status === "fulfilled" ? runtimeResult.value : state.runtime || {});
  setBusy(button, false);
}

initPage("governance");
$("#refreshAudit").innerHTML = `${icon("refresh", 15)}同步记录`;
$("#auditSearchIcon").innerHTML = icon("search", 15);
$("#auditIconRuns").innerHTML = icon("pulse", 17);
$("#auditIconLatency").innerHTML = icon("clock", 17);
$("#auditIconGuardrail").innerHTML = icon("check", 17);
$("#auditIconHandoff").innerHTML = icon("user", 17);
$("#closeRunModal").innerHTML = icon("close", 17);
loadAudit();

$("#auditKeyword")?.addEventListener("input", event => { auditState.keyword = event.target.value; renderRuns(); });
$("#auditOutcomeFilter")?.addEventListener("change", event => { auditState.outcome = event.target.value; renderRuns(); });
$("#refreshAudit")?.addEventListener("click", loadAudit);
$("#closeRunModal")?.addEventListener("click", closeRunModal);
$("#runModal")?.addEventListener("click", event => { if (event.target === event.currentTarget) closeRunModal(); });
