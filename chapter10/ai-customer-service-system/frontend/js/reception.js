/*
 * 会话收件箱
 *
 * 页面不再手工填写 sessionId、客户和订单号，而是从后端队列领取现有会话。
 * AI 只生成建议草稿；真正发送、等待和解决都是可审计的人工动作。
 */

const OPERATOR = "support-linxiao";
const inboxState = {
  view: "ALL",
  keyword: "",
  selectedId: null,
  detail: null,
  orders: [],
  context: "customer",
  composerMode: "reply",
  aiDecision: null,
  action: null
};

function conversationId(item) {
  return item?.conversationId || item?.id || item?.sessionId;
}

function customerOf(item) {
  return item?.customer || {
    customerId: item?.customerId,
    displayName: item?.customerName,
    tier: item?.customerTier,
    phoneMasked: item?.phoneMasked
  };
}

function normalizeMessages(detail) {
  return (detail?.messages || []).map((message, index) => ({
    id: message.messageId || message.id || `${conversationId(detail)}-${index}`,
    role: String(message.role || message.senderType || message.type || "SYSTEM").toUpperCase(),
    sender: message.senderName || message.sender || message.operator || "系统",
    content: message.content || message.message || "",
    createdAt: message.createdAt || message.sentAt || detail.updatedAt,
    internal: message.internal === true || String(message.visibility || "").toUpperCase() === "INTERNAL" || String(message.role || "").toUpperCase() === "INTERNAL",
    deliveryStatus: message.deliveryStatus || "SENT"
  }));
}

function filteredConversations() {
  const keyword = inboxState.keyword.trim().toLowerCase();
  return state.conversations.filter(item => {
    const owner = item.owner || item.assignee || "";
    const matchesView = inboxState.view === "ALL"
      || (inboxState.view === "MY" && owner === OPERATOR)
      || (inboxState.view === "URGENT" && item.priority === "URGENT")
      || item.status === inboxState.view;
    const searchable = [
      conversationId(item),
      item.customerId,
      item.customerName,
      item.customer?.displayName,
      item.lastMessage,
      item.lastMessagePreview,
      item.subject,
      item.orderNo
    ].filter(Boolean).join(" ").toLowerCase();
    return matchesView && (!keyword || searchable.includes(keyword));
  });
}

function renderConversationList() {
  const items = filteredConversations();
  $("#conversationCount").textContent = String(items.length);
  if (!items.length) {
    $("#conversationList").innerHTML = emptyState("没有符合条件的会话", "可以切换队列或调整搜索条件");
    return;
  }
  $("#conversationList").innerHTML = items.map(item => {
    const customer = customerOf(item);
    const tierClass = customer?.tier === "ENTERPRISE" ? "enterprise" : customer?.tier === "VIP" ? "vip" : "";
    const id = conversationId(item);
    const waitSeconds = item.waitSeconds ?? item.waitingSeconds ?? (item.waitingMinutes === undefined ? undefined : item.waitingMinutes * 60);
    return `
      <button class="conversation-item${id === inboxState.selectedId ? " active" : ""}" type="button" data-conversation-id="${escapeHtml(id)}">
        <span class="customer-avatar ${tierClass}">${escapeHtml(initials(customer?.displayName || item.customerName || item.customerId))}<span class="channel-mini">${escapeHtml(channelLabel(item.channel).slice(0, 1))}</span></span>
        <span class="conversation-copy">
          <span class="conversation-line"><strong>${escapeHtml(customer?.displayName || item.customerName || item.customerId || "未知客户")}</strong><time>${formatTime(item.lastMessageAt || item.updatedAt)}</time></span>
          <span class="conversation-preview">${escapeHtml(item.lastMessage || item.lastMessagePreview || item.subject || "等待处理")}</span>
          <span class="conversation-meta">
            <span class="mini-tag">${escapeHtml(item.intent || channelLabel(item.channel))}</span>
            ${item.priority === "URGENT" || item.priority === "HIGH" ? `<span class="mini-tag text-danger">${priorityLabel(item.priority)}</span>` : ""}
            ${waitSeconds ? `<span class="wait-time">等待 ${formatDuration(waitSeconds)}</span>` : ""}
            ${item.unreadCount ? `<span class="unread-dot">${item.unreadCount}</span>` : ""}
          </span>
        </span>
      </button>`;
  }).join("");
  $$('[data-conversation-id]').forEach(button => button.addEventListener("click", () => selectConversation(button.dataset.conversationId)));
}

