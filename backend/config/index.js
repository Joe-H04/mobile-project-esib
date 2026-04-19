module.exports = {
  JWT_SECRET: process.env.JWT_SECRET || "betnow-dev-secret-change-in-prod",
  PORT: process.env.PORT || 3000,
  POLYMARKET_GAMMA_URL: "https://gamma-api.polymarket.com",
  MARKET_REFRESH_INTERVAL_MS: 30000,
  SEED_MARKET_COUNT: 20,
};
