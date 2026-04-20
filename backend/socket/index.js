const { Server } = require("socket.io");
const jwt = require("jsonwebtoken");
const config = require("../config");
const store = require("../data/store");
const chats = require("../services/supportChats");

const ADMIN_ROOM = "support:admins";
const userRoom = (userId) => `support:user:${userId}`;

function setupSocket(server) {
  const io = new Server(server, {
    cors: { origin: "*" },
  });

  io.on("connection", (socket) => {
    console.log(`Client connected: ${socket.id}`);

    socket.on("support:auth", (payload, ack) => {
      try {
        const token = payload && payload.token;
        if (!token) throw new Error("No token");
        const decoded = jwt.verify(token, config.JWT_SECRET);
        socket.data.userId = decoded.userId;
        socket.data.email = decoded.email;
        socket.data.role = decoded.role || "user";

        if (socket.data.role === "admin") {
          socket.join(ADMIN_ROOM);
          if (ack) ack({ ok: true, role: "admin" });
        } else {
          const user = store.users.find((u) => u.id === decoded.userId);
          if (!user) throw new Error("User not found");
          const chat = chats.findOrCreateChat(user);
          chats.markRead(chat, "user");
          socket.join(userRoom(user.id));
          if (ack)
            ack({ ok: true, role: "user", messages: chat.messages });
        }
      } catch (err) {
        if (ack) ack({ ok: false, error: err.message });
      }
    });

    socket.on("support:send", (payload, ack) => {
      try {
        if (!socket.data.userId) throw new Error("Not authed");
        const text = (payload && payload.text || "").toString().trim();
        if (!text) throw new Error("Empty message");
        if (text.length > 2000) throw new Error("Message too long");

        const isAdmin = socket.data.role === "admin";
        const targetUserId = isAdmin ? payload.userId : socket.data.userId;
        if (!targetUserId) throw new Error("Missing userId");

        const user = store.users.find((u) => u.id === targetUserId);
        if (!user) throw new Error("User not found");

        const chat = chats.findOrCreateChat(user);
        const message = chats.appendMessage(
          chat,
          isAdmin ? "admin" : "user",
          text
        );

        const event = { userId: user.id, userEmail: user.email, message };
        io.to(userRoom(user.id)).emit("support:message", event);
        io.to(ADMIN_ROOM).emit("support:message", event);

        if (ack) ack({ ok: true, message });
      } catch (err) {
        if (ack) ack({ ok: false, error: err.message });
      }
    });

    socket.on("support:adminOpen", (payload, ack) => {
      try {
        if (socket.data.role !== "admin") throw new Error("Admin only");
        const chat = store.supportChats.find(
          (c) => c.userId === payload.userId
        );
        if (!chat) throw new Error("Chat not found");
        chats.markRead(chat, "admin");
        if (ack)
          ack({
            ok: true,
            userId: chat.userId,
            userEmail: chat.userEmail,
            messages: chat.messages,
          });
      } catch (err) {
        if (ack) ack({ ok: false, error: err.message });
      }
    });

    socket.on("disconnect", () => {
      console.log(`Client disconnected: ${socket.id}`);
    });
  });

  return io;
}

module.exports = { setupSocket };