function roleClass(message) {
  if (message.internal) return "internal";
  if (["AGENT", "HUMAN", "OPERATOR"].includes(message.role)) return "agent";
  if (["AI", "ASSISTANT", "AI_DRAFT"].includes(message.role)) return "ai";
  if (["CUSTOMER", "USER"].includes(message.role)) return "customer";
  return "system";
}

function roleLabel(message) {
  const role = roleClass(message);
  return ({ agent: message.sender || "坐席", ai: "AI 助手", customer: message.sender || "客户", internal: "内部备注", system: "系统" })[role];
}

function renderMessages(detail) {
  const messages = normalizeMessages(detail);
  if (!messages.length) {
    $("#chatMessages").innerHTML = emptyState("这条会话还没有消息", "等待客户发送第一条消息");
    return;
  }
  let previousDay = "";
  $("#chatMessages").innerHTML = messages.map(message => {
    const day = new Date(message.createdAt).toLocaleDateString("zh-CN", { month: "long", day: "numeric" });
    const divider = day !== previousDay ? `<div class="timeline-divider">${escapeHtml(day)}</div>` : "";
    previousDay = day;
    const role = roleClass(message);
    if (role === "system" || role === "internal") {
      return `${divider}<div class="message-row ${role}"><div class="message-bubble">${escapeHtml(message.content)}</div></div>`;
    }
    const label = roleLabel(message);
    return `${divider}<div class="message-row ${role}">
      <span class="message-avatar">${role === "ai" ? "✦" : escapeHtml(initials(label))}</span>
      <div class="message-body"><div class="message-meta"><span>${escapeHtml(label)}</span><time>${formatTime(message.createdAt)}</time>${role === "agent" ? `<span>${message.deliveryStatus === "READ" ? "已读" : "已发送"}</span>` : ""}</div><div class="message-bubble">${escapeHtml(message.content)}</div></div>
    </div>`;
  }).join("");
  $("#chatMessages").scrollTop = $("#chatMessages").scrollHeight;
}

function renderChatHeader(detail) {
  const customer = customerOf(detail);
  const id = conversationId(detail);
  $("#chatHeader").hidden = false;
  const waiting = detail.status === "WAITING";
  const resolved = ["RESOLVED", "CLOSED"].includes(detail.status);
  const ownedByMe = detail.assignee === OPERATOR || detail.owner === OPERATOR;
  // 待领取、已解决或属于其他坐席的会话保持只读，最终权限仍由 Java Service 校验。
  $("#composer").hidden = waiting || resolved || !ownedByMe;
  $("#chatAvatar").textContent = initials(customer?.displayName || detail.customerName || detail.customerId);
  $("#chatCustomerName").textContent = customer?.displayName || detail.customerName || detail.customerId || "未知客户";
  $("#chatStatus").innerHTML = conversationStatusPill(detail.status);
  $("#chatPriority").innerHTML = detail.priority && detail.priority !== "NORMAL" ? priorityPill(detail.priority) : "";
  $("#chatMeta").textContent = `${channelLabel(detail.channel)} · ${id} · ${detail.queueName || detail.queue || "售后服务队列"}`;

  $("#claimConversation").hidden = !waiting;
  $("#waitConversation").hidden = waiting || resolved || !ownedByMe;
  $("#transferConversation").hidden = waiting || resolved || !ownedByMe;
  $("#resolveConversation").hidden = waiting || resolved || !ownedByMe;
}

