const persistence = require("./persistence");

const store = {
  users: [],
  // { id, email, passwordHash, balance, role, createdAt }

  markets: [],
  // { id, question, description, slug, image, category, outcomes, outcomePrices,
  //   volume, liquidity, endDate, active, closed, resolved, winningOutcome, lastUpdated }

  bets: [],
  // { id, userId, marketId, marketQuestion, side, shares, pricePerShare,
  //   totalCost, createdAt, redeemed, payout, redeemedAt }

  watchlist: [],
  // { userId, marketId, createdAt }

  supportChats: [],
  // { userId, userEmail, messages: [{ id, from, text, ts }], lastMessageAt, unreadForAdmin, unreadForUser }
};

persistence.hydrate(store);
persistence.attachAutoSave(store);

module.exports = store;
