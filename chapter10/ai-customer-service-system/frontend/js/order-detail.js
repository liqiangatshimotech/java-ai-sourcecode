/* 订单详情：展示业务事实，并只允许界面声明的合法状态迁移。 */

const allowedOrderTransitions = {
  PAID_PICKING: ["SHIPPED", "REFUNDING", "CLOSED"],
  SHIPPED: ["DELIVERED", "REFUNDING"],
  DELIVERED: ["REFUNDING", "CLOSED"],
  REFUNDING: ["CLOSED"],
  CLOSED: []
};

function renderOrderDetail(order) {
  state.currentOrder = order;
  $("#orderPageTitle").textContent = order.orderNo;
  $("#orderPageSubtitle").textContent = `${order.customerName} · ${order.productSummary}`;
  $("#openCustomerConversation").href = `./reception.html?customerId=${encodeURIComponent(order.customerId)}`;
  const transitions = allowedOrderTransitions[order.status] || [];
  $("#orderDetailRoot").innerHTML = `
    <div class="detail-page-grid">
      <div class="detail-column">
        <section class="surface detail-hero">
          <span class="detail-hero-icon">${icon("order", 21)}</span>
          <div class="detail-hero-copy"><h2>${escapeHtml(order.productSummary)}</h2><p>${escapeHtml(order.orderNo)} · 支付于 ${escapeHtml(order.paymentTime)}</p><div class="meta-row">${orderStatusPill(order.status)}${pill(channelLabel(order.channel), "outline")}${pill(riskLabel(order.riskLevel), ["HIGH_VALUE", "FINANCE_REVIEW"].includes(order.riskLevel) ? "orange" : "neutral")}</div></div>
          <strong style="font-size:20px">${formatMoney(order.amount)}</strong>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>订单信息</h2><p>客服回复和售后动作使用的订单主数据。</p></div></div>
          <div class="surface-body definition-grid">
            <div class="definition-item"><span>订单号</span><strong class="mono">${escapeHtml(order.orderNo)}</strong></div>
            <div class="definition-item"><span>实付金额</span><strong>${formatMoney(order.amount)}</strong></div>
            <div class="definition-item"><span>下单渠道</span><strong>${escapeHtml(channelLabel(order.channel))}</strong></div>
            <div class="definition-item"><span>支付时间</span><strong>${escapeHtml(order.paymentTime)}</strong></div>
            <div class="definition-item"><span>创建时间</span><strong>${escapeHtml(formatDate(order.createdAt))}</strong></div>
            <div class="definition-item"><span>最近更新</span><strong>${escapeHtml(formatDate(order.updatedAt))}</strong></div>
          </div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>履约轨迹</h2><p>课程版用订单状态生成履约节点，生产项目应接入真实履约事件。</p></div></div>
          <div class="surface-body"><div class="timeline">
            <div class="timeline-event"><span class="timeline-node">${icon("check", 10)}</span><div class="timeline-copy"><strong>订单支付成功</strong><p>${escapeHtml(order.productSummary)}</p><time>${escapeHtml(order.paymentTime)}</time></div></div>
            <div class="timeline-event"><span class="timeline-node">${icon(order.status === "PAID_PICKING" ? "clock" : "check", 10)}</span><div class="timeline-copy"><strong>仓库履约</strong><p>${order.status === "PAID_PICKING" ? "等待仓库拣货" : "订单已完成拣货并进入后续流程"}</p><time>${escapeHtml(formatDate(order.updatedAt))}</time></div></div>
            ${["SHIPPED", "DELIVERED"].includes(order.status) ? `<div class="timeline-event"><span class="timeline-node">${icon("pulse", 10)}</span><div class="timeline-copy"><strong>${order.status === "DELIVERED" ? "客户已签收" : "运输中"}</strong><p>物流状态由业务系统同步</p><time>${escapeHtml(formatDate(order.updatedAt))}</time></div></div>` : ""}
          </div></div>
        </section>
      </div>
      <aside class="detail-column">
        <section class="surface">
          <div class="surface-head"><div><h2>客户与收货信息</h2><p>敏感字段在后端完成脱敏。</p></div></div>
          <div class="surface-body"><div class="detail-list"><div class="detail-row"><span>客户名称</span><strong>${escapeHtml(order.customerName)}</strong></div><div class="detail-row"><span>客户编号</span><strong>${escapeHtml(order.customerId)}</strong></div><div class="detail-row"><span>收货信息</span><strong>${escapeHtml(order.receiverMasked)}</strong></div><div class="detail-row"><span>风险标签</span><strong>${escapeHtml(riskLabel(order.riskLevel))}</strong></div></div></div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>受控状态动作</h2><p>界面只暴露当前状态允许的下一步。</p></div></div>
          <div class="surface-body">${transitions.length ? `<div class="field"><label for="nextOrderStatus">下一状态</label><select id="nextOrderStatus">${transitions.map(status => `<option value="${status}">${escapeHtml(orderStatusLabel(status))}</option>`).join("")}</select></div><button class="button" id="updateOrderStatus" style="width:100%;margin-top:10px">提交状态变更</button>` : '<div class="empty-state compact"><strong>订单流程已结束</strong><p>当前状态没有允许的后续动作</p></div>'}</div>
        </section>
        <section class="surface">
          <div class="surface-head"><div><h2>客服建议</h2><p>根据订单状态给出人工判断提示。</p></div></div>
          <div class="surface-body"><div class="ai-insight"><strong><span>✦</span> 业务提醒</strong><p>${order.riskLevel === "HIGH_VALUE" || order.riskLevel === "FINANCE_REVIEW" ? "这是高价值或财务复核订单，退款承诺必须经过人工审批，并保留工单记录。" : "回复客户前应再次核对订单归属和最新履约状态。"}</p></div></div>
        </section>
      </aside>
    </div>`;
  $("#updateOrderStatus")?.addEventListener("click", updateOrderStatus);
}

async function loadOrderDetail() {
  const orderNo = queryParam("orderNo");
  if (!orderNo) return renderError($("#orderDetailRoot"), new Error("缺少 orderNo 参数"));
  try {
    renderOrderDetail(await request(`/api/support/orders/${encodeURIComponent(orderNo)}`));
  } catch (error) {
    renderError($("#orderDetailRoot"), error, loadOrderDetail);
  }
}

async function updateOrderStatus() {
  const status = $("#nextOrderStatus").value;
  const button = $("#updateOrderStatus");
  setBusy(button, true, "更新中");
  try {
    const updated = await request(`/api/support/orders/${encodeURIComponent(state.currentOrder.orderNo)}/status`, { method: "POST", body: { status, operator: "support-linxiao" } });
    renderOrderDetail(updated);
    showToast(`订单状态已更新为${orderStatusLabel(status)}`);
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

initPage("orderDetail");
loadOrderDetail();
