const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { v4: uuidv4 } = require("uuid");
const config = require("../config");
const store = require("../data/store");
const authMiddleware = require("../middleware/auth");

const router = express.Router();

router.post("/register", (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: "Email and password are required" });
  }
  if (password.length < 6) {
    return res
      .status(400)
      .json({ error: "Password must be at least 6 characters" });
  }

  const existing = store.users.find((u) => u.email === email);
  if (existing) {
    return res.status(409).json({ error: "Email already registered" });
  }

  const passwordHash = bcrypt.hashSync(password, 10);
  const user = {
    id: uuidv4(),
    email,
    passwordHash,
    balance: 1000.0,
    createdAt: new Date().toISOString(),
  };
  store.users.push(user);
  store.persist();

  const token = jwt.sign(
    { userId: user.id, email: user.email },
    config.JWT_SECRET,
    { expiresIn: "7d" }
  );

  res.status(201).json({
    token,
    user: { id: user.id, email: user.email, balance: user.balance },
  });
});

router.post("/login", (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: "Email and password are required" });
  }

  const user = store.users.find((u) => u.email === email);
  if (!user) {
    return res.status(401).json({ error: "Invalid credentials" });
  }

  if (!bcrypt.compareSync(password, user.passwordHash)) {
    return res.status(401).json({ error: "Invalid credentials" });
  }

  const token = jwt.sign(
    { userId: user.id, email: user.email },
    config.JWT_SECRET,
    { expiresIn: "7d" }
  );

  res.json({
    token,
    user: { id: user.id, email: user.email, balance: user.balance },
  });
});

router.get("/me", authMiddleware, (req, res) => {
  const user = store.users.find((u) => u.id === req.user.userId);
  if (!user) return res.status(404).json({ error: "User not found" });

  const userBets = store.bets.filter((b) => b.userId === user.id);
  const wagered = userBets.reduce((sum, b) => sum + (b.totalCost || 0), 0);
  const redeemedBets = userBets.filter((b) => b.redeemed);
  const redeemed = redeemedBets.reduce((sum, b) => sum + (b.payout || 0), 0);
  const openBets = userBets.filter((b) => !b.redeemed).length;
  const wins = redeemedBets.filter((b) => (b.payout || 0) > 0).length;

  res.json({
    user: {
      id: user.id,
      email: user.email,
      balance: round2(user.balance),
      createdAt: user.createdAt,
    },
    stats: {
      betsCount: userBets.length,
      openBets,
      wagered: round2(wagered),
      redeemed: round2(redeemed),
      wins,
      netProfit: round2(user.balance + wagered - 1000),
    },
  });
});

function round2(n) {
  return Math.round(n * 100) / 100;
}

module.exports = router;
