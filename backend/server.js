const express = require("express");
const http = require("http");
const cors = require("cors");
const config = require("./config");
const store = require("./data/store");
const persistence = require("./data/persistence");
const authRoutes = require("./routes/auth");
const marketRoutes = require("./routes/markets");
const betRoutes = require("./routes/bets");
const watchlistRoutes = require("./routes/watchlist");
const leaderboardRoutes = require("./routes/leaderboard");
const { setupSocket } = require("./socket");
const {
  seedMarkets,
  startPeriodicRefresh,
} = require("./services/polymarket");

const app = express();

app.use(cors());
app.use(express.json());

// Routes
app.use("/api/auth", authRoutes);
app.use("/api/markets", marketRoutes);
app.use("/api/bets", betRoutes);
app.use("/api/watchlist", watchlistRoutes);
app.use("/api/leaderboard", leaderboardRoutes);

// Health check
app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    marketsCount: store.markets.length,
    usersCount: store.users.length,
    betsCount: store.bets.length,
    watchlistCount: store.watchlist.length,
  });
});

// Start server
async function start() {
  const server = http.createServer(app);
  const io = setupSocket(server);

  await seedMarkets();
  startPeriodicRefresh(io);

  server.listen(config.PORT, () => {
    console.log(`BetNow server running on port ${config.PORT}`);
  });

  const shutdown = () => {
    console.log("Shutting down, flushing db...");
    persistence.save(store);
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(0), 2000).unref();
  };
  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);
}

start();
