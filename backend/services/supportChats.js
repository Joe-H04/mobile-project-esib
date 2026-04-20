const { v4: uuidv4 } = require("uuid");
const store = require("../data/store");

function findOrCreateChat(user) {
  let chat = store.supportChats.find((c) => c.userId === user.id);
  if (!chat) {
    chat = {
      userId: user.id,
      userEmail: user.email,
      messages: [],
      lastMessageAt: null,
      unreadForAdmin: 0,
      unreadForUser: 0,
    };
    store.supportChats.push(chat);
    store.persist();
  }
  return chat;
}

function appendMessage(chat, from, text) {
  const message = {
    id: uuidv4(),
    from,
    text,
    ts: new Date().toISOString(),
  };
  chat.messages.push(message);
  chat.lastMessageAt = message.ts;
  if (from === "user") chat.unreadForAdmin += 1;
  else chat.unreadForUser += 1;
  store.persist();
  return message;
}

function markRead(chat, forRole) {
  if (forRole === "admin") chat.unreadForAdmin = 0;
  else chat.unreadForUser = 0;
  store.persist();
}

function listChatSummaries() {
  return [...store.supportChats]
    .sort((a, b) => (b.lastMessageAt || "").localeCompare(a.lastMessageAt || ""))
    .map((c) => ({
      userId: c.userId,
      userEmail: c.userEmail,
      lastMessageAt: c.lastMessageAt,
      unreadForAdmin: c.unreadForAdmin,
      messageCount: c.messages.length,
      preview: c.messages.length
        ? c.messages[c.messages.length - 1].text.slice(0, 120)
        : "",
    }));
}

module.exports = {
  findOrCreateChat,
  appendMessage,
  markRead,
  listChatSummaries,
};
