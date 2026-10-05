/*
 * 星桥客服运营平台 - 前端公共能力
 *
 * 项目刻意保持零前端依赖，方便只熟悉 Java 的读者直接理解浏览器如何调用
 * Spring Boot API。这里集中处理导航、请求、状态标签和通用交互，业务页面只需要
 * 关注自己的数据与动作。
 */

const state = {
  runtime: null,
  dashboard: null,
  conversations: [],
  orders: [],
  tickets: [],
  knowledgeDocuments: [],
  productionIssues: [],
  currentConversation: null,
  currentOrder: null,
  currentTicket: null
};

const pageMeta = {
  overview: { section: "工作台", title: "运营驾驶舱", href: "./index.html" },
  reception: { section: "工作台", title: "会话收件箱", href: "./reception.html" },
  tickets: { section: "工作台", title: "工单协作", href: "./tickets.html" },
  ticketDetail: { section: "工单协作", title: "工单详情", href: "./ticket-detail.html" },
  orders: { section: "客户业务", title: "订单与售后", href: "./orders.html" },
  orderDetail: { section: "订单与售后", title: "订单详情", href: "./order-detail.html" },
  knowledge: { section: "知识与 AI", title: "知识运营", href: "./knowledge.html" },
  recall: { section: "知识与 AI", title: "RAG 评测实验室", href: "./recall.html" },
  governance: { section: "知识与 AI", title: "AI 运行审计", href: "./governance.html" },
  settings: { section: "管理", title: "平台设置", href: "./settings.html" }
};

const navGroups = [
  {
    title: "工作台",
    items: [
      { key: "overview", label: "运营驾驶舱", href: "./index.html", icon: "dashboard" },
      { key: "reception", label: "会话收件箱", href: "./reception.html", icon: "inbox", badge: "queue" },
      { key: "tickets", label: "工单协作", href: "./tickets.html", icon: "ticket" }
    ]
  },
  {
    title: "客户业务",
    items: [
      { key: "orders", label: "订单与售后", href: "./orders.html", icon: "order" }
    ]
  },
  {
    title: "知识与 AI",
    items: [
      { key: "knowledge", label: "知识运营", href: "./knowledge.html", icon: "book" },
      { key: "recall", label: "RAG 评测实验室", href: "./recall.html", icon: "flask" },
      { key: "governance", label: "AI 运行审计", href: "./governance.html", icon: "pulse" }
    ]
  },
  {
    title: "管理",
    items: [
      { key: "settings", label: "平台设置", href: "./settings.html", icon: "settings" }
    ]
  }
];

