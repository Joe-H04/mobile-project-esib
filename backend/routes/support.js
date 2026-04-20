const express = require("express");
const store = require("../data/store");
const authMiddleware = require("../middleware/auth");
const chats = require("../services/supportChats");

const router = express.Router();

function requireAdmin(req, res, next) {
  if (req.user.role !== "admin") {
    return res.status(403).json({ error: "Admin only" });
  }
  next();
}

router.get("/me", authMiddleware, (req, res) => {
  const user = store.users.find((u) => u.id === req.user.userId);
  if (!user) return res.status(404).json({ error: "User not found" });
  const chat = chats.findOrCreateChat(user);
  chats.markRead(chat, "user");
  res.json({ messages: chat.messages });
});

router.get("/chats", authMiddleware, requireAdmin, (req, res) => {
  res.json({ chats: chats.listChatSummaries() });
});

router.get("/chats/:userId", authMiddleware, requireAdmin, (req, res) => {
  const chat = store.supportChats.find((c) => c.userId === req.params.userId);
  if (!chat) return res.status(404).json({ error: "Chat not found" });
  chats.markRead(chat, "admin");
  res.json({
    userId: chat.userId,
    userEmail: chat.userEmail,
    messages: chat.messages,
  });
});

module.exports = router;
