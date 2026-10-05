/* 运营驾驶舱：所有指标都从会话、工单和运行配置聚合，不再展示静态功能入口。 */

function numberValue(...values) {
  const value = values.find(item => item !== undefined && item !== null && item !== "");
  return value === undefined ? null : Number(value);
}

function setMetric(id, value, suffix = "") {
  const element = document.getElementById(id);
  if (!element) return;
  element.innerHTML = value === null || Number.isNaN(value)
    ? "--"
    : `${value}${suffix ? `<small>${escapeHtml(suffix)}</small>` : ""}`;
}

function setupMetricIcons() {
  $("#metricIconWaiting").innerHTML = icon("inbox", 16);
  $("#metricIconActive").innerHTML = icon("user", 16);
  $("#metricIconSla").innerHTML = icon("clock", 16);
  $("#metricIconResponse").innerHTML = icon("pulse", 16);
  $("#metricIconAi").innerHTML = "✦";
  $("#metricIconCsat").innerHTML = icon("check", 16);
  $("#refreshDashboard").innerHTML = `${icon("refresh", 15)}刷新数据`;
}

function normalizeConversations(payload) {
  if (Array.isArray(payload)) return payload;
  return payload?.items || payload?.content || [];
}

function byStatus(conversations, ...statuses) {
  return conversations.filter(item => statuses.includes(item.status)).length;
}

function dashboardValue(dashboard, conversations, candidates, fallbackStatuses = []) {
  const direct = numberValue(...candidates.map(key => dashboard?.[key]));
  return direct === null && fallbackStatuses.length ? byStatus(conversations, ...fallbackStatuses) : direct;
}

function renderMetrics(dashboard, conversations, ticketDashboard) {
  const waiting = dashboardValue(dashboard, conversations, ["waitingCount", "waiting"], ["WAITING"]);
  const active = dashboardValue(dashboard, conversations, ["inProgressCount", "activeCount", "inProgress"], ["IN_PROGRESS", "ACTIVE", "AI_ASSISTING"]);
  const urgent = dashboardValue(dashboard, conversations, ["urgentCount"], ["URGENT"]);
  const slaRisk = numberValue(dashboard?.slaRiskCount, dashboard?.overdueCount, ticketDashboard?.breachedCount);
  const averageResponse = numberValue(dashboard?.averageFirstResponseSeconds, dashboard?.avgFirstResponseSeconds);
  const aiRate = numberValue(dashboard?.aiAssistRate, dashboard?.aiAdoptionRate);
  const csat = numberValue(dashboard?.csatScore, dashboard?.customerSatisfactionScore);

  setMetric("metricWaiting", waiting);
  setMetric("metricActive", active);
  setMetric("metricSla", slaRisk);
  if (averageResponse === null) {
    setMetric("metricResponse", null);
  } else if (averageResponse >= 60) {
    setMetric("metricResponse", Math.round(averageResponse / 60), "分");
  } else {
    setMetric("metricResponse", Math.round(averageResponse), "秒");
  }
  setMetric("metricAi", aiRate === null ? null : Math.round(aiRate > 1 ? aiRate : aiRate * 100), "%");
  setMetric("metricCsat", csat === null ? null : csat.toFixed(1), "/ 5");

  // Trend indicators and card styling
  const waitingCard = $("#metricCardWaiting");
  const activeCard = $("#metricCardActive");
  const slaCard = $("#metricCardSla");
  const responseCard = $("#metricCardResponse");
  const aiCard = $("#metricCardAi");
  const csatCard = $("#metricCardCsat");

  // Waiting: up = bad (more waiting), down = good
  waitingCard.className = `metric-card card-hover-lift ${waiting > 3 ? "trend-up" : "trend-neutral"}`;
  $("#metricWaitingHint").innerHTML = waiting === 0 ? "队列已清空" : `最长等待 ${formatDuration(dashboard?.longestWaitSeconds || 0)}${waiting > 3 ? '<span class="metric-trend up">↑ 积压</span>' : ''}`;

  // Active: neutral trend
  activeCard.className = "metric-card card-hover-lift trend-neutral";
  $("#metricActiveHint").textContent = `${dashboard?.onlineAgentCount ?? 1} 位坐席在线`;

  // SLA: up = bad (more risk), down = good
  slaCard.className = `metric-card card-hover-lift ${slaRisk ? "trend-up" : "trend-down"}`;
  $("#metricSlaHint").innerHTML = slaRisk ? `${slaRisk} 项需要立即处理<span class="metric-trend up">↑ 风险</span>` : '当前没有超时风险<span class="metric-trend down">↓ 正常</span>';

  // Response time: down = good (faster), up = bad (slower)
  const responseGood = averageResponse !== null && averageResponse <= 60;
  responseCard.className = `metric-card card-hover-lift ${responseGood ? "trend-down" : "trend-up"}`;
  $("#metricResponseHint").innerHTML = responseGood ? '达到 60 秒服务目标<span class="metric-trend down">↓ 达标</span>' : '目标：60 秒内<span class="metric-trend up">↑ 偏慢</span>';

  // AI rate: up = good
  const aiGood = aiRate !== null && aiRate > 0.3;
  aiCard.className = `metric-card card-hover-lift ${aiGood ? "trend-up" : "trend-neutral"}`;
  $("#metricAiHint").innerHTML = aiRate === null ? "暂无采纳记录" : `来自最终发送内容对比${aiGood ? '<span class="metric-trend up">↑ 良好</span>' : ''}`;

  // CSAT: up = good
  const csatGood = csat !== null && csat >= 4.0;
  csatCard.className = `metric-card card-hover-lift ${csatGood ? "trend-up" : csat !== null ? "trend-down" : "trend-neutral"}`;
  $("#metricCsatHint").innerHTML = `${dashboard?.feedbackCount ?? 0} 条有效评价${csatGood ? '<span class="metric-trend up">↑ 优秀</span>' : csat !== null ? '<span class="metric-trend down">↓ 待提升</span>' : ''}`;
}