const iconPaths = {
  dashboard: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="4" rx="2"/><rect x="14" y="11" width="7" height="10" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/>',
  inbox: '<path d="M4 5.5h16v12.8a1.7 1.7 0 0 1-1.7 1.7H5.7A1.7 1.7 0 0 1 4 18.3V5.5Z"/><path d="M4 14h4l1.5 2h5l1.5-2h4"/><path d="M7 9h10"/>',
  ticket: '<path d="M5 4h14v5a3 3 0 0 0 0 6v5H5v-5a3 3 0 0 0 0-6V4Z"/><path d="M12 7v2M12 12v1M12 16v1"/>',
  order: '<path d="M4 7 12 3l8 4v10l-8 4-8-4V7Z"/><path d="m4 7 8 4 8-4M12 11v10M8 5l8 4"/>',
  book: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v17H6.5A2.5 2.5 0 0 0 4 22V5.5ZM20 5.5A2.5 2.5 0 0 0 17.5 3H13v17h4.5A2.5 2.5 0 0 1 20 22V5.5Z"/>',
  flask: '<path d="M9 3h6M10 3v6l-5 9a2 2 0 0 0 1.8 3h10.4A2 2 0 0 0 19 18l-5-9V3"/><path d="M7.5 15h9"/>',
  pulse: '<path d="M3 12h4l2-6 4 12 2-6h6"/><path d="M5 4.5A9 9 0 1 1 4 18"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1 1.55V21h-4v-.08a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.55-1H3v-4h.08a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.55V3h4v.08a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9a1.7 1.7 0 0 0 1.55 1H21v4h-.08a1.7 1.7 0 0 0-1.52 1Z"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  chevron: '<path d="m9 18 6-6-6-6"/>',
  refresh: '<path d="M20 6v5h-5"/><path d="M4 18v-5h5"/><path d="M18.5 9A7 7 0 0 0 6 6.5L4 9M5.5 15A7 7 0 0 0 18 17.5l2-2.5"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  arrowUp: '<path d="m6 15 6-6 6 6"/>',
  arrowDown: '<path d="m6 9 6 6 6-6"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  external: '<path d="M14 4h6v6M20 4l-9 9"/><path d="M18 13v6H5V6h6"/>',
  more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>'
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

function icon(name, size = 18) {
  return `<svg class="icon" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${iconPaths[name] || iconPaths.more}</svg>`;
}

/**
 * 前后端由同一个 Spring Boot JAR 提供时，API 使用当前域名；单独用静态服务器开发时，
 * 默认连接 18081。这个判断让下载代码后既能一条命令运行，也能前后端分开调试。
 */
function apiBase() {
  const pageInput = $("#apiBase");
  if (pageInput?.value.trim()) return pageInput.value.trim().replace(/\/$/, "");
  const stored = localStorage.getItem("supportApiBase");
  if (stored) return stored.replace(/\/$/, "");
  if (["18081", "8080"].includes(window.location.port)) return window.location.origin;
  return "http://localhost:18081";
}

function newRequestId() {
  return `WEB-${Date.now().toString(36).toUpperCase()}-${Math.random().toString(16).slice(2, 8).toUpperCase()}`;
}

/**
 * 统一 API 请求入口。后端异常、代理返回 HTML、204 无正文都在这里处理，业务页面不会
 * 因为 JSON.parse 再次报错而丢失真正的 HTTP 错误信息。
 */
async function request(path, options = {}) {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), options.timeout || 15000);
  const headers = {
    Accept: "application/json",
    "X-Request-Id": newRequestId(),
    ...(options.headers || {})
  };
  const accessToken = sessionStorage.getItem("supportAccessToken");
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const init = {
    method: options.method || "GET",
    headers,
    signal: controller.signal
  };
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    init.body = typeof options.body === "string" ? options.body : JSON.stringify(options.body);
  }

  try {
    const response = await fetch(`${apiBase()}${path}`, init);
    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = { message: text.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim() };
      }
    }
    if (!response.ok) {
      const details = Array.isArray(data?.details) && data.details.length
        ? `：${data.details.join("；")}`
        : "";
      const error = new Error(`${data?.message || `请求失败（HTTP ${response.status}）`}${details}`);
      error.status = response.status;
      error.code = data?.code;
      throw error;
    }
    return data;
  } catch (error) {
    if (error.name === "AbortError") throw new Error("请求超时，请检查后端或模型服务状态");
    throw error;
  } finally {
    window.clearTimeout(timeout);
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatDate(value, withSeconds = false) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: withSeconds ? "2-digit" : undefined,
    hour12: false
  });
}

function formatTime(value) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
}

function formatDuration(seconds) {
  const total = Math.max(0, Number(seconds || 0));
  if (total < 60) return `${Math.round(total)} 秒`;
  if (total < 3600) return `${Math.floor(total / 60)} 分 ${Math.round(total % 60)} 秒`;
  return `${Math.floor(total / 3600)} 小时 ${Math.floor((total % 3600) / 60)} 分`;
}

function formatMoney(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(amount);
}

function initials(name) {
  const text = String(name || "--").trim();
  if (!text) return "--";
  if (/^[\u4e00-\u9fa5]/.test(text)) return text.slice(0, 2);
  return text.split(/\s+/).map(part => part[0]).join("").slice(0, 2).toUpperCase();
}

function pill(text, type = "neutral", extra = "") {
  return `<span class="pill ${type} ${extra}">${escapeHtml(text)}</span>`;
}

function conversationStatusLabel(status) {
  return ({
    WAITING: "待领取",
    ACTIVE: "服务中",
    IN_PROGRESS: "服务中",
    AI_ASSISTING: "AI 处理中",
    WAITING_CUSTOMER: "等待客户",
    WAITING_INTERNAL: "内部处理中",
    RESOLVED: "已解决",
    CLOSED: "已关闭"
  })[status] || status || "--";
}

