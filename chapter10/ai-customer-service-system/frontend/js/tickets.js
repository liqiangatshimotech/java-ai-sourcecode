/* 工单列表只负责筛选和导航，处理动作放在独立详情页。 */

const ticketPageState = { keyword: "", status: "", priority: "" };

function filteredTickets() {
  const keyword = ticketPageState.keyword.trim().toLowerCase();
  return state.tickets.filter(ticket => {
    const searchable = [ticket.ticketId, ticket.customerId, ticket.question, ticket.intent, ticket.owner].filter(Boolean).join(" ").toLowerCase();
    return (!keyword || searchable.includes(keyword))
      && (!ticketPageState.status || ticket.status === ticketPageState.status)
      && (!ticketPageState.priority || ticket.priority === ticketPageState.priority);
  });
}

function renderTicketMetrics(dashboard = {}) {
  const open = dashboard.openCount ?? state.tickets.filter(ticket => ticket.status !== "RESOLVED").length;
  const assigned = dashboard.assignedCount ?? state.tickets.filter(ticket => ticket.status === "ASSIGNED").length;
  const risk = (dashboard.breachedCount ?? state.tickets.filter(ticket => ticket.slaBreached).length)
    + (dashboard.dueSoonCount ?? state.tickets.filter(ticket => ticket.slaStatus === "DUE_SOON").length);
  const resolved = dashboard.resolvedCount ?? state.tickets.filter(ticket => ticket.status === "RESOLVED").length;
  $("#ticketStatOpen").textContent = String(open);
  $("#ticketStatAssigned").textContent = String(assigned);
  $("#ticketStatRisk").textContent = String(risk);
  $("#ticketStatResolved").textContent = String(resolved);
}

function renderTickets() {
  const tickets = filteredTickets();
  $("#ticketResultCount").textContent = `共 ${tickets.length} 条`;
  $("#ticketFooterText").textContent = `显示 ${tickets.length} 条工单`;
  if (!tickets.length) {
    $("#ticketsResult").innerHTML = emptyState("没有符合条件的工单", "调整状态、优先级或搜索条件后重试");
    return;
  }
  $("#ticketsResult").innerHTML = `
    <table class="data-table">
      <thead><tr><th>工单</th><th>客户与问题</th><th>意图</th><th>优先级</th><th>SLA</th><th>负责人</th><th>更新时间</th><th></th></tr></thead>
      <tbody>${tickets.map(ticket => `
        <tr>
          <td><div class="cell-main"><a class="table-action mono" href="./ticket-detail.html?ticketId=${encodeURIComponent(ticket.ticketId)}">${escapeHtml(ticket.ticketId)}</a><small>${statusPill(ticket.status)}</small></div></td>
          <td><div class="cell-main" style="max-width:330px"><strong>${escapeHtml(ticket.customerId)}</strong><small style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${escapeHtml(ticket.question)}</small></div></td>
          <td>${pill(ticket.intent || "UNKNOWN", "outline")}</td>
          <td>${priorityPill(ticket.priority)}</td>
          <td><div class="cell-main">${slaPill(ticket)}<small>${ticket.slaBreached ? `超时 ${formatDuration(Math.abs(ticket.remainingMinutes || 0) * 60)}` : `剩余 ${formatDuration(Math.max(0, ticket.remainingMinutes || 0) * 60)}`}</small></div></td>
          <td><div class="customer-cell"><span class="customer-avatar">${escapeHtml(initials(ticket.owner || "未分配"))}</span><span>${escapeHtml(ticket.owner || "未分配")}</span></div></td>
          <td class="nowrap">${escapeHtml(formatDate(ticket.updatedAt))}</td>
          <td>
            <div class="row-actions">
              <a class="icon-button" href="./ticket-detail.html?ticketId=${encodeURIComponent(ticket.ticketId)}" aria-label="查看工单">${icon("chevron", 15)}</a>
            </div>
          </td>
        </tr>`).join("")}</tbody>
    </table>`;
}

async function loadTickets() {
  const button = $("#refreshTickets");
  setBusy(button, true, "同步中");
  $("#ticketsResult").innerHTML = skeletonRows(5);
  try {
    const [tickets, dashboard] = await Promise.all([
      request("/api/support/tickets"),
      request("/api/support/tickets/dashboard").catch(() => ({}))
    ]);
    state.tickets = tickets;
    renderTicketMetrics(dashboard);
    renderTickets();
  } catch (error) {
    renderError($("#ticketsResult"), error, loadTickets);
  } finally {
    setBusy(button, false);
  }
}

initPage("tickets");
$("#refreshTickets").innerHTML = `${icon("refresh", 15)}同步工单`;
$("#ticketSearchIcon").innerHTML = icon("search", 15);
$("#ticketStatIconOpen").innerHTML = icon("ticket", 17);
$("#ticketStatIconAssigned").innerHTML = icon("user", 17);
$("#ticketStatIconRisk").innerHTML = icon("clock", 17);
$("#ticketStatIconResolved").innerHTML = icon("check", 17);
loadTickets();

$("#ticketKeyword")?.addEventListener("input", event => { ticketPageState.keyword = event.target.value; renderTickets(); });
$("#ticketStatusFilter")?.addEventListener("change", event => { ticketPageState.status = event.target.value; renderTickets(); });
$("#ticketPriorityFilter")?.addEventListener("change", event => { ticketPageState.priority = event.target.value; renderTickets(); });
$("#refreshTickets")?.addEventListener("click", loadTickets);
$("#clearTicketFilters")?.addEventListener("click", () => {
  ticketPageState.keyword = ticketPageState.status = ticketPageState.priority = "";
  $("#ticketKeyword").value = "";
  $("#ticketStatusFilter").value = "";
  $("#ticketPriorityFilter").value = "";
  renderTickets();
});