function renderCustomerContext(detail) {
  const customer = customerOf(detail);
  const tags = detail.tags || customer?.tags || [];
  const tierLabel = ({ ENTERPRISE: "企业客户", VIP: "VIP 客户", NORMAL: "普通客户" })[customer?.tier] || customer?.tier || "普通客户";
  $("#contextContent").innerHTML = `
    <div class="customer-profile">
      <span class="customer-avatar ${customer?.tier === "ENTERPRISE" ? "enterprise" : customer?.tier === "VIP" ? "vip" : ""}">${escapeHtml(initials(customer?.displayName || detail.customerName))}</span>
      <h3>${escapeHtml(customer?.displayName || detail.customerName || detail.customerId)}</h3>
      <p>${escapeHtml(customer?.customerId || detail.customerId)} · ${escapeHtml(tierLabel)}</p>
      <div class="tag-row">${tags.length ? tags.map(tag => pill(tag, "outline")).join("") : pill(tierLabel, customer?.tier === "VIP" ? "amber" : "blue")}</div>
    </div>
    <section class="context-section">
      <div class="context-section-head"><h4>基本资料</h4><button type="button">查看完整档案</button></div>
      <div class="detail-list">
        <div class="detail-row"><span>联系电话</span><strong>${escapeHtml(customer?.phoneMasked || "--")}</strong></div>
        <div class="detail-row"><span>客户等级</span><strong>${escapeHtml(tierLabel)}</strong></div>
        <div class="detail-row"><span>服务归属</span><strong>${escapeHtml(detail.queueName || "华东售后中心")}</strong></div>
        <div class="detail-row"><span>历史会话</span><strong>${customer?.conversationCount ?? "--"} 次</strong></div>
      </div>
    </section>
    <section class="context-section">
      <div class="context-section-head"><h4>当前服务</h4></div>
      <div class="detail-list">
        <div class="detail-row"><span>识别意图</span><strong>${escapeHtml(detail.intent || "待识别")}</strong></div>
        <div class="detail-row"><span>情绪状态</span><strong>${escapeHtml(detail.sentiment || "平稳")}</strong></div>
        <div class="detail-row"><span>当前处理人</span><strong>${escapeHtml(detail.owner || detail.assignee || "未分配")}</strong></div>
        <div class="detail-row"><span>关联工单</span><strong>${escapeHtml(detail.ticketId || "暂无")}</strong></div>
      </div>
    </section>`;
}

function renderOrderContext(detail) {
  if (!inboxState.orders.length) {
    $("#contextContent").innerHTML = emptyState("没有关联订单", "客户当前没有可用订单记录");
    return;
  }
  $("#contextContent").innerHTML = `
    <section class="context-section" style="border-top:0">
      <div class="context-section-head"><h4>客户订单</h4><a href="./orders.html?customerId=${encodeURIComponent(customerOf(detail)?.customerId || detail.customerId || "")}">查看全部</a></div>
      <div style="display:grid;gap:8px">${inboxState.orders.map(order => `
        <a class="order-mini-card" href="./order-detail.html?orderNo=${encodeURIComponent(order.orderNo)}">
          <div class="order-mini-head"><strong>${escapeHtml(order.orderNo)}</strong>${orderStatusPill(order.status)}</div>
          <p>${escapeHtml(order.productSummary)}</p>
          <footer><span class="muted">${escapeHtml(order.paymentTime || formatDate(order.createdAt))}</span><strong>${formatMoney(order.amount)}</strong></footer>
        </a>`).join("")}</div>
    </section>`;
}