function renderVolumeChart(conversations) {
  const hours = [8, 10, 12, 14, 16, 18, 20];
  const counts = hours.map((hour, index) => {
    const nextHour = hours[index + 1] ?? 24;
    return conversations.filter(item => {
      const date = new Date(item.updatedAt || item.lastMessageAt || item.createdAt);
      return !Number.isNaN(date.getTime()) && date.getHours() >= hour && date.getHours() < nextHour;
    }).length;
  });
  const max = Math.max(...counts, 1);
  const plotLeft = 25;
  const plotWidth = 620;
  const plotTop = 15;
  const plotHeight = 145;
  const points = counts.map((count, index) => {
    const x = plotLeft + index * (plotWidth / (counts.length - 1));
    const y = plotTop + plotHeight - (count / max) * (plotHeight - 12);
    return { x, y, count };
  });
  const pointText = points.map(point => `${point.x},${point.y}`).join(" ");
  const areaText = `${plotLeft},${plotTop + plotHeight} ${pointText} ${plotLeft + plotWidth},${plotTop + plotHeight}`;
  const yLabels = [max, Math.round(max / 2), 0];
  const gridY = [plotTop, plotTop + plotHeight / 2, plotTop + plotHeight];
  $("#volumeChart").innerHTML = `
    <svg viewBox="0 0 680 190" role="img" aria-label="会话流入趋势">
      <defs><linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2f7af2" stop-opacity=".2"/><stop offset="1" stop-color="#2f7af2" stop-opacity="0"/></linearGradient></defs>
      ${gridY.map((y, index) => `<line class="chart-grid-line" x1="${plotLeft}" x2="${plotLeft + plotWidth}" y1="${y}" y2="${y}"/><text class="chart-axis-label" x="2" y="${y + 3}">${yLabels[index]}</text>`).join("")}
      <polygon class="chart-area" points="${areaText}"/>
      <polyline class="chart-line" points="${pointText}"/>
      ${points.map(point => `<circle class="chart-dot" cx="${point.x}" cy="${point.y}" r="3.5"><title>${point.count} 个会话</title></circle>`).join("")}
      ${hours.map((hour, index) => `<text class="chart-axis-label" x="${plotLeft + index * (plotWidth / (hours.length - 1)) - 8}" y="183">${String(hour).padStart(2, "0")}:00</text>`).join("")}
    </svg>`;
}

