/* 知识运营：编辑者提交审核，审核者发布，线上版本才参与 RAG。 */

const KNOWLEDGE_EDITOR = "knowledge-linxiao";
const KNOWLEDGE_REVIEWER = "knowledge-reviewer";
const knowledgePageState = { status: "", keyword: "", source: "" };

function filteredKnowledge() {
  const keyword = knowledgePageState.keyword.trim().toLowerCase();
  return state.knowledgeDocuments.filter(document => {
    const searchable = [document.documentId, document.title, document.content, document.sourceName].filter(Boolean).join(" ").toLowerCase();
    return (!knowledgePageState.status || document.status === knowledgePageState.status)
      && (!knowledgePageState.source || document.sourceType === knowledgePageState.source)
      && (!keyword || searchable.includes(keyword));
  });
}

function renderKnowledgeMetrics() {
  const count = status => state.knowledgeDocuments.filter(document => document.status === status).length;
  const all = state.knowledgeDocuments.length;
  const draft = count("DRAFT");
  const review = count("REVIEWING");
  const published = count("PUBLISHED");
  const disabled = count("DISABLED");
  $("#knowledgeStatAll").textContent = all;
  $("#knowledgeStatDraft").textContent = draft;
  $("#knowledgeStatReview").textContent = review;
  $("#knowledgeStatPublished").textContent = published;
  $("#knowledgeNavAll").textContent = all;
  $("#knowledgeNavDraft").textContent = draft;
  $("#knowledgeNavReview").textContent = review;
  $("#knowledgeNavPublished").textContent = published;
  $("#knowledgeNavDisabled").textContent = disabled;
}

function renderKnowledgeDocuments() {
  const documents = filteredKnowledge();
  $("#knowledgeResultCount").textContent = `共 ${documents.length} 篇`;
  if (!documents.length) {
    $("#knowledgeDocumentsResult").innerHTML = emptyState("没有符合条件的知识", "可以切换知识空间或调整搜索条件");
    return;
  }
  $("#knowledgeDocumentsResult").innerHTML = `
    <table class="data-table">
      <thead><tr><th>文档</th><th>来源</th><th>版本</th><th>状态</th><th>维护人</th><th>更新时间</th><th>操作</th></tr></thead>
      <tbody>${documents.map(document => `
        <tr>
          <td><div class="cell-main" style="max-width:360px"><strong>${escapeHtml(document.title)}</strong><small class="mono">${escapeHtml(document.documentId)}</small><small style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${escapeHtml(document.content)}</small></div></td>
          <td><div class="cell-main"><strong>${escapeHtml(document.sourceName || "控制台")}</strong><small>${escapeHtml(document.sourceType)}</small></div></td>
          <td>${pill(document.version, "outline")}</td>
          <td>${knowledgeStatusPill(document.status)}</td>
          <td>${escapeHtml(document.operator || "--")}</td>
          <td class="nowrap">${escapeHtml(formatDate(document.updatedAt))}</td>
          <td><div class="inline-actions"><button class="button ghost tiny" type="button" data-preview="${escapeHtml(document.documentId)}">查看切分</button>${document.status === "DRAFT" ? `<button class="button secondary tiny" type="button" data-submit="${escapeHtml(document.documentId)}">提交审核</button>` : ""}${document.status === "REVIEWING" ? `<button class="button tiny" type="button" data-publish="${escapeHtml(document.documentId)}">审核发布</button>` : ""}${document.status === "PUBLISHED" ? `<button class="button danger tiny" type="button" data-disable="${escapeHtml(document.documentId)}">下线</button>` : ""}</div></td>
        </tr>`).join("")}</tbody>
    </table>`;
  $$('[data-preview]').forEach(button => button.addEventListener("click", () => previewKnowledgeChunks(button.dataset.preview)));
  $$('[data-submit]').forEach(button => button.addEventListener("click", () => knowledgeAction(button.dataset.submit, "submit-review", KNOWLEDGE_EDITOR, button)));
  $$('[data-publish]').forEach(button => button.addEventListener("click", () => knowledgeAction(button.dataset.publish, "publish", KNOWLEDGE_REVIEWER, button)));
  $$('[data-disable]').forEach(button => button.addEventListener("click", () => knowledgeAction(button.dataset.disable, "disable", KNOWLEDGE_REVIEWER, button)));
}

async function loadKnowledgeDocuments() {
  $("#knowledgeDocumentsResult").innerHTML = skeletonRows(5);
  try {
    state.knowledgeDocuments = await request("/api/support/knowledge/documents");
    renderKnowledgeMetrics();
    renderKnowledgeDocuments();
  } catch (error) {
    renderError($("#knowledgeDocumentsResult"), error, loadKnowledgeDocuments);
  }
}