function conversationStatusPill(status) {
  const type = ({
    WAITING: "amber",
    ACTIVE: "blue",
    IN_PROGRESS: "blue",
    AI_ASSISTING: "violet",
    WAITING_CUSTOMER: "cyan",
    WAITING_INTERNAL: "orange",
    RESOLVED: "green",
    CLOSED: "neutral"
  })[status] || "neutral";
  return pill(conversationStatusLabel(status), type);
}

function priorityLabel(priority) {
  return ({ URGENT: "紧急", HIGH: "高", NORMAL: "普通", LOW: "低" })[priority] || priority || "普通";
}

function priorityPill(priority) {
  const type = priority === "URGENT" ? "red" : priority === "HIGH" ? "orange" : priority === "LOW" ? "neutral" : "blue";
  return pill(priorityLabel(priority), type);
}

function ticketStatusLabel(status) {
  return ({ NEW: "待受理", ASSIGNED: "处理中", WAITING_CUSTOMER: "等待客户", RESOLVED: "已解决" })[status] || status || "--";
}

function statusPill(status) {
  const type = status === "RESOLVED" ? "green" : status === "ASSIGNED" ? "blue" : status === "WAITING_CUSTOMER" ? "cyan" : "amber";
  return pill(ticketStatusLabel(status), type);
}

function slaPill(ticket) {
  const status = ticket?.slaStatus || "ON_TRACK";
  const label = ticket?.slaBreached || status === "BREACHED" ? "已超时" : status === "DUE_SOON" ? "即将超时" : "正常";
  return pill(label, label === "已超时" ? "red" : label === "即将超时" ? "orange" : "green");
}

function orderStatusLabel(status) {
  return ({
    PAID_PICKING: "待拣货",
    SHIPPED: "运输中",
    DELIVERED: "已签收",
    REFUNDING: "退款中",
    CLOSED: "已关闭"
  })[status] || status || "--";
}

function orderStatusPill(status) {
  const type = status === "DELIVERED" ? "green" : status === "REFUNDING" ? "orange" : status === "CLOSED" ? "neutral" : status === "SHIPPED" ? "violet" : "blue";
  return pill(orderStatusLabel(status), type);
}

function riskLabel(riskLevel) {
  return ({ HIGH_VALUE: "高客单", FINANCE_REVIEW: "财务复核", LOW: "低风险", NORMAL: "常规" })[riskLevel] || riskLevel || "--";
}

function knowledgeStatusLabel(status) {
  return ({ DRAFT: "草稿", REVIEWING: "审核中", PUBLISHED: "已发布", DISABLED: "已下线" })[status] || status || "--";
}

function knowledgeStatusPill(status) {
  const type = status === "PUBLISHED" ? "green" : status === "REVIEWING" ? "violet" : status === "DISABLED" ? "neutral" : "amber";
  return pill(knowledgeStatusLabel(status), type);
}

function channelLabel(channel) {
  return ({ WEB: "官网", APP: "App", WECHAT: "微信", PHONE: "电话", HOTLINE: "电话", EMAIL: "邮件", MINI_PROGRAM: "小程序" })[channel] || channel || "在线";
}

function showToast(message, type = "success", title = "") {
  const host = $(".toast-container") || $("#toastHost") || (() => {
    const element = document.createElement("div");
    element.id = "toastHost";
    element.className = "toast-container";
    document.body.appendChild(element);
    return element;
  })();
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  const iconName = type === "error" ? "close" : type === "warning" ? "bell" : "check";
  const displayTitle = title || (type === "error" ? "操作失败" : type === "warning" ? "请注意" : type === "info" ? "提示" : "操作成功");
  toast.innerHTML = `
    <span class="toast-icon">${icon(iconName, 16)}</span>
    <div class="toast-body">
      <div class="toast-title">${escapeHtml(displayTitle)}</div>
      <div class="toast-message">${escapeHtml(message)}</div>
    </div>
    <button class="toast-close" aria-label="关闭">${icon("close", 14)}</button>
  `;
  host.appendChild(toast);
  toast.querySelector(".toast-close")?.addEventListener("click", () => {
    toast.style.animation = "toastFadeOut 240ms ease forwards";
    setTimeout(() => toast.remove(), 240);
  });
  setTimeout(() => {
    if (toast.parentNode) toast.remove();
  }, 4200);
}

