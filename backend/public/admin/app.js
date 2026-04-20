(() => {
  const state = {
    token: localStorage.getItem("betnow_admin_token") || null,
    email: localStorage.getItem("betnow_admin_email") || null,
    socket: null,
    chats: [],
    activeUserId: null,
  };

  const el = (id) => document.getElementById(id);

  const loginView = el("login");
  const appView = el("app");
  const loginForm = el("login-form");
  const loginError = el("login-error");
  const sessionEmail = el("session-email");
  const chatList = el("chat-list");
  const chatsCount = el("chats-count");
  const threadEmpty = el("thread-empty");
  const thread = el("thread");
  const threadEmail = el("thread-email");
  const threadMeta = el("thread-meta");
  const messages = el("messages");
  const replyForm = el("reply-form");
  const replyText = el("reply-text");

  function showLogin() {
    loginView.classList.remove("hidden");
    appView.classList.add("hidden");
  }
  function showApp() {
    loginView.classList.add("hidden");
    appView.classList.remove("hidden");
    sessionEmail.textContent = state.email || "";
  }

  async function login(email, password) {
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || "Login failed");
    }
    const data = await res.json();
    if (data.user.role !== "admin") {
      throw new Error("This account is not an admin");
    }
    state.token = data.token;
    state.email = data.user.email;
    localStorage.setItem("betnow_admin_token", state.token);
    localStorage.setItem("betnow_admin_email", state.email);
  }

  function logout() {
    localStorage.removeItem("betnow_admin_token");
    localStorage.removeItem("betnow_admin_email");
    if (state.socket) state.socket.disconnect();
    state.token = null;
    state.email = null;
    state.chats = [];
    state.activeUserId = null;
    showLogin();
  }

  async function fetchChats() {
    const res = await fetch("/api/support/chats", {
      headers: { Authorization: `Bearer ${state.token}` },
    });
    if (res.status === 401 || res.status === 403) {
      logout();
      return;
    }
    const data = await res.json();
    state.chats = data.chats || [];
    renderChatList();
  }

  function renderChatList() {
    chatsCount.textContent = state.chats.length;
    chatList.innerHTML = "";
    for (const chat of state.chats) {
      const li = document.createElement("li");
      li.dataset.userId = chat.userId;
      if (chat.userId === state.activeUserId) li.classList.add("active");
      li.innerHTML = `
        <div class="chat-row-top">
          <span class="chat-email">${escapeHtml(chat.userEmail)}</span>
          <span class="chat-time">${formatTime(chat.lastMessageAt)}</span>
        </div>
        <div class="chat-preview">
          ${escapeHtml(chat.preview || "No messages yet")}
          ${chat.unreadForAdmin ? `<span class="chat-unread">${chat.unreadForAdmin}</span>` : ""}
        </div>
      `;
      li.addEventListener("click", () => openChat(chat.userId));
      chatList.appendChild(li);
    }
  }

  function openChat(userId) {
    state.activeUserId = userId;
    [...chatList.children].forEach((li) => {
      li.classList.toggle("active", li.dataset.userId === userId);
    });
    state.socket.emit("support:adminOpen", { userId }, (res) => {
      if (!res || !res.ok) return;
      threadEmpty.classList.add("hidden");
      thread.classList.remove("hidden");
      threadEmail.textContent = res.userEmail;
      threadMeta.textContent = `User id: ${res.userId}`;
      renderMessages(res.messages);
      const chat = state.chats.find((c) => c.userId === userId);
      if (chat) {
        chat.unreadForAdmin = 0;
        renderChatList();
      }
    });
  }

  function renderMessages(list) {
    messages.innerHTML = "";
    for (const m of list) appendMessage(m);
    messages.scrollTop = messages.scrollHeight;
  }

  function appendMessage(m) {
    const div = document.createElement("div");
    div.className = `bubble ${m.from}`;
    div.innerHTML = `${escapeHtml(m.text)}<span class="ts">${formatTime(m.ts)}</span>`;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
  }

  function connectSocket() {
    state.socket = io({ transports: ["websocket", "polling"] });
    state.socket.on("connect", () => {
      state.socket.emit("support:auth", { token: state.token }, (res) => {
        if (!res || !res.ok) {
          console.error("Socket auth failed:", res && res.error);
          logout();
          return;
        }
        fetchChats();
      });
    });
    state.socket.on("support:message", (evt) => {
      upsertChat(evt);
      if (evt.userId === state.activeUserId) appendMessage(evt.message);
    });
  }

  function upsertChat(evt) {
    let chat = state.chats.find((c) => c.userId === evt.userId);
    if (!chat) {
      chat = {
        userId: evt.userId,
        userEmail: evt.userEmail,
        lastMessageAt: evt.message.ts,
        unreadForAdmin: evt.message.from === "user" ? 1 : 0,
        preview: evt.message.text.slice(0, 120),
        messageCount: 1,
      };
      state.chats.unshift(chat);
    } else {
      chat.lastMessageAt = evt.message.ts;
      chat.preview = evt.message.text.slice(0, 120);
      chat.messageCount += 1;
      if (evt.message.from === "user" && evt.userId !== state.activeUserId) {
        chat.unreadForAdmin += 1;
      }
      state.chats.sort((a, b) =>
        (b.lastMessageAt || "").localeCompare(a.lastMessageAt || "")
      );
    }
    renderChatList();
  }

  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    loginError.textContent = "";
    try {
      await login(el("email").value.trim(), el("password").value);
      showApp();
      connectSocket();
    } catch (err) {
      loginError.textContent = err.message;
    }
  });

  el("logout").addEventListener("click", logout);

  replyForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const text = replyText.value.trim();
    if (!text || !state.activeUserId) return;
    state.socket.emit(
      "support:send",
      { userId: state.activeUserId, text },
      (res) => {
        if (res && res.ok) replyText.value = "";
      }
    );
  });

  replyText.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      replyForm.requestSubmit();
    }
  });

  function formatTime(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    const now = new Date();
    const sameDay =
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate();
    return sameDay
      ? d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
      : d.toLocaleDateString();
  }
  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);
  }

  if (state.token) {
    showApp();
    connectSocket();
  } else {
    showLogin();
  }
})();
