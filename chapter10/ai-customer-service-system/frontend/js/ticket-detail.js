/* 工单详情：把主数据、协作动作和审计时间线放在独立路由中。 */

const TICKET_OPERATOR = "support-linxiao";

function timelineTypeLabel(type) {
  return ({ CREATED: "工单创建", ASSIGNED: "负责人变更", NOTE_ADDED: "添加备注", RESOLVED: "工单解决", DUPLICATE_HIT: "重复问题命中" })[type] || type;
}

function renderTicketDetail(ticket) {
  state.currentTicket = ticket;
  $("#ticketPageTitle").textContent = ticket.ticketId;
  $("#ticketPageSubtitle").textContent = `${ticket.customerId} · ${ticket.intent} · 创建于 ${formatDate(ticket.createdAt)}`;
  if (ticket.sessionId) {
    $("#linkedConversation").hidden = false;
    $("#linkedConversation").href = `./reception.html?conversationId=${encodeURIComponent(ticket.sessionId)}`;
  }
  $("#ticketDetailRoot").innerHTML = `
    <div class="detail-page-grid">
      <div class="detail-column">
        <section class="surface detail-hero">
          <span class="detail-hero-icon">${icon("ticket", 21)}</span>
          <div class="detail-hero-copy"><h2>${escapeHtml(ticket.question)}</h2><p>客户 ${escapeHtml(ticket.customerId)} · 会话 ${escapeHtml(ticket.sessionId)}</p><div class="meta-row">${statusPill(ticket.status)}${priorityPill(ticket.priority)}${slaPill(ticket)}${ticket.duplicateCount ? pill(`重复命中 ${ticket.duplicateCount} 次`, "amber") : ""}</div></div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>工单信息</h2><p>业务归属、服务承诺和当前处理状态。</p></div></div>
          <div class="surface-body definition-grid">
            <div class="definition-item"><span>客户编号</span><strong>${escapeHtml(ticket.customerId)}</strong></div>
            <div class="definition-item"><span>识别意图</span><strong>${escapeHtml(ticket.intent)}</strong></div>
            <div class="definition-item"><span>负责人</span><strong>${escapeHtml(ticket.owner || "未分配")}</strong></div>
            <div class="definition-item"><span>SLA 截止</span><strong>${escapeHtml(formatDate(ticket.slaDueAt))}</strong></div>
            <div class="definition-item"><span>创建时间</span><strong>${escapeHtml(formatDate(ticket.createdAt))}</strong></div>
            <div class="definition-item"><span>更新时间</span><strong>${escapeHtml(formatDate(ticket.updatedAt))}</strong></div>
          </div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>AI 处理草稿</h2><p>创建工单时保留的建议，仅供人工处理参考。</p></div></div>
          <div class="surface-body"><div class="ai-insight"><strong><span>✦</span> 建议回复</strong><p>${escapeHtml(ticket.answerDraft || "没有生成 AI 草稿")}</p></div></div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>协作时间线</h2><p>每次分配、备注和关闭都保留操作人及时间。</p></div></div>
          <div class="surface-body"><div class="timeline">${(ticket.timeline || []).map(event => `
            <div class="timeline-event"><span class="timeline-node">${icon(event.type === "RESOLVED" ? "check" : "pulse", 10)}</span><div class="timeline-copy"><strong>${escapeHtml(timelineTypeLabel(event.type))} · ${escapeHtml(event.operator)}</strong><p>${escapeHtml(event.message)}</p><time>${escapeHtml(formatDate(event.occurredAt, true))}</time></div></div>`).join("") || '<p class="subtle">暂无时间线事件</p>'}</div></div>
        </section>
      </div>
      <aside class="detail-column">
        <section class="surface">
          <div class="surface-head"><div><h2>处理负责人</h2><p>分配给具体协作人员。</p></div></div>
          <div class="surface-body"><div class="field"><label for="ticketOwner">处理人账号</label><input id="ticketOwner" value="${escapeHtml(ticket.owner || TICKET_OPERATOR)}"></div><button class="button" id="assignTicket" style="width:100%;margin-top:10px" ${ticket.actionable ? "" : "disabled"}>更新负责人</button></div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>内部备注</h2><p>记录只对内部协作人员可见。</p></div></div>
          <div class="surface-body"><div class="field"><label for="ticketNote">处理进展</label><textarea id="ticketNote" placeholder="例如：已联系仓库，等待拦截结果"></textarea></div><button class="button secondary" id="addTicketNote" style="width:100%;margin-top:10px">添加备注</button></div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>解决工单</h2><p>填写可复盘的解决结论后关闭。</p></div></div>
          <div class="surface-body"><div class="field"><label for="ticketResolution">解决结论</label><textarea id="ticketResolution" placeholder="描述最终处理方案和客户确认结果"></textarea></div><button class="button success" id="resolveTicket" style="width:100%;margin-top:10px" ${ticket.actionable ? "" : "disabled"}>标记为已解决</button></div>
        </section>
      </aside>
    </div>`;
  $("#assignTicket")?.addEventListener("click", assignTicket);
  $("#addTicketNote")?.addEventListener("click", addTicketNote);
  $("#resolveTicket")?.addEventListener("click", resolveTicket);
}

async function loadTicketDetail() {
  const ticketId = queryParam("ticketId");
  if (!ticketId) {
    renderError($("#ticketDetailRoot"), new Error("缺少 ticketId 参数"));
    return;
  }
  try {
    renderTicketDetail(await request(`/api/support/tickets/${encodeURIComponent(ticketId)}`));
  } catch (error) {
    renderError($("#ticketDetailRoot"), error, loadTicketDetail);
  }
}

async function updateTicket(path, body, button, successMessage) {
  setBusy(button, true, "提交中");
  try {
    const updated = await request(`/api/support/tickets/${encodeURIComponent(state.currentTicket.ticketId)}/${path}`, { method: "POST", body });
    renderTicketDetail(updated);
    showToast(successMessage);
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

function assignTicket() {
  const owner = $("#ticketOwner").value.trim();
  if (!owner) return showToast("处理人不能为空", "error");
  updateTicket("assign", { owner }, $("#assignTicket"), "负责人已更新");
}

function addTicketNote() {
  const message = $("#ticketNote").value.trim();
  if (!message) return showToast("请输入处理进展", "error");
  updateTicket("notes", { operator: TICKET_OPERATOR, message }, $("#addTicketNote"), "内部备注已添加");
}

function resolveTicket() {
  const summary = $("#ticketResolution").value.trim();
  if (!summary) return showToast("请输入解决结论", "error");
  updateTicket("close", { operator: TICKET_OPERATOR, summary }, $("#resolveTicket"), "工单已解决");
}

initPage("ticketDetail");
loadTicketDetail();