function renderQueues(dashboard, conversations) {
  const rows = [
    { label: "待领取", value: dashboardValue(dashboard, conversations, ["waitingCount"], ["WAITING"]), color: "#d98a17" },
    { label: "服务中", value: dashboardValue(dashboard, conversations, ["inProgressCount", "activeCount"], ["IN_PROGRESS", "ACTIVE"]), color: "#2f7af2" },
    { label: "等待客户", value: dashboardValue(dashboard, conversations, ["waitingCustomerCount"], ["WAITING_CUSTOMER"]), color: "#1d94aa" }
  ];
  const max = Math.max(...rows.map(row => row.value || 0), 1);
  $("#queueOverview").innerHTML = rows.map(row => `
    <div class="queue-stat">
      <span>${row.label}</span>
      <strong>${row.value ?? "--"}</strong>
      <div class="progress"><i style="width:${Math.max(6, ((row.value || 0) / max) * 100)}%;background:${row.color}"></i></div>
    </div>`).join("");
}

function renderAlerts(conversations, tickets) {
  const urgentConversations = conversations
    .filter(item => item.priority === "URGENT" || item.slaBreached || item.slaStatus === "BREACHED")
    .slice(0, 3)
    .map(item => ({
      type: "conversation",
      title: `${item.customerName || item.customer?.displayName || item.customerId} · ${priorityLabel(item.priority)}`,
      text: item.lastMessage || item.lastMessagePreview || item.subject || "会话等待人工处理",
      time: item.updatedAt,
      href: `./reception.html?conversationId=${encodeURIComponent(item.conversationId || item.id)}`
    }));
  const riskyTickets = tickets
    .filter(item => item.slaBreached || item.slaStatus === "DUE_SOON" || item.priority === "URGENT")
    .slice(0, 3)
    .map(item => ({
      type: "ticket",
      title: `${item.ticketId} · ${item.slaBreached ? "已超时" : "即将超时"}`,
      text: item.question || "工单需要处理",
      time: item.updatedAt,
      href: `./ticket-detail.html?ticketId=${encodeURIComponent(item.ticketId)}`
    }));
  const alerts = [...urgentConversations, ...riskyTickets].slice(0, 4);
  $("#alertCount").textContent = String(alerts.length);
  if (!alerts.length) {
    $("#alertList").innerHTML = emptyState("当前没有高风险事项", "所有会话和工单均在 SLA 范围内");
    return;
  }
  $("#alertList").innerHTML = alerts.map((alert, index) => `
    <a class="alert-item" href="${alert.href}">
      <span class="alert-marker ${index > 1 ? "amber" : ""}">${icon(alert.type === "ticket" ? "ticket" : "clock", 14)}</span>
      <span class="alert-copy"><strong>${escapeHtml(alert.title)}</strong><p>${escapeHtml(alert.text)}</p><time>${formatDate(alert.time)}</time></span>
      ${icon("chevron", 14)}
    </a>`).join("");
}