function openImportModal() {
  $("#importModal").classList.add("open");
  setTimeout(() => $("#knowledgeTitle").focus(), 60);
}

function closeImportModal() {
  $("#importModal").classList.remove("open");
}

async function importKnowledge() {
  const title = $("#knowledgeTitle").value.trim();
  const content = $("#knowledgeContent").value.trim();
  if (!title || !content) return showToast("标题和正文不能为空", "error");
  const button = $("#importKnowledge");
  setBusy(button, true, "导入中");
  try {
    const document = await request("/api/support/knowledge/documents", {
      method: "POST",
      body: {
        title,
        content,
        version: $("#knowledgeVersion").value.trim() || "v1",
        sourceType: $("#knowledgeSourceType").value,
        sourceName: $("#knowledgeSourceName").value.trim() || "控制台录入",
        operator: KNOWLEDGE_EDITOR
      }
    });
    state.selectedKnowledgeId = document.documentId;
    closeImportModal();
    await loadKnowledgeDocuments();
    showToast(`知识 ${document.documentId} 已导入为草稿`);
    $("#knowledgeTitle").value = "";
    $("#knowledgeContent").value = "";
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

async function knowledgeAction(documentId, action, operator, button) {
  setBusy(button, true, "提交中");
  try {
    const document = await request(`/api/support/knowledge/documents/${encodeURIComponent(documentId)}/${action}`, { method: "POST", body: { operator } });
    await loadKnowledgeDocuments();
    showToast(`知识状态已更新为${knowledgeStatusLabel(document.status)}`);
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setBusy(button, false);
  }
}

async function previewKnowledgeChunks(documentId) {
  $("#chunkModal").classList.add("open");
  $("#chunkModalBody").innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
  try {
    const chunks = await request(`/api/support/knowledge/documents/${encodeURIComponent(documentId)}/chunks`);
    $("#chunkModalBody").innerHTML = chunks.length ? chunks.map((chunk, index) => `
      <article class="retrieval-result"><div class="retrieval-result-head"><span class="retrieval-rank">${index + 1}</span><strong>${escapeHtml(chunk.title || `Chunk ${index + 1}`)}</strong>${pill(chunk.version, "violet")}</div><p>${escapeHtml(chunk.text)}</p><footer><span class="mono">${escapeHtml(chunk.chunkId)}</span><span>字符数 ${chunk.text?.length || 0}</span></footer></article>`).join("") : emptyState("没有可预览的 Chunk", "知识发布时会生成并同步向量切分");
  } catch (error) {
    renderError($("#chunkModalBody"), error);
  }
}

function closeChunkModal() { $("#chunkModal").classList.remove("open"); }

initPage("knowledge");
$("#openImportModal").innerHTML = `${icon("plus", 15)}导入知识`;
$("#knowledgeSearchIcon").innerHTML = icon("search", 15);
$("#knowledgeIconAll").innerHTML = icon("book", 17);
$("#knowledgeIconPublished").innerHTML = icon("check", 17);
$("#knowledgeIconReview").innerHTML = icon("clock", 17);
$("#knowledgeIconDraft").innerHTML = icon("plus", 17);
$("#closeImportModal").innerHTML = icon("close", 17);
$("#closeChunkModal").innerHTML = icon("close", 17);
loadKnowledgeDocuments();

$$('[data-knowledge-status]').forEach(button => button.addEventListener("click", () => {
  $$('[data-knowledge-status]').forEach(item => item.classList.remove("active"));
  button.classList.add("active");
  knowledgePageState.status = button.dataset.knowledgeStatus;
  renderKnowledgeDocuments();
}));
$("#knowledgeKeyword")?.addEventListener("input", event => { knowledgePageState.keyword = event.target.value; renderKnowledgeDocuments(); });
$("#knowledgeSourceFilter")?.addEventListener("change", event => { knowledgePageState.source = event.target.value; renderKnowledgeDocuments(); });
$("#refreshKnowledge")?.addEventListener("click", loadKnowledgeDocuments);
$("#openImportModal")?.addEventListener("click", openImportModal);
$("#closeImportModal")?.addEventListener("click", closeImportModal);
$("#cancelImportModal")?.addEventListener("click", closeImportModal);
$("#importKnowledge")?.addEventListener("click", importKnowledge);
$("#closeChunkModal")?.addEventListener("click", closeChunkModal);
$("#importModal")?.addEventListener("click", event => { if (event.target === event.currentTarget) closeImportModal(); });
$("#chunkModal")?.addEventListener("click", event => { if (event.target === event.currentTarget) closeChunkModal(); });
