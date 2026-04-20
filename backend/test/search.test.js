const test = require("node:test");
const assert = require("node:assert/strict");

const store = require("../data/store");
const marketsRouter = require("../routes/markets");
const { searchMarkets } = require("../services/polymarket");

const originalFetch = global.fetch;

function resetStore() {
  store.persist = () => {};
  store.users = [];
  store.bets = [];
  store.watchlist = [];
  store.markets = [];
}

function baseMarket(overrides = {}) {
  return {
    id: overrides.id || "market-id",
    question: overrides.question || "Sample market",
    description: overrides.description || "",
    slug: overrides.slug || "sample-market",
    image: overrides.image || "",
    category: overrides.category || "Other",
    outcomes: overrides.outcomes || ["Yes", "No"],
    outcomePrices: overrides.outcomePrices || [0.5, 0.5],
    volume: overrides.volume || "0",
    liquidity: overrides.liquidity || "0",
    endDate: overrides.endDate || null,
    active: overrides.active ?? true,
    closed: overrides.closed ?? false,
    isSports: overrides.isSports ?? false,
    resolved: overrides.resolved ?? false,
    winningOutcome: overrides.winningOutcome || null,
    lastUpdated: overrides.lastUpdated || null,
  };
}

test.afterEach(() => {
  global.fetch = originalFetch;
  resetStore();
});

test("searchMarkets uses direct public-search results and keeps only active matches", async () => {
  resetStore();

  global.fetch = async (url) => {
    const parsed = new URL(url);

    assert.equal(parsed.pathname, "/public-search");
    assert.equal(parsed.searchParams.get("q"), "hormuz");
    assert.equal(parsed.searchParams.get("optimized"), null);
    assert.equal(parsed.searchParams.get("search_tags"), "true");
    assert.equal(parsed.searchParams.get("limit_per_type"), "25");

    return response({
      events: [
        {
          id: "event-1",
          title: "Strait of Hormuz traffic returns to normal by end of April?",
          category: "Geopolitics",
          subcategory: "Iran",
          image: "event-image",
          markets: [
            {
              id: "market-1",
              conditionId: "condition-1",
              question: "Strait of Hormuz traffic returns to normal by end of April?",
              description: "",
              outcomes: JSON.stringify(["Yes", "No"]),
              outcomePrices: JSON.stringify(["0.25", "0.75"]),
              volume: "17000000",
              liquidity: "383000",
              active: true,
              closed: false,
            },
            {
              id: "market-2",
              conditionId: "condition-2",
              question: "Closed Hormuz market",
              description: "",
              outcomes: JSON.stringify(["Yes", "No"]),
              outcomePrices: JSON.stringify(["1", "0"]),
              volume: "1",
              liquidity: "1",
              active: true,
              closed: true,
            },
          ],
        },
      ],
    });
  };

  const markets = await searchMarkets("hormuz");

  assert.equal(markets.length, 1);
  assert.equal(markets[0].id, "condition-1");
  assert.equal(markets[0].question, "Strait of Hormuz traffic returns to normal by end of April?");
  assert.equal(markets[0].category, "Geopolitics");
  assert.equal(markets[0].image, "event-image");
  assert.equal(store.markets.length, 1);
  assert.equal(store.markets[0].id, "condition-1");
});

test("markets route falls back to active local search matches only", async () => {
  resetStore();
  store.markets = [
    baseMarket({
      id: "active-hormuz",
      question: "Hormuz traffic returns to normal?",
      category: "Geopolitics",
      active: true,
      closed: false,
    }),
    baseMarket({
      id: "closed-hormuz",
      question: "Hormuz resolved market",
      category: "Geopolitics",
      active: false,
      closed: true,
      resolved: true,
      winningOutcome: "NO",
    }),
    baseMarket({
      id: "bitcoin",
      question: "MicroStrategy sells any Bitcoin in 2025?",
      category: "Crypto",
      active: true,
      closed: false,
    }),
  ];

  global.fetch = async () => {
    throw new Error("network down");
  };

  const result = await invokeRoute(marketsRouter, "/", "get", {
    query: { search: "hormuz" },
  });

  assert.equal(result.statusCode, 200);
  assert.deepEqual(
    result.body.markets.map((market) => market.id),
    ["active-hormuz"]
  );
});

test("categories route collapses sports subcategories into Sports only", async () => {
  resetStore();
  store.markets = [
    baseMarket({
      id: "football-market",
      category: "Football",
      isSports: true,
    }),
    baseMarket({
      id: "basketball-market",
      category: "Basketball",
      isSports: true,
    }),
    baseMarket({
      id: "crypto-market",
      category: "Crypto",
    }),
  ];

  const result = await invokeRoute(marketsRouter, "/categories", "get");

  assert.equal(result.statusCode, 200);
  assert.deepEqual(result.body.categories, [
    { name: "Sports", count: 2 },
    { name: "Crypto", count: 1 },
  ]);
});

function invokeRoute(router, path, method, req = {}) {
  const layer = router.stack.find(
    (entry) => entry.route && entry.route.path === path && entry.route.methods?.[method]
  );

  if (!layer) {
    throw new Error(`Route ${method.toUpperCase()} ${path} not found`);
  }

  return new Promise((resolve, reject) => {
    const response = {
      statusCode: 200,
      body: null,
      status(code) {
        this.statusCode = code;
        return this;
      },
      json(payload) {
        this.body = payload;
        resolve({ statusCode: this.statusCode, body: payload });
        return this;
      },
    };

    try {
      const maybePromise = layer.route.stack[0].handle(
        { query: {}, params: {}, headers: {}, ...req },
        response
      );
      if (maybePromise && typeof maybePromise.then === "function") {
        maybePromise.catch(reject);
      }
    } catch (error) {
      reject(error);
    }
  });
}

function response(payload) {
  return {
    ok: true,
    json: async () => payload,
  };
}