function renderEfficiency(dashboard, conversations) {
  const rawRate = numberValue(dashboard?.aiAssistRate, dashboard?.aiAdoptionRate);
  const rate = rawRate === null
    ? Math.round((conversations.filter(item => item.aiAssisted || item.aiAssistEnabled).length / Math.max(conversations.length, 1)) * 100)
    : Math.round(rawRate > 1 ? rawRate : rawRate * 100);
  const handoff = numberValue(dashboard?.handoffRate);
  const autoResolved = numberValue(dashboard?.autoResolutionRate);
  $("#efficiencyCard").innerHTML = `
    <div class="donut" style="--value:${Math.min(100, rate)}"><strong>${rate}%</strong></div>
    <div class="efficiency-details">
      <div class="efficiency-row"><span>AI 建议覆盖</span><strong>${dashboard?.aiSuggestedCount ?? conversations.filter(item => item.aiAssisted).length}</strong></div>
      <div class="efficiency-row"><span>转人工率</span><strong>${handoff === null ? "--" : `${Math.round(handoff > 1 ? handoff : handoff * 100)}%`}</strong></div>
      <div class="efficiency-row"><span>自动解决率</span><strong>${autoResolved === null ? "--" : `${Math.round(autoResolved > 1 ? autoResolved : autoResolved * 100)}%`}</strong></div>
      <div class="efficiency-row"><span>模型平均耗时</span><strong>${dashboard?.averageModelLatencyMs ? `${dashboard.averageModelLatencyMs} ms` : "--"}</strong></div>
    </div>`;
}

function renderActivity(conversations) {
  const recent = [...conversations]
    .sort((a, b) => new Date(b.updatedAt || b.lastMessageAt) - new Date(a.updatedAt || a.lastMessageAt))
    .slice(0, 4);
  if (!recent.length) {
    $("#activityList").innerHTML = emptyState("暂无服务动态", "新会话进入后会显示在这里");
    return;
  }
  $("#activityList").innerHTML = recent.map(item => {
    const customer = item.customerName || item.customer?.displayName || item.customerId;
    return `<a class="activity-item" href="./reception.html?conversationId=${encodeURIComponent(item.conversationId || item.id)}">
      <span class="customer-avatar" style="width:28px;height:28px;border-radius:8px;font-size:9px">${escapeHtml(initials(customer))}</span>
      <span class="activity-copy"><strong>${escapeHtml(customer)} · ${conversationStatusLabel(item.status)}</strong><p>${escapeHtml(item.lastMessage || item.lastMessagePreview || item.subject || "会话状态已更新")}</p><time>${formatDate(item.updatedAt || item.lastMessageAt)}</time></span>
    </a>`;
  }).join("");
}

async function refreshDashboard() {
  const button = $("#refreshDashboard");
  setBusy(button, true, "同步中");
  try {
    const [dashboardResult, conversationResult, ticketResult, ticketDashboardResult, runtime] = await Promise.allSettled([
      request("/api/support/conversations/dashboard"),
      request("/api/support/conversations"),
      request("/api/support/tickets"),
      request("/api/support/tickets/dashboard"),
      loadRuntime()
    ]);
    const dashboard = dashboardResult.status === "fulfilled" ? dashboardResult.value : {};
    const conversations = conversationResult.status === "fulfilled" ? normalizeConversations(conversationResult.value) : [];
    const tickets = ticketResult.status === "fulfilled" ? ticketResult.value : [];
    const ticketDashboard = ticketDashboardResult.status === "fulfilled" ? ticketDashboardResult.value : {};
    state.dashboard = dashboard;
    state.conversations = conversations;
    state.tickets = tickets;

    renderMetrics(dashboard, conversations, ticketDashboard);
    renderVolumeChart(conversations);
    renderQueues(dashboard, conversations);
    renderAlerts(conversations, tickets);
    renderEfficiency(dashboard, conversations);
    renderActivity(conversations);

    const currentRuntime = runtime.status === "fulfilled" ? runtime.value : state.runtime;
    $("#modelHealth").textContent = currentRuntime?.configured === false ? "模型未配置，已切人工模式" : `${currentRuntime?.provider || "模型"} 可调用`;
    $("#syncTime").textContent = `最后同步 ${new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false })}`;
  } finally {
    setBusy(button, false);
  }
}

initPage("overview");
setupMetricIcons();
refreshDashboard();

$$('.segmented button').forEach(button => button.addEventListener("click", () => {
  $$('.segmented button').forEach(item => item.classList.remove("active"));
  button.classList.add("active");
  showToast("当前演示数据按会话时间聚合", "info");
}));

$("#refreshDashboard")?.addEventListener("click", refreshDashboard);
