const test = require("node:test");
const assert = require("node:assert/strict");

const store = require("../data/store");
const { refreshPrices, seedMarkets } = require("../services/polymarket");

const originalFetch = global.fetch;

function makeMarket({
  conditionId,
  question = "Test market",
  outcomePrices,
  active,
  closed,
}) {
  return {
    id: conditionId,
    conditionId,
    question,
    description: "",
    slug: question.toLowerCase().replace(/\s+/g, "-"),
    image: "",
    category: "Test",
    outcomes: JSON.stringify(["Yes", "No"]),
    outcomePrices: JSON.stringify(outcomePrices),
    volume: "0",
    liquidity: "0",
    active,
    closed,
  };
}

function resetStore() {
  store.persist = () => {};
  store.users = [];
  store.bets = [];
  store.watchlist = [];
  store.markets = [];
}

test.afterEach(() => {
  global.fetch = originalFetch;
  resetStore();
});

test("refreshPrices updates tracked bets when the market is only returned as closed", async () => {
  resetStore();
  store.markets = [
    {
      id: "tracked-market",
      question: "Tracked market",
      description: "",
      slug: "tracked-market",
      image: "",
      category: "Test",
      outcomes: ["Yes", "No"],
      outcomePrices: [0.41, 0.59],
      volume: "0",
      liquidity: "0",
      endDate: null,
      active: true,
      closed: false,
      resolved: false,
      winningOutcome: null,
      lastUpdated: "2026-04-18T00:00:00.000Z",
    },
  ];
  store.bets = [{ marketId: "tracked-market" }];

  global.fetch = async (url) => {
    const parsed = new URL(url);
    const closed = parsed.searchParams.get("closed");
    const conditionIds = parsed.searchParams.getAll("condition_ids");

    if (!conditionIds.length && closed === "false") {
      return response([]);
    }
    if (conditionIds.includes("tracked-market") && closed === "false") {
      return response([]);
    }
    if (conditionIds.includes("tracked-market") && closed === "true") {
      return response([
        makeMarket({
          conditionId: "tracked-market",
          question: "Tracked market",
          outcomePrices: [1, 0],
          active: false,
          closed: true,
        }),
      ]);
    }

    throw new Error(`Unexpected URL ${url}`);
  };

  await refreshPrices();

  assert.equal(store.markets.length, 1);
  assert.equal(store.markets[0].resolved, true);
  assert.equal(store.markets[0].winningOutcome, "YES");
  assert.equal(store.markets[0].closed, true);
  assert.equal(store.markets[0].active, false);
});

test("seedMarkets also loads tracked closed markets on startup", async () => {
  resetStore();
  store.bets = [{ marketId: "closed-on-startup" }];

  global.fetch = async (url) => {
    const parsed = new URL(url);
    const closed = parsed.searchParams.get("closed");
    const conditionIds = parsed.searchParams.getAll("condition_ids");

    if (!conditionIds.length && closed === "false") {
      return response([
        makeMarket({
          conditionId: "active-market",
          question: "Active market",
          outcomePrices: [0.6, 0.4],
          active: true,
          closed: false,
        }),
      ]);
    }
    if (conditionIds.includes("closed-on-startup") && closed === "false") {
      return response([]);
    }
    if (conditionIds.includes("closed-on-startup") && closed === "true") {
      return response([
        makeMarket({
          conditionId: "closed-on-startup",
          question: "Closed market",
          outcomePrices: [0, 1],
          active: false,
          closed: true,
        }),
      ]);
    }

    throw new Error(`Unexpected URL ${url}`);
  };

  await seedMarkets();

  assert.equal(store.markets.length, 2);
  const closedMarket = store.markets.find((market) => market.id === "closed-on-startup");
  assert.ok(closedMarket);
  assert.equal(closedMarket.resolved, true);
  assert.equal(closedMarket.winningOutcome, "NO");
});

function response(payload) {
  return {
    ok: true,
    json: async () => payload,
  };
}
