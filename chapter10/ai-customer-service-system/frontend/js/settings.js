/* 设置页只保存本地开发连接；模型密钥始终由后端环境变量管理。 */

const providers = [
  { id: "DEEPSEEK", name: "DeepSeek", logo: "DS", description: "默认生产模型，使用 DEEPSEEK_API_KEY。" },
  { id: "OLLAMA", name: "Ollama", logo: "OL", description: "本地模型运行时，默认连接 /v1 兼容接口。" },
  { id: "CHATGPT", name: "ChatGPT", logo: "AI", description: "OpenAI 模型，通过 OPENAI_API_KEY 启用。" }
];

function renderProviders(runtime) {
  const activeProvider = String(runtime?.provider || "DEEPSEEK").toUpperCase();
  $("#providerGrid").innerHTML = providers.map(provider => `
    <article class="provider-card${provider.id === activeProvider ? " active" : ""}">
      <span class="provider-logo" style="${provider.id === "DEEPSEEK" ? "background:#1769e8" : provider.id === "OLLAMA" ? "background:#252b36" : "background:#0d8c6e"}">${provider.logo}</span>
      <span class="provider-state">${provider.id === activeProvider ? pill("当前使用", "blue") : pill("可切换", "outline")}</span>
      <h4>${provider.name}</h4><p>${provider.description}</p>
    </article>`).join("");
}

function renderRuntimeSettings(runtime) {
  const configured = runtime?.configured !== false;
  $("#modelConfigState").innerHTML = configured ? pill("配置已就绪", "green") : pill("需要配置", "red");
  $("#runtimeDetail").innerHTML = `<div class="definition-grid">
    <div class="definition-item"><span>Provider</span><strong>${escapeHtml(runtime?.provider || "--")}</strong></div>
    <div class="definition-item"><span>Model</span><strong>${escapeHtml(runtime?.model || "--")}</strong></div>
    <div class="definition-item"><span>Base URL</span><strong class="mono">${escapeHtml(runtime?.baseUrl || "--")}</strong></div>
    <div class="definition-item"><span>业务存储</span><strong>${escapeHtml(runtime?.storage || "--")}</strong></div>
    <div class="definition-item"><span>配置状态</span><strong class="${configured ? "text-success" : "text-danger"}">${configured ? "可调用" : "人工服务模式"}</strong></div>
    <div class="definition-item"><span>配置提示</span><strong>${escapeHtml(runtime?.configurationMessage || "READY")}</strong></div>
  </div><div class="health-strip" style="margin:15px 0 0"><div class="health-title">${icon("check", 14)}安全边界</div><span>浏览器无法读取、修改或回显模型密钥；切换供应商需要重启 Spring Boot。</span></div>`;
}

async function refreshSettingsRuntime() {
  const button = $("#refreshRuntime");
  setBusy(button, true, "检查中");
  try {
    const runtime = await request("/api/support/runtime", { timeout: 5000 });
    renderProviders(runtime);
    renderRuntimeSettings(runtime);
  } catch (error) {
    renderProviders({});
    renderError($("#runtimeDetail"), error, refreshSettingsRuntime);
  } finally {
    setBusy(button, false);
  }
}

async function saveAndTestConnection() {
  const value = $("#apiBase").value.trim().replace(/\/$/, "");
  if (!/^https?:\/\//.test(value)) return showToast("请输入完整的 http 或 https 地址", "error");
  sessionStorage.setItem("supportAccessToken", $("#accessToken").value.trim());
  localStorage.setItem("supportApiBase", value);
  const result = $("#connectionResult");
  result.hidden = false;
  result.innerHTML = '<span class="spinner"></span><span>正在检查后端运行状态...</span>';
  try {
    const runtime = await request("/api/support/runtime", { timeout: 5000 });
    result.innerHTML = `<div class="health-title">${icon("check", 14)}连接成功</div><span>${escapeHtml(runtime.provider)} · ${escapeHtml(runtime.model)} · ${escapeHtml(runtime.storage)}</span>`;
    showToast("开发连接已保存");
  } catch (error) {
    result.style.background = "var(--red-50)";
    result.style.borderColor = "var(--red-100)";
    result.innerHTML = `<div class="health-title text-danger">${icon("close", 14)}连接失败</div><span class="text-danger">${escapeHtml(error.message)}</span>`;
  }
}

initPage("settings");
$("#accessToken").value = sessionStorage.getItem("supportAccessToken") || "";
$("#settingIconConnection").innerHTML = icon("pulse", 15);
$("#settingIconQueue").innerHTML = icon("inbox", 15);
$("#settingIconChannel").innerHTML = icon("external", 15);
$("#apiBase").value = localStorage.getItem("supportApiBase") || "http://localhost:18081";
refreshSettingsRuntime();

$$('[data-settings]').forEach(button => button.addEventListener("click", () => {
  $$('[data-settings]').forEach(item => item.classList.remove("active"));
  $$('[data-settings-section]').forEach(item => item.classList.remove("active"));
  button.classList.add("active");
  $(`[data-settings-section="${button.dataset.settings}"]`).classList.add("active");
}));
$("#refreshRuntime")?.addEventListener("click", refreshSettingsRuntime);
$("#saveApiBase")?.addEventListener("click", saveAndTestConnection);
$("#resetApiBase")?.addEventListener("click", () => {
  localStorage.removeItem("supportApiBase");
  $("#apiBase").value = "http://localhost:18081";
  showToast("已恢复默认连接地址");
});