function setBusy(button, busy, label = "处理中...") {
  if (!button) return;
  if (!button.dataset.defaultHtml) button.dataset.defaultHtml = button.innerHTML;
  button.disabled = busy;
  if (busy) {
    button.classList.add("loading");
    button.setAttribute("aria-busy", "true");
  } else {
    button.classList.remove("loading");
    button.removeAttribute("aria-busy");
    button.innerHTML = button.dataset.defaultHtml;
  }
}

function renderError(container, error, retry) {
  if (!container) return;
  container.innerHTML = `
    <div class="empty-state">
      <div class="empty-state-icon">${icon("pulse", 28)}</div>
      <h3>数据暂时不可用</h3>
      <p>${escapeHtml(error.message || "请稍后重试")}</p>
      ${retry ? '<button class="button secondary small" data-retry>重新加载</button>' : ""}
    </div>`;
  if (retry) $("[data-retry]", container)?.addEventListener("click", retry);
}

function emptyState(title, description = "", action = "") {
  return `<div class="empty-state"><div class="empty-state-icon">${icon("inbox", 28)}</div><h3>${escapeHtml(title)}</h3>${description ? `<p>${escapeHtml(description)}</p>` : ""}${action}</div>`;
}

function skeletonRows(count = 4) {
  return Array.from({ length: count }, () => `
    <div class="skeleton skeleton-text long"></div>
    <div class="skeleton skeleton-text medium"></div>
    <div class="skeleton skeleton-text short" style="margin-bottom:16px"></div>
  `).join("");
}

function skeletonCards(count = 3) {
  return Array.from({ length: count }, () => `<div class="skeleton skeleton-card" style="margin-bottom:12px"></div>`).join("");
}

function skeletonTable(rows = 5) {
  return Array.from({ length: rows }, () => `
    <div style="display:flex;gap:12px;padding:13px;border-bottom:1px solid var(--ink-100)">
      <div class="skeleton skeleton-text" style="flex:1.3"></div>
      <div class="skeleton skeleton-text" style="flex:1"></div>
      <div class="skeleton skeleton-text" style="flex:.5"></div>
    </div>
  `).join("");
}

function showPageLoading(message = "加载中...") {
  let overlay = $(".page-loading-overlay");
  if (!overlay) {
    overlay = document.createElement("div");
    overlay.className = "page-loading-overlay";
    document.body.appendChild(overlay);
  }
  overlay.innerHTML = `<div class="spinner"></div><p>${escapeHtml(message)}</p>`;
  overlay.style.display = "flex";
}

function hidePageLoading() {
  const overlay = $(".page-loading-overlay");
  if (overlay) overlay.style.display = "none";
}

function renderSidebar(activePage) {
  const sidebar = $("#sidebar");
  if (!sidebar) return;
  const activeRoot = activePage === "ticketDetail" ? "tickets" : activePage === "orderDetail" ? "orders" : activePage;
  sidebar.innerHTML = `
    <div class="brand">
      <div class="brand-mark"><span>✦</span></div>
      <div class="brand-copy">
        <strong>星桥客服</strong>
        <span>ServiceOS</span>
      </div>
      <button class="icon-button sidebar-close" id="sidebarClose" aria-label="关闭菜单">${icon("close")}</button>
    </div>
    <div class="workspace-switcher">
      <span class="workspace-avatar">华</span>
      <span><strong>华东售后中心</strong><small>在线业务组</small></span>
      ${icon("chevron", 15)}
    </div>
    <nav class="nav" aria-label="主导航">
      ${navGroups.map(group => `
        <div class="nav-group">
          <div class="nav-title">${group.title}</div>
          ${group.items.map(item => `
            <a class="nav-item${activeRoot === item.key ? " active" : ""}" href="${item.href}">
              <span class="nav-icon">${icon(item.icon)}</span>
              <span>${item.label}</span>
              ${item.badge === "queue" ? '<em class="nav-badge" id="navQueueBadge">--</em>' : ""}
            </a>
          `).join("")}
        </div>
      `).join("")}
    </nav>
    <div class="sidebar-foot">
      <div class="runtime-chip" id="runtimeBox">
        <span class="status-dot checking"></span>
        <span><strong>正在连接服务</strong><small>检查运行配置...</small></span>
      </div>
      <div class="operator-card">
        <span class="operator-avatar">林</span>
        <span><strong>林晓 · 在线</strong><small>售后二线客服</small></span>
        <button class="icon-button" aria-label="账号菜单">${icon("more", 17)}</button>
      </div>
    </div>`;

  $("#sidebarClose")?.addEventListener("click", closeSidebar);
}

