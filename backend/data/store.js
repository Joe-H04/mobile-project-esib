const persistence = require("./persistence");

const store = {
  users: [],
  // { id, email, passwordHash, balance, createdAt }

  markets: [],
  // { id, question, description, slug, image, category, outcomes, outcomePrices,
  //   volume, liquidity, endDate, active, closed, resolved, winningOutcome, lastUpdated }

  bets: [],
  // { id, userId, marketId, marketQuestion, side, shares, pricePerShare,
  //   totalCost, createdAt, redeemed, payout, redeemedAt }

  watchlist: [],
  // { userId, marketId, createdAt }
};

persistence.hydrate(store);
persistence.attachAutoSave(store);

module.exports = store;