function renderAiContext(detail) {
  const decision = inboxState.aiDecision;
  if (!decision) {
    $("#contextContent").innerHTML = `
      <section class="context-section" style="border-top:0">
        <div class="ai-insight"><strong><span>✦</span> AI 坐席助手</strong><p>基于客户最新问题、知识库和订单工具生成回复建议。建议不会自动发送，高风险动作仍需人工确认。</p></div>
        <button class="button" id="contextGenerateAi" style="width:100%;margin-top:12px" type="button">生成本轮建议</button>
      </section>`;
    $("#contextGenerateAi")?.addEventListener("click", generateAiDraft);
    return;
  }
  const sources = decision.sources || [];
  const traces = decision.toolTraces || [];
  const guardrails = decision.guardrails || [];
  $("#contextContent").innerHTML = `
    <section class="context-section" style="border-top:0">
      <div class="ai-insight"><strong><span>✦</span> 决策摘要</strong><p>意图 ${escapeHtml(decision.intent)}，置信度 ${Math.round((decision.intentConfidence || 0) * 100)}%，${decision.handoffRequired ? "需要人工确认" : "可以由坐席确认后直接回复"}。</p></div>
    </section>
    <section class="context-section">
      <div class="context-section-head"><h4>知识依据</h4><span class="subtle">${sources.length} 条</span></div>
      <div class="detail-list">${sources.length ? sources.map(source => `<div class="order-mini-card"><div class="order-mini-head"><strong>${escapeHtml(source.title)}</strong><span class="mono subtle">${escapeHtml(source.version || "")}</span></div><p>${escapeHtml(source.content || source.snippet || "")}</p></div>`).join("") : '<p class="subtle">没有知识命中</p>'}</div>
    </section>
    <section class="context-section">
      <div class="context-section-head"><h4>工具调用</h4><span class="subtle">${traces.length} 次</span></div>
      <div class="detail-list">${traces.length ? traces.map(trace => `<div class="detail-row"><span>${escapeHtml(trace.toolName || trace.tool)}</span><strong>${escapeHtml(trace.result || trace.summary)}</strong></div>`).join("") : '<p class="subtle">本轮没有调用业务工具</p>'}</div>
    </section>
    <section class="context-section">
      <div class="context-section-head"><h4>风险护栏</h4><span class="subtle">${guardrails.length} 项</span></div>
      <div class="detail-list">${guardrails.length ? guardrails.map(item => `<div class="guardrail-card"><header><strong>${escapeHtml(item.problem || item.type)}</strong>${item.requiresHandoff ? pill("需人工", "red") : pill("已通过", "green")}</header><p>${escapeHtml(item.strategy || item.evidence || "")}</p></div>`).join("") : '<p class="subtle">未触发风险护栏</p>'}</div>
    </section>`;
}

function renderContext() {
  if (!inboxState.detail) return;
  if (inboxState.context === "order") renderOrderContext(inboxState.detail);
  else if (inboxState.context === "ai") renderAiContext(inboxState.detail);
  else renderCustomerContext(inboxState.detail);
}

function renderAiDraft(decision) {
  inboxState.aiDecision = decision;
  $("#aiDraftBody").textContent = decision.answer || decision.answerDraft || "";
  $("#aiConfidence").textContent = `${decision.intent || "UNKNOWN"} · ${Math.round((decision.intentConfidence || 0) * 100)}%`;
  $("#aiDraftCard").hidden = false;
  renderContext();
}

async function loadCustomerOrders(detail) {
  try {
    inboxState.orders = await request(`/api/support/orders?customerId=${encodeURIComponent(customerOf(detail)?.customerId || detail.customerId)}`);
  } catch {
    inboxState.orders = [];
  }
  if (inboxState.context === "order") renderOrderContext(detail);
}

async function selectConversation(id) {
  inboxState.selectedId = id;
  renderConversationList();
  $("#chatPane").classList.add("open");
  $("#chatMessages").innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
  try {
    const detail = await request(`/api/support/conversations/${encodeURIComponent(id)}`);
    inboxState.detail = detail;
    inboxState.aiDecision = null;
    renderChatHeader(detail);
    renderMessages(detail);
    renderContext();
    loadCustomerOrders(detail);
    history.replaceState(null, "", `./reception.html?conversationId=${encodeURIComponent(id)}`);
    const seedDraft = detail.aiDraft || detail.suggestedReply;
    if (seedDraft) renderAiDraft({ answer: seedDraft, intent: detail.intent, intentConfidence: detail.intentConfidence || .8, sources: detail.sources || [] });
  } catch (error) {
    renderError($("#chatMessages"), error, () => selectConversation(id));
  }
}

