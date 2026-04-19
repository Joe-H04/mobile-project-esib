const express = require("express");
const authMiddleware = require("../middleware/auth");
const store = require("../data/store");

const router = express.Router();

router.get("/", authMiddleware, (req, res) => {
  const userId = req.user.userId;
  const entries = store.watchlist.filter((w) => w.userId === userId);
  const markets = entries
    .map((w) => store.markets.find((m) => m.id === w.marketId))
    .filter(Boolean);
  res.json({ markets });
});

router.post("/:marketId", authMiddleware, (req, res) => {
  const { marketId } = req.params;
  const userId = req.user.userId;

  const market = store.markets.find((m) => m.id === marketId);
  if (!market) return res.status(404).json({ error: "Market not found" });

  const exists = store.watchlist.find(
    (w) => w.userId === userId && w.marketId === marketId
  );
  if (!exists) {
    store.watchlist.push({
      userId,
      marketId,
      createdAt: new Date().toISOString(),
    });
    store.persist();
  }
  res.status(201).json({ watching: true });
});

router.delete("/:marketId", authMiddleware, (req, res) => {
  const { marketId } = req.params;
  const userId = req.user.userId;
  const before = store.watchlist.length;
  store.watchlist = store.watchlist.filter(
    (w) => !(w.userId === userId && w.marketId === marketId)
  );
  if (store.watchlist.length !== before) store.persist();
  res.json({ watching: false });
});

module.exports = router;
