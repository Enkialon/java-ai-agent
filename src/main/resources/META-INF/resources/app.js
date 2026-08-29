(() => {
  "use strict";

  const STORAGE_KEY = "coding-agent.sessions";
  const USER_KEY = "coding-agent.userId";

  const els = {
    timeline: document.getElementById("timeline"),
    statusBar: document.getElementById("statusBar"),
    promptInput: document.getElementById("promptInput"),
    sendBtn: document.getElementById("sendBtn"),
    sessionList: document.getElementById("sessionList"),
    sessionLabel: document.getElementById("sessionLabel"),
    modelLabel: document.getElementById("modelLabel"),
    workspaceInput: document.getElementById("workspaceInput"),
    bindWorkspaceBtn: document.getElementById("bindWorkspaceBtn"),
    newSessionBtn: document.getElementById("newSessionBtn"),
  };

  /** @type {{id:string,title:string,createdAt:number}[]} */
  let sessions = loadSessions();
  let currentSessionId = sessions[0]?.id || createSessionId();
  let userId = localStorage.getItem(USER_KEY) || createId("user");
  localStorage.setItem(USER_KEY, userId);

  if (!sessions.find((s) => s.id === currentSessionId)) {
    sessions.unshift({ id: currentSessionId, title: "Session", createdAt: Date.now() });
    saveSessions();
  }

  let running = false;
  /** @type {HTMLElement|null} */
  let currentTextEl = null;
  let currentTextRaw = "";
  /** @type {Map<string, HTMLElement>} */
  const toolCards = new Map();

  ensureSession();
  renderSessionList();
  refreshStatus();

  els.sendBtn.addEventListener("click", () => void sendPrompt());
  els.newSessionBtn.addEventListener("click", () => newSession());
  els.bindWorkspaceBtn.addEventListener("click", () => void bindWorkspace());
  els.promptInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void sendPrompt();
    }
  });

  function authHeaders(extra = {}) {
    return {
      "X-Agent-Session-Id": currentSessionId,
      "X-Agent-User-Id": userId,
      ...extra,
    };
  }

  async function refreshStatus() {
    try {
      const res = await fetch("/api/agent/status", { headers: authHeaders() });
      if (!res.ok) return;
      const data = await res.json();
      els.modelLabel.textContent = data.model || data.modelClient || "—";
      els.sessionLabel.textContent = shortId(currentSessionId);
      if (data.workspace) {
        els.workspaceInput.value = data.workspace;
      }
    } catch {
      /* ignore */
    }
  }

  async function bindWorkspace() {
    const path = els.workspaceInput.value.trim();
    if (!path) return;
    const res = await fetch("/api/agent/session/workspace", {
      method: "PUT",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ path }),
    });
    if (!res.ok) {
      setStatus("Bind workspace failed: " + (await res.text()), "error");
      return;
    }
    const data = await res.json();
    els.workspaceInput.value = data.path;
    setStatus("Workspace bound", "done");
  }

  async function sendPrompt() {
    const message = els.promptInput.value.trim();
    if (!message || running) return;

    if (!sessions.find((s) => s.id === currentSessionId)?.title
        || sessions.find((s) => s.id === currentSessionId)?.title === "Session") {
      const s = sessions.find((x) => x.id === currentSessionId);
      if (s) {
        s.title = message.slice(0, 48);
        saveSessions();
        renderSessionList();
      }
    }

    running = true;
    setSendEnabled(false);
    els.promptInput.value = "";
    toolCards.clear();
    currentTextEl = null;
    currentTextRaw = "";

    appendTask(message);
    setStatus("Working…", "running");

    try {
      await runAgent(message);
      setStatus("Done", "done");
    } catch (err) {
      setStatus(err.message || String(err), "error");
      appendSystemError(err.message || String(err));
    } finally {
      running = false;
      setSendEnabled(true);
      finalizeText();
    }
  }

  async function runAgent(message) {
    const res = await fetch("/api/agent/chat", {
      method: "POST",
      headers: authHeaders({
        "Content-Type": "application/json",
        Accept: "text/event-stream",
      }),
      body: JSON.stringify({ message }),
    });

    if (!res.ok) {
      throw new Error("Chat failed: HTTP " + res.status);
    }
    if (!res.body) {
      throw new Error("No response body (SSE unsupported)");
    }

    await readSse(res.body, (event) => handleEvent(event));
  }

  async function readSse(body, onEvent) {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let sep;
      while ((sep = buffer.indexOf("\n\n")) >= 0) {
        const chunk = buffer.slice(0, sep);
        buffer = buffer.slice(sep + 2);
        const dataLines = chunk
          .split("\n")
          .filter((line) => line.startsWith("data:"))
          .map((line) => line.slice(5).trimStart());
        if (!dataLines.length) continue;
        const raw = dataLines.join("\n");
        if (!raw || raw === "[DONE]") continue;
        try {
          onEvent(JSON.parse(raw));
        } catch (e) {
          console.warn("Bad SSE JSON", raw, e);
        }
      }
    }
  }

  function handleEvent(ev) {
    switch (ev.type) {
      case "agent_start":
        setStatus("Working…", "running");
        break;
      case "text_delta":
        handleTextDelta(ev.delta || "");
        break;
      case "message_end":
        finalizeText();
        break;
      case "tool_approval_required":
        handleApprovalRequired(ev);
        break;
      case "tool_approval_resolved":
        handleApprovalResolved(ev);
        break;
      case "tool_start":
        handleToolStart(ev);
        break;
      case "tool_end":
        handleToolEnd(ev);
        break;
      case "agent_end":
        finalizeText();
        setStatus("Done", "done");
        break;
      default:
        console.debug("Unknown event", ev);
    }
  }

  function handleTextDelta(delta) {
    if (!currentTextEl) {
      currentTextEl = document.createElement("div");
      currentTextEl.className = "item-text streaming";
      currentTextEl.innerHTML = '<div class="md"></div>';
      els.timeline.appendChild(currentTextEl);
      currentTextRaw = "";
    }
    currentTextRaw += delta;
    currentTextEl.querySelector(".md").innerHTML = renderMarkdown(currentTextRaw);
    scrollToBottom();
  }

  function finalizeText() {
    if (currentTextEl) {
      currentTextEl.classList.remove("streaming");
      currentTextEl = null;
      currentTextRaw = "";
    }
  }

  function handleApprovalRequired(ev) {
    finalizeText();
    const card = ensureToolCard(ev.callId, ev.toolName, ev.arguments);
    card.classList.add("pending-approval");
    card.classList.remove("running", "success", "error");
    setToolStatus(card, "◐");
    updateToolSummary(card, ev.toolName, ev.arguments);

    let banner = card.querySelector(".approval-banner");
    if (!banner) {
      banner = document.createElement("div");
      banner.className = "approval-banner";
      card.appendChild(banner);
    }
    banner.textContent = `Approval required (${ev.permission})`;

    let actions = card.querySelector(".approval-actions");
    if (!actions) {
      actions = document.createElement("div");
      actions.className = "tool-actions approval-actions";
      actions.innerHTML = `
        <button type="button" class="btn approve">Approve</button>
        <button type="button" class="btn deny">Deny</button>`;
      card.appendChild(actions);
      actions.querySelector(".approve").addEventListener("click", () => void decide(ev.callId, true));
      actions.querySelector(".deny").addEventListener("click", () => void decide(ev.callId, false));
    }
    setStatus("Waiting for approval…", "running");
    scrollToBottom();
  }

  function handleApprovalResolved(ev) {
    const card = toolCards.get(ev.callId);
    if (!card) return;
    card.classList.remove("pending-approval");
    const actions = card.querySelector(".approval-actions");
    if (actions) actions.remove();
    const banner = card.querySelector(".approval-banner");
    if (banner) {
      banner.textContent = ev.approved ? "Approved" : "Denied";
      banner.style.color = ev.approved ? "var(--success)" : "var(--danger)";
    }
    if (!ev.approved) {
      card.classList.add("error");
      setToolStatus(card, "✕");
    }
  }

  function handleToolStart(ev) {
    finalizeText();
    const card = ensureToolCard(ev.callId, ev.toolName, ev.arguments);
    card.classList.add("running");
    card.classList.remove("pending-approval", "success", "error");
    setToolStatus(card, "●");
    updateToolSummary(card, ev.toolName, ev.arguments);
    maybePrepareToolExtras(card, ev.toolName, ev.arguments);
    scrollToBottom();
  }

  function handleToolEnd(ev) {
    const card = toolCards.get(ev.callId) || ensureToolCard(ev.callId, "tool", "{}");
    card.classList.remove("running", "pending-approval");
    const ok = ev.success !== false && !String(ev.result || "").startsWith("Error:");
    card.classList.toggle("success", ok);
    card.classList.toggle("error", !ok);
    setToolStatus(card, ok ? "✓" : "✕");

    const toolName = card.dataset.toolName || "tool";
    const args = safeParse(card.dataset.arguments || "{}");
    fillToolResult(card, toolName, args, ev.result || "", ok);
    scrollToBottom();
  }

  async function decide(callId, approved) {
    const path = approved ? "approve" : "deny";
    const res = await fetch(`/api/agent/session/approvals/${encodeURIComponent(callId)}/${path}`, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
    });
    if (!res.ok) {
      setStatus("Approval failed: " + (await res.text()), "error");
    }
  }

  function ensureToolCard(callId, toolName, argumentsJson) {
    let card = toolCards.get(callId);
    if (card) {
      if (toolName) card.dataset.toolName = toolName;
      if (argumentsJson) card.dataset.arguments = argumentsJson;
      return card;
    }
    card = document.createElement("div");
    card.className = "tool-card";
    card.dataset.callId = callId;
    card.dataset.toolName = toolName || "tool";
    card.dataset.arguments = argumentsJson || "{}";
    card.innerHTML = `
      <div class="tool-head">
        <span class="tool-status">▸</span>
        <span class="tool-name"></span>
        <span class="tool-summary"></span>
        <span class="tool-stats"></span>
      </div>
      <div class="tool-body"></div>
      <div class="tool-actions result-actions"></div>`;
    card.querySelector(".tool-name").textContent = toolName || "tool";
    updateToolSummary(card, toolName, argumentsJson);
    els.timeline.appendChild(card);
    toolCards.set(callId, card);
    return card;
  }

  function setToolStatus(card, symbol) {
    card.querySelector(".tool-status").textContent = symbol;
  }

  function updateToolSummary(card, toolName, argumentsJson) {
    card.querySelector(".tool-name").textContent = toolName || card.dataset.toolName;
    const args = safeParse(argumentsJson || card.dataset.arguments || "{}");
    const summary = card.querySelector(".tool-summary");
    if (toolName === "bash") {
      summary.textContent = args.command ? "$ " + args.command : "";
    } else if (toolName === "read" || toolName === "write" || toolName === "edit") {
      summary.textContent = args.path || "";
    } else {
      summary.textContent = "";
    }
  }

  function maybePrepareToolExtras(card, toolName, argumentsJson) {
    if (toolName !== "edit") return;
    const args = safeParse(argumentsJson || "{}");
    if (args.old_string == null || args.new_string == null) return;
    const stats = lineStats(args.old_string, args.new_string);
    card.querySelector(".tool-stats").textContent = `+${stats.added} -${stats.removed}`;
  }

  function fillToolResult(card, toolName, args, result, ok) {
    const body = card.querySelector(".tool-body");
    const actions = card.querySelector(".result-actions");
    actions.innerHTML = "";
    body.classList.remove("open");
    body.innerHTML = "";

    if (toolName === "bash") {
      const pre = document.createElement("pre");
      pre.className = "terminal";
      const cmd = args.command ? `$ ${args.command}\n\n` : "";
      const full = cmd + stripErrorPrefix(result);
      pre.textContent = truncate(full, 4000);
      body.appendChild(pre);
      body.classList.add("open");
      if (full.length > 4000) {
        addToggle(actions, body, () => {
          pre.textContent = full;
        }, "Expand output");
      }
      return;
    }

    if (toolName === "edit" && args.old_string != null && args.new_string != null) {
      const stats = lineStats(args.old_string, args.new_string);
      card.querySelector(".tool-stats").textContent = `+${stats.added} -${stats.removed}`;
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn linkish";
      btn.textContent = "View Diff";
      btn.addEventListener("click", () => {
        body.classList.toggle("open");
        if (!body.dataset.ready) {
          body.innerHTML = `<div class="diff-box">${renderDiff(args.path || "file", args.old_string, args.new_string)}</div>`;
          body.dataset.ready = "1";
        }
        btn.textContent = body.classList.contains("open") ? "Hide Diff" : "View Diff";
      });
      actions.appendChild(btn);
      return;
    }

    if (toolName === "read" || toolName === "write" || !ok) {
      const pre = document.createElement("pre");
      pre.className = "terminal";
      pre.textContent = truncate(stripErrorPrefix(result), 3000);
      body.appendChild(pre);
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn linkish";
      btn.textContent = "Expand";
      btn.addEventListener("click", () => {
        body.classList.toggle("open");
        btn.textContent = body.classList.contains("open") ? "Collapse" : "Expand";
      });
      actions.appendChild(btn);
    }
  }

  function addToggle(actions, body, onExpand, label) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "btn linkish";
    btn.textContent = label;
    btn.addEventListener("click", () => {
      onExpand();
      body.classList.add("open");
      btn.remove();
    });
    actions.appendChild(btn);
  }

  function renderDiff(path, oldStr, newStr) {
    if (typeof Diff === "undefined" || typeof Diff2Html === "undefined") {
      return `<pre class="terminal">${escapeHtml(oldStr)}\n---\n${escapeHtml(newStr)}</pre>`;
    }
    const patch = Diff.createPatch(path || "file", oldStr, newStr, "", "");
    return Diff2Html.html(patch, {
      drawFileList: false,
      matching: "lines",
      outputFormat: "line-by-line",
    });
  }

  function lineStats(oldStr, newStr) {
    if (typeof Diff === "undefined") {
      const a = (oldStr || "").split("\n").length;
      const b = (newStr || "").split("\n").length;
      return { added: Math.max(0, b - a), removed: Math.max(0, a - b) };
    }
    const parts = Diff.diffLines(oldStr || "", newStr || "");
    let added = 0;
    let removed = 0;
    for (const p of parts) {
      const n = p.count || p.value.split("\n").length - (p.value.endsWith("\n") ? 1 : 0);
      if (p.added) added += n;
      if (p.removed) removed += n;
    }
    return { added, removed };
  }

  function appendTask(message) {
    const el = document.createElement("div");
    el.className = "item-task";
    el.innerHTML = `<div class="label">Task</div><div class="body"></div>`;
    el.querySelector(".body").textContent = message;
    els.timeline.appendChild(el);
    scrollToBottom();
  }

  function appendSystemError(msg) {
    const el = document.createElement("div");
    el.className = "tool-card error";
    el.textContent = msg;
    els.timeline.appendChild(el);
  }

  function renderMarkdown(text) {
    if (typeof marked !== "undefined") {
      return marked.parse(text, { breaks: true });
    }
    return escapeHtml(text).replace(/\n/g, "<br>");
  }

  function setStatus(text, kind) {
    els.statusBar.textContent = text;
    els.statusBar.className = "status-bar " + (kind || "idle");
  }

  function setSendEnabled(enabled) {
    els.sendBtn.disabled = !enabled;
    els.promptInput.disabled = !enabled;
  }

  function scrollToBottom() {
    els.timeline.scrollTop = els.timeline.scrollHeight;
  }

  function newSession() {
    if (running) return;
    currentSessionId = createSessionId();
    sessions.unshift({ id: currentSessionId, title: "Session", createdAt: Date.now() });
    saveSessions();
    els.timeline.innerHTML = "";
    toolCards.clear();
    currentTextEl = null;
    renderSessionList();
    refreshStatus();
    setStatus("Ready", "idle");
  }

  function switchSession(id) {
    if (running || id === currentSessionId) return;
    currentSessionId = id;
    els.timeline.innerHTML = "";
    toolCards.clear();
    currentTextEl = null;
    renderSessionList();
    refreshStatus();
    setStatus("Ready (history not loaded)", "idle");
  }

  function renderSessionList() {
    els.sessionList.innerHTML = "";
    for (const s of sessions) {
      const li = document.createElement("li");
      li.textContent = s.title || shortId(s.id);
      if (s.id === currentSessionId) li.classList.add("active");
      li.addEventListener("click", () => switchSession(s.id));
      els.sessionList.appendChild(li);
    }
    els.sessionLabel.textContent = shortId(currentSessionId);
  }

  function ensureSession() {
    saveSessions();
  }

  function loadSessions() {
    try {
      const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      return Array.isArray(raw) ? raw : [];
    } catch {
      return [];
    }
  }

  function saveSessions() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.slice(0, 30)));
  }

  function createSessionId() {
    return createId("sess");
  }

  function createId(prefix) {
    return prefix + "_" + Math.random().toString(36).slice(2, 10) + Date.now().toString(36);
  }

  function shortId(id) {
    return String(id).slice(-8);
  }

  function safeParse(json) {
    try {
      return JSON.parse(json);
    } catch {
      return {};
    }
  }

  function stripErrorPrefix(text) {
    return String(text || "").replace(/^Error:\s*/, "");
  }

  function truncate(text, max) {
    if (text.length <= max) return text;
    return text.slice(0, max) + "\n…[truncated]";
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }
})();