async function loadConversations(keepSelection = true) {
  try {
    const payload = await request("/api/support/conversations");
    state.conversations = Array.isArray(payload) ? payload : payload?.items || payload?.content || [];
    renderConversationList();
    const requestedId = queryParam("conversationId");
    const visibleItems = filteredConversations();
    const nextId = keepSelection && inboxState.selectedId
      ? inboxState.selectedId
      : requestedId || conversationId(visibleItems.find(item => item.status === "WAITING") || visibleItems[0]);
    if (nextId) await selectConversation(nextId);
  } catch (error) {
    renderError($("#conversationList"), error, () => loadConversations(false));
  }
}

function latestCustomerQuestion() {
  const messages = normalizeMessages(inboxState.detail).filter(message => ["CUSTOMER", "USER"].includes(message.role));
  return messages.at(-1)?.content || inboxState.detail?.lastMessage || inboxState.detail?.lastMessagePreview || inboxState.detail?.subject;
}

async function generateAiDraft() {
  if (!inboxState.detail) return;
  const button = $("#generateAiDraft");
  const typingIndicator = $("#typingIndicator");
  setBusy(button, true, "AI 分析中");
  typingIndicator.hidden = false;
  try {
    const detail = inboxState.detail;
    const decision = await request("/api/support/chat", {
      method: "POST",
      timeout: 45000,
      body: {
        sessionId: conversationId(detail),
        customerId: customerOf(detail)?.customerId || detail.customerId,
        orderNo: detail.orderNo || inboxState.orders[0]?.orderNo || "",
        question: latestCustomerQuestion()
      }
    });
    renderAiDraft(decision);
    saveLastDecision(decision);
    showToast("AI 建议已生成，请确认事实与风险后再发送");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
    typingIndicator.hidden = true;
  }
}

