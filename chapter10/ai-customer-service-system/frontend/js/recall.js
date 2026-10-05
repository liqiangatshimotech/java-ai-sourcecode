/* RAG 评测：预期文档让单次检索结果具备可验证标准。 */

const evaluationCases = [
  { label: "高价值订单发货拦截", query: "企业客户的订单已经进仓拣货，现在还能直接退款吗？", expected: "KB-REFUND-001" },
  { label: "物流长时间未更新", query: "物流超过一天没有更新，我应该怎么处理？", expected: "KB-SHIPPING-001" },
  { label: "企业专票校验", query: "公司申请增值税专票需要准备哪些资料？", expected: "KB-INVOICE-001" },
  { label: "不确定政策时的回复边界", query: "知识库没有写清楚赔付金额，客服可以自己承诺吗？", expected: "KB-AI-GUARDRAIL-001" }
];

function renderExamples() {
  $("#evalExamples").innerHTML = evaluationCases.map((item, index) => `<button class="eval-example" type="button" data-case="${index}"><strong>${escapeHtml(item.label)}</strong><br>${escapeHtml(item.query)}</button>`).join("");
  $$('[data-case]').forEach(button => button.addEventListener("click", () => {
    const item = evaluationCases[Number(button.dataset.case)];
    $("#knowledgeQuery").value = item.query;
    $("#expectedDocument").value = item.expected;
  }));
}

function renderEvaluationSummary(results, expected, elapsedMs) {
  const expectedIndex = results.findIndex(item => item.documentId === expected);
  const hit = expectedIndex >= 0;
  const first = expectedIndex === 0;
  const bestScore = results[0]?.score;
  $("#evalSummary").innerHTML = `
    <div class="surface-head"><div><h2>本次评测结论</h2><p>以 ${escapeHtml(expected || "未设置预期文档")} 作为黄金答案。</p></div>${expected ? (hit ? pill("Hit@K 通过", "green") : pill("未命中", "red")) : pill("探索模式", "blue")}</div>
    <div class="surface-body">
      <div class="queue-overview" style="padding:0;grid-template-columns:repeat(3,1fr)">
        <div class="queue-stat"><span>预期文档命中</span><strong class="${hit ? "text-success" : "text-danger"}">${expected ? (hit ? "是" : "否") : "--"}</strong></div>
        <div class="queue-stat"><span>首位命中</span><strong class="${first ? "text-success" : ""}">${expected ? (first ? "是" : "否") : "--"}</strong></div>
        <div class="queue-stat"><span>最高相似度</span><strong>${bestScore === undefined ? "--" : Number(bestScore).toFixed(3)}</strong></div>
      </div>
      <div class="health-strip" style="margin:13px 0 0;${hit ? "" : "background:var(--red-50);border-color:var(--red-100);color:var(--red-700)"}">
        <div class="health-title">${hit ? icon("check", 14) + " 检索符合预期" : icon("pulse", 14) + " 需要调整召回"}</div>
        <span>${hit ? `预期知识位于第 ${expectedIndex + 1} 位，本次检索耗时 ${elapsedMs} ms。` : "预期知识没有进入结果，需要检查切分、向量模型、阈值或重排策略。"}</span>
      </div>
    </div>`;
}

function renderResults(results, expected) {
  $("#retrievalCount").textContent = `${results.length} 条`;
  if (!results.length) {
    $("#knowledgeResult").innerHTML = emptyState("没有召回结果", "检查是否存在已发布并同步向量的知识");
    return;
  }
  const maxScore = Math.max(...results.map(item => Number(item.score || 0)), 1);
  $("#knowledgeResult").innerHTML = results.map((item, index) => {
    const isExpected = expected && item.documentId === expected;
    return `<article class="retrieval-result" style="${isExpected ? "border-color:#8fd5b9;background:var(--green-50)" : ""}">
      <div class="retrieval-result-head"><span class="retrieval-rank">${index + 1}</span><strong>${escapeHtml(item.title)}</strong>${isExpected ? pill("预期答案", "green") : ""}<span class="score-meter"><i style="--score:${Math.max(4, (Number(item.score || 0) / maxScore) * 100)}%"></i>${Number(item.score).toFixed(3)}</span></div>
      <p>${escapeHtml(item.text)}</p>
      <footer><span class="mono">${escapeHtml(item.documentId)}</span><span>${escapeHtml(item.version)}</span><span>仅 PUBLISHED 状态参与检索</span></footer>
    </article>`;
  }).join("");
}

async function searchKnowledge() {
  const query = $("#knowledgeQuery").value.trim();
  const expected = $("#expectedDocument").value.trim();
  const limit = Number($("#knowledgeLimit").value || 3);
  if (!query) return showToast("请输入要验证的客户问题", "error");
  const button = $("#searchKnowledge");
  setBusy(button, true, "向量检索中");
  $("#knowledgeResult").innerHTML = '<div class="empty-state"><span class="spinner"></span><p>正在生成查询向量并检索已发布知识</p></div>';
  const started = performance.now();
  try {
    const data = await request(`/api/support/knowledge/search?query=${encodeURIComponent(query)}&limit=${encodeURIComponent(limit)}`);
    const elapsedMs = Math.round(performance.now() - started);
    const results = data.results || [];
    renderResults(results, expected);
    renderEvaluationSummary(results, expected, elapsedMs);
    $("#retrievalMeta").textContent = `查询“${data.query || query}” · Top ${limit} · ${elapsedMs} ms`;
  } catch (error) {
    renderError($("#knowledgeResult"), error, searchKnowledge);
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

initPage("recall");
$("#searchKnowledge").innerHTML = `${icon("flask", 15)}运行检索评测`;
$("#evalEmptyIcon").innerHTML = icon("flask", 22);
$("#resultEmptyIcon").innerHTML = icon("search", 22);
renderExamples();
$("#searchKnowledge")?.addEventListener("click", searchKnowledge);
$("#knowledgeQuery")?.addEventListener("keydown", event => { if ((event.metaKey || event.ctrlKey) && event.key === "Enter") searchKnowledge(); });