function renderTopbar(activePage) {
  const topbar = $("#topbar");
  if (!topbar) return;
  const meta = pageMeta[activePage] || pageMeta.overview;
  const today = new Date().toLocaleDateString("zh-CN", { month: "long", day: "numeric", weekday: "short" });
  topbar.innerHTML = `
    <div class="topbar-left">
      <button class="icon-button menu-button" id="sidebarOpen" aria-label="打开菜单">${icon("menu")}</button>
      <div class="breadcrumb"><span>${escapeHtml(meta.section)}</span>${icon("chevron", 13)}<strong>${escapeHtml(meta.title)}</strong></div>
    </div>
    <div class="topbar-right">
      <span class="today-label">${escapeHtml(today)}</span>
      <button class="global-search" id="globalSearch" type="button">${icon("search", 16)}<span>搜索会话、工单、订单</span><kbd>⌘ K</kbd></button>
      <button class="icon-button notification-button" aria-label="通知">${icon("bell")}<span></span></button>
    </div>`;
  $("#sidebarOpen")?.addEventListener("click", openSidebar);
  $("#globalSearch")?.addEventListener("click", () => showToast("全局搜索将在后续接入 Elasticsearch", "info"));
}

function openSidebar() {
  document.body.classList.add("sidebar-open");
}

function closeSidebar() {
  document.body.classList.remove("sidebar-open");
}

function renderRuntime(runtime) {
  state.runtime = runtime;
  const box = $("#runtimeBox");
  if (!box) return;
  const configured = runtime?.configured !== false;
  box.innerHTML = `
    <span class="status-dot ${configured ? "online" : "warning"}"></span>
    <span>
      <strong>${configured ? "AI 服务正常" : "人工服务模式"}</strong>
      <small>${escapeHtml(runtime?.provider || "--")} · ${escapeHtml(runtime?.model || "--")}</small>
    </span>`;
}

async function loadRuntime() {
  try {
    const runtime = await request("/api/support/runtime", { timeout: 5000 });
    renderRuntime(runtime);
    return runtime;
  } catch (error) {
    const box = $("#runtimeBox");
    if (box) {
      box.innerHTML = `<span class="status-dot offline"></span><span><strong>后端未连接</strong><small>${escapeHtml(error.message)}</small></span>`;
    }
    return null;
  }
}

async function loadNavigationQueueCount() {
  try {
    const dashboard = await request("/api/support/conversations/dashboard", { timeout: 5000 });
    const count = dashboard?.waitingCount ?? dashboard?.waiting ?? 0;
    const badge = $("#navQueueBadge");
    if (badge) badge.textContent = count > 99 ? "99+" : String(count);
  } catch {
    const badge = $("#navQueueBadge");
    if (badge) badge.textContent = "·";
  }
}

function queryParam(name) {
  return new URLSearchParams(window.location.search).get(name);
}

function saveLastDecision(data) {
  try { localStorage.setItem("supportLastDecision", JSON.stringify(data)); } catch { /* 浏览器禁用存储时不影响主流程 */ }
}

function loadLastDecision() {
  try {
    const value = localStorage.getItem("supportLastDecision");
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

function initPage(activePage) {
  document.body.dataset.page = activePage;
  renderSidebar(activePage);
  renderTopbar(activePage);
  loadRuntime();
  loadNavigationQueueCount();
  document.addEventListener("keydown", event => {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
      event.preventDefault();
      $("#globalSearch")?.click();
    }
    if (event.key === "Escape") closeSidebar();
  });
}