async function sendComposerContent() {
  const content = $("#replyContent").value.trim();
  if (!content || !inboxState.detail) {
    showToast("请输入要发送的内容", "error");
    return;
  }
  const button = $("#sendReply");
  setBusy(button, true, inboxState.composerMode === "note" ? "记录中" : "发送中");
  try {
    const path = inboxState.composerMode === "note" ? "notes" : "reply";
    await request(`/api/support/conversations/${encodeURIComponent(inboxState.selectedId)}/${path}`, {
      method: "POST",
      body: { operator: OPERATOR, content }
    });
    $("#replyContent").value = "";
    $("#aiDraftCard").hidden = true;
    await selectConversation(inboxState.selectedId);
    await loadConversations(true);
    showToast(inboxState.composerMode === "note" ? "内部备注已记录" : "回复已发送");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

async function claimConversation() {
  const button = $("#claimConversation");
  setBusy(button, true, "领取中");
  try {
    await request(`/api/support/conversations/${encodeURIComponent(inboxState.selectedId)}/claim`, { method: "POST", body: { operator: OPERATOR } });
    await loadConversations(true);
    showToast("会话已领取到我的工作台");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

function openActionModal(action) {
  inboxState.action = action;
  const config = {
    wait: { title: "等待客户回复", label: "等待原因", placeholder: "例如：已请客户补充订单凭证", confirm: "确认等待" },
    transfer: { title: "转接会话", label: "目标处理人", placeholder: "例如：support-zhangsan", confirm: "确认转接" },
    resolve: { title: "解决会话", label: "解决结论", placeholder: "记录本次问题如何解决，便于质检与复盘", confirm: "标记已解决" }
  }[action];
  $("#actionModalTitle").textContent = config.title;
  $("#actionModalLabel").textContent = config.label;
  $("#actionModalInput").placeholder = config.placeholder;
  $("#actionModalInput").value = "";
  $("#confirmActionModal").textContent = config.confirm;
  $("#actionModal").classList.add("open");
  setTimeout(() => $("#actionModalInput").focus(), 60);
}

function closeActionModal() {
  $("#actionModal").classList.remove("open");
  inboxState.action = null;
}

async function confirmAction() {
  const value = $("#actionModalInput").value.trim();
  if (!value) {
    showToast("请填写完整信息", "error");
    return;
  }
  const action = inboxState.action;
  const config = {
    wait: { path: "wait-customer", body: { operator: OPERATOR, reason: value } },
    transfer: { path: "assign", body: { owner: value, operator: OPERATOR } },
    resolve: { path: "resolve", body: { operator: OPERATOR, resolution: value } }
  }[action];
  const button = $("#confirmActionModal");
  setBusy(button, true, "提交中");
  try {
    await request(`/api/support/conversations/${encodeURIComponent(inboxState.selectedId)}/${config.path}`, { method: "POST", body: config.body });
    closeActionModal();
    await loadConversations(true);
    showToast(action === "resolve" ? "会话已解决" : action === "transfer" ? "会话已转接" : "会话已进入等待客户状态");
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

initPage("reception");
$("#conversationSearchIcon").innerHTML = icon("search", 15);
$("#emptyChatIcon").innerHTML = icon("inbox", 24);
$("#toggleContext").innerHTML = icon("user", 17);
$("#mobileBack").innerHTML = icon("chevron", 17);
$("#quickReply").innerHTML = icon("book", 16);
$("#attachFile").innerHTML = icon("plus", 16);
$("#closeActionModal").innerHTML = icon("close", 17);
loadConversations(false);

$("#conversationSearch")?.addEventListener("input", event => { inboxState.keyword = event.target.value; renderConversationList(); });
$$('.inbox-view').forEach(button => button.addEventListener("click", () => {
  $$('.inbox-view').forEach(item => item.classList.remove("active"));
  button.classList.add("active");
  inboxState.view = button.dataset.view;
  renderConversationList();
}));
$$('.composer-tab').forEach(button => button.addEventListener("click", () => {
  $$('.composer-tab').forEach(item => item.classList.remove("active"));
  button.classList.add("active");
  inboxState.composerMode = button.dataset.mode;
  $("#replyContent").placeholder = inboxState.composerMode === "note" ? "输入仅内部可见的处理备注" : "输入回复内容；AI 建议需要人工确认后才会发送";
  $("#sendReply").textContent = inboxState.composerMode === "note" ? "记录备注" : "发送回复";
}));
$$('.context-tab').forEach(button => button.addEventListener("click", () => {
  $$('.context-tab').forEach(item => item.classList.remove("active"));
  button.classList.add("active");
  inboxState.context = button.dataset.context;
  renderContext();
}));

$("#generateAiDraft")?.addEventListener("click", generateAiDraft);
$("#regenerateAiDraft")?.addEventListener("click", generateAiDraft);
$("#applyAiDraft")?.addEventListener("click", () => {
  $("#replyContent").value = inboxState.aiDecision?.answer || "";
  $("#replyContent").focus();
  $("#aiDraftCard").hidden = true;
});
$("#dismissAiDraft")?.addEventListener("click", () => { $("#aiDraftCard").hidden = true; });
$("#sendReply")?.addEventListener("click", sendComposerContent);
$("#replyContent")?.addEventListener("keydown", event => {
  if ((event.metaKey || event.ctrlKey) && event.key === "Enter") sendComposerContent();
});
$("#claimConversation")?.addEventListener("click", claimConversation);
$("#waitConversation")?.addEventListener("click", () => openActionModal("wait"));
$("#transferConversation")?.addEventListener("click", () => openActionModal("transfer"));
$("#resolveConversation")?.addEventListener("click", () => openActionModal("resolve"));
$("#closeActionModal")?.addEventListener("click", closeActionModal);
$("#cancelActionModal")?.addEventListener("click", closeActionModal);
$("#confirmActionModal")?.addEventListener("click", confirmAction);
$("#actionModal")?.addEventListener("click", event => { if (event.target === event.currentTarget) closeActionModal(); });
$("#toggleContext")?.addEventListener("click", () => $("#contextPane").classList.toggle("open"));
$("#mobileBack")?.addEventListener("click", () => $("#chatPane").classList.remove("open"));
$("#quickReply")?.addEventListener("click", () => { $("#replyContent").value = "您好，我已经看到您的问题，正在为您核对订单和相关规则，请稍等。"; $("#replyContent").focus(); });
$("#attachFile")?.addEventListener("click", () => showToast("附件上传需要接入对象存储，当前课程版仅保留受控入口", "info"));
