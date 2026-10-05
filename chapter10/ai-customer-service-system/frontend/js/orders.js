/* 订单列表：客服先查明业务事实，再进入订单详情执行受控动作。 */

const orderPageState = { keyword: "", status: "", risk: "" };

function filteredOrders() {
  const keyword = orderPageState.keyword.trim().toLowerCase();
  return state.orders.filter(order => {
    const searchable = [order.orderNo, order.customerId, order.customerName, order.productSummary].filter(Boolean).join(" ").toLowerCase();
    return (!keyword || searchable.includes(keyword))
      && (!orderPageState.status || order.status === orderPageState.status)
      && (!orderPageState.risk || order.riskLevel === orderPageState.risk);
  });
}

function renderOrderMetrics() {
  $("#orderStatAll").textContent = String(state.orders.length);
  $("#orderStatShipping").textContent = String(state.orders.filter(order => ["PAID_PICKING", "SHIPPED"].includes(order.status)).length);
  $("#orderStatRefund").textContent = String(state.orders.filter(order => order.status === "REFUNDING").length);
  $("#orderStatRisk").textContent = String(state.orders.filter(order => ["HIGH_VALUE", "FINANCE_REVIEW"].includes(order.riskLevel)).length);
}

function renderOrders() {
  const orders = filteredOrders();
  $("#orderResultCount").textContent = `共 ${orders.length} 条`;
  $("#orderFooterText").textContent = `显示 ${orders.length} 条订单`;
  if (!orders.length) {
    $("#ordersResult").innerHTML = emptyState("没有符合条件的订单", "调整状态、风险或搜索条件后重试");
    return;
  }
  $("#ordersResult").innerHTML = `
    <table class="data-table">
      <thead><tr><th>订单号</th><th>客户</th><th>商品</th><th>金额</th><th>履约状态</th><th>渠道</th><th>风险</th><th>更新时间</th><th></th></tr></thead>
      <tbody>${orders.map(order => `
        <tr>
          <td><div class="cell-main"><a class="table-action mono" href="./order-detail.html?orderNo=${encodeURIComponent(order.orderNo)}">${escapeHtml(order.orderNo)}</a><small>${escapeHtml(order.paymentTime || "--")}</small></div></td>
          <td><div class="customer-cell"><span class="customer-avatar ${order.riskLevel === "HIGH_VALUE" ? "enterprise" : ""}">${escapeHtml(initials(order.customerName))}</span><div class="cell-main"><strong>${escapeHtml(order.customerName)}</strong><small>${escapeHtml(order.customerId)}</small></div></div></td>
          <td><div class="cell-main" style="max-width:270px"><strong style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${escapeHtml(order.productSummary)}</strong><small>${escapeHtml(order.receiverMasked)}</small></div></td>
          <td class="nowrap"><strong>${formatMoney(order.amount)}</strong></td>
          <td>${orderStatusPill(order.status)}</td>
          <td>${pill(channelLabel(order.channel), "outline")}</td>
          <td>${pill(riskLabel(order.riskLevel), ["HIGH_VALUE", "FINANCE_REVIEW"].includes(order.riskLevel) ? "orange" : "neutral")}</td>
          <td class="nowrap">${escapeHtml(formatDate(order.updatedAt))}</td>
          <td>
            <div class="row-actions">
              <a class="icon-button" href="./order-detail.html?orderNo=${encodeURIComponent(order.orderNo)}" aria-label="查看订单">${icon("chevron", 15)}</a>
            </div>
          </td>
        </tr>`).join("")}</tbody>
    </table>`;
}

async function loadOrders() {
  const button = $("#refreshOrders");
  setBusy(button, true, "同步中");
  $("#ordersResult").innerHTML = skeletonRows(5);
  try {
    state.orders = await request("/api/support/orders");
    renderOrderMetrics();
    renderOrders();
  } catch (error) {
    renderError($("#ordersResult"), error, loadOrders);
  } finally {
    setBusy(button, false);
  }
}

initPage("orders");
$("#refreshOrders").innerHTML = `${icon("refresh", 15)}同步订单`;
$("#orderSearchIcon").innerHTML = icon("search", 15);
$("#orderStatIconAll").innerHTML = icon("order", 17);
$("#orderStatIconShipping").innerHTML = icon("pulse", 17);
$("#orderStatIconRefund").innerHTML = icon("refresh", 17);
$("#orderStatIconRisk").innerHTML = icon("clock", 17);
loadOrders();

$("#orderKeyword")?.addEventListener("input", event => { orderPageState.keyword = event.target.value; renderOrders(); });
$("#orderStatusFilter")?.addEventListener("change", event => { orderPageState.status = event.target.value; renderOrders(); });
$("#orderRiskFilter")?.addEventListener("change", event => { orderPageState.risk = event.target.value; renderOrders(); });
$("#refreshOrders")?.addEventListener("click", loadOrders);
$("#clearOrderFilters")?.addEventListener("click", () => {
  orderPageState.keyword = orderPageState.status = orderPageState.risk = "";
  $("#orderKeyword").value = ""; $("#orderStatusFilter").value = ""; $("#orderRiskFilter").value = "";
  renderOrders();
});
