const config = require("../config");
const store = require("../data/store");

const SEARCH_EVENT_LIMIT = 25;
const SPORTS_TAG_LIMIT = 8;
const SPORTS_EVENT_LIMIT = 15;
const TRACKED_MARKET_BATCH_SIZE = 50;
const SPORTS_KEYWORDS = [
  "sports",
  "nba",
  "wnba",
  "nfl",
  "mlb",
  "nhl",
  "soccer",
  "football",
  "baseball",
  "basketball",
  "hockey",
  "tennis",
  "golf",
  "mma",
  "ufc",
  "boxing",
  "cricket",
  "f1",
  "formula 1",
  "nascar",
  "epl",
  "champions league",
  "la liga",
  "serie a",
  "bundesliga",
  "ncaa",
  "cbb",
  "cfb",
  "esports",
];

async function seedMarkets() {
  try {
    const rawMarkets = await fetchGammaJson("/markets", {
      limit: config.SEED_MARKET_COUNT,
      active: true,
      closed: false,
    });

    const markets = rawMarkets
      .filter((m) => m.outcomePrices && m.outcomes)
      .map(normalizeMarket);

    store.markets = markets;
    await refreshTrackedBetMarkets();
    console.log(`Seeded ${markets.length} markets from Polymarket`);
  } catch (err) {
    console.error("Failed to seed markets from Polymarket:", err.message);
    console.log("Server will start with empty markets");
  }
}

async function refreshPrices(io) {
  try {
    const rawMarkets = await fetchGammaJson("/markets", {
      limit: config.SEED_MARKET_COUNT,
      active: true,
      closed: false,
    });

    applyMarketUpdates(rawMarkets.map(normalizeMarket), io);
    await refreshTrackedBetMarkets(io);
  } catch (err) {
    console.error("Failed to refresh prices:", err.message);
  }
}

async function searchMarkets(query) {
  const trimmedQuery = `${query || ""}`.trim();
  if (!trimmedQuery) return [];

  const result = await fetchGammaJson("/public-search", {
    q: trimmedQuery,
    limit_per_type: SEARCH_EVENT_LIMIT,
    events_status: "open",
    keep_closed_markets: 0,
    search_profiles: false,
    search_tags: true,
  });

  const searchEvents = Array.isArray(result?.events) ? result.events : [];
  const markets = flattenMarketsFromEvents(searchEvents, { activeOnly: true });

  upsertMarkets(markets);

  return markets;
}

async function fetchSportsMarkets() {
  const sports = await fetchGammaJson("/sports");
  const tagIds = dedupeById(
    (Array.isArray(sports) ? sports : [])
      .flatMap((sport) => `${sport?.tags || ""}`.split(","))
      .map((tagId) => `${tagId}`.trim())
      .filter(Boolean)
      .map((tagId) => ({ id: tagId })),
    "id"
  )
    .slice(0, SPORTS_TAG_LIMIT)
    .map((tag) => tag.id);

  if (!tagIds.length) return [];

  const sportEventSets = await Promise.all(
    tagIds.map(async (tagId) => {
      try {
        return await fetchGammaJson("/events", {
          tag_id: tagId,
          related_tags: true,
          active: true,
          closed: false,
          limit: SPORTS_EVENT_LIMIT,
        });
      } catch {
        return [];
      }
    })
  );

  const markets = flattenMarketsFromEvents(sportEventSets.flat()).map((market) => ({
    ...market,
    isSports: true,
  }));

  upsertMarkets(markets);

  return markets;
}

function normalizeMarket(m) {
  const outcomes = safeJsonParse(m.outcomes, ["Yes", "No"]);
  const outcomePrices = safeJsonParse(m.outcomePrices, [0.5, 0.5]).map(Number);
  const closed = m.closed === true;
  const active = m.active !== false;
  const resolved = isResolvedPrices(outcomePrices) || closed;
  const winningOutcome = isResolvedPrices(outcomePrices)
    ? winningSideFromPrices(outcomePrices)
    : null;

  return {
    id: m.conditionId || m.id,
    question: m.question || "",
    description: m.description || "",
    slug: m.slug || "",
    image: m.image || m.icon || "",
    category: extractCategory(m),
    isSports: isSportsMarket(m),
    outcomes,
    outcomePrices,
    volume: `${m.volume || "0"}`,
    liquidity: `${m.liquidity || "0"}`,
    endDate: m.endDate || null,
    active,
    closed,
    resolved,
    winningOutcome,
    lastUpdated: new Date().toISOString(),
  };
}

function extractCategory(m) {
  if (typeof m.category === "string" && m.category.trim()) return m.category.trim();
  if (Array.isArray(m.tags) && m.tags.length) {
    const first = m.tags[0];
    if (typeof first === "string") return first;
    if (first && typeof first.label === "string") return first.label;
  }
  if (Array.isArray(m.events) && m.events.length) {
    const ev = m.events[0];
    if (ev && typeof ev.category === "string" && ev.category.trim()) {
      return ev.category.trim();
    }
  }
  return "Other";
}

function isResolvedPrices(prices) {
  if (!Array.isArray(prices) || prices.length < 2) return false;
  const [yes, no] = prices;
  return (yes === 1 && no === 0) || (yes === 0 && no === 1);
}

function winningSideFromPrices(prices) {
  if (!Array.isArray(prices) || prices.length < 2) return null;
  if (prices[0] === 1) return "YES";
  if (prices[1] === 1) return "NO";
  return null;
}

function startPeriodicRefresh(io) {
  setInterval(() => refreshPrices(io), config.MARKET_REFRESH_INTERVAL_MS);
  console.log(
    `Price refresh scheduled every ${config.MARKET_REFRESH_INTERVAL_MS / 1000}s`
  );
}

function safeJsonParse(value, fallback) {
  if (Array.isArray(value)) return value;
  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function flattenMarketsFromEvents(events, options = {}) {
  const { activeOnly = false } = options;

  return dedupeById(
    (Array.isArray(events) ? events : [])
      .flatMap((event) =>
        (Array.isArray(event?.markets) ? event.markets : []).map((market) =>
          attachEventContext(market, event)
        )
      )
      .filter((market) => market && market.outcomes && market.outcomePrices)
      .filter((market) => !activeOnly || isActiveSearchMarket(market))
      .map(normalizeMarket),
    "id"
  );
}

function attachEventContext(market, event) {
  return {
    ...market,
    image: market?.image || event?.image || event?.icon || "",
    icon: market?.icon || event?.icon || event?.image || "",
    category: market?.category || event?.category || event?.subcategory || "",
    tags: Array.isArray(market?.tags) && market.tags.length ? market.tags : event?.tags,
    events:
      Array.isArray(market?.events) && market.events.length
        ? market.events
        : [
            {
              category: event?.category,
              subcategory: event?.subcategory,
              title: event?.title,
            },
          ],
  };
}

function isActiveSearchMarket(market) {
  return market?.active !== false && market?.closed !== true;
}

function upsertMarkets(markets) {
  if (!Array.isArray(markets) || !markets.length) return;

  const indexesById = new Map(store.markets.map((market, index) => [market.id, index]));

  for (const market of markets) {
    const existingIndex = indexesById.get(market.id);
    if (existingIndex == null) {
      store.markets.push(market);
      indexesById.set(market.id, store.markets.length - 1);
      continue;
    }

    store.markets[existingIndex] = {
      ...store.markets[existingIndex],
      ...market,
    };
  }
}

function applyMarketUpdates(markets, io) {
  for (const market of Array.isArray(markets) ? markets : []) {
    const existing = store.markets.find((entry) => entry.id === market.id);
    if (!existing) {
      store.markets.push(market);
      continue;
    }

    const pricesChanged =
      existing.outcomePrices[0] !== market.outcomePrices[0] ||
      existing.outcomePrices[1] !== market.outcomePrices[1];
    const resolvedChanged =
      existing.resolved !== market.resolved ||
      existing.winningOutcome !== market.winningOutcome ||
      existing.closed !== market.closed ||
      existing.active !== market.active;

    if (!pricesChanged && !resolvedChanged) continue;

    Object.assign(existing, market);

    if (io && pricesChanged) {
      io.emit("odds-update", {
        marketId: existing.id,
        outcomePrices: existing.outcomePrices,
        timestamp: existing.lastUpdated,
      });
    }
    if (io && resolvedChanged && market.resolved) {
      io.emit("market-resolved", {
        marketId: existing.id,
        winningOutcome: existing.winningOutcome,
        timestamp: existing.lastUpdated,
      });
    }
  }
}

async function refreshTrackedBetMarkets(io) {
  const marketIds = Array.from(
    new Set(
      store.bets
        .map((bet) => bet?.marketId)
        .filter(Boolean)
    )
  );

  if (!marketIds.length) return;

  const batches = chunkArray(marketIds, TRACKED_MARKET_BATCH_SIZE);
  const fetched = [];

  for (const ids of batches) {
    const [openMarkets, closedMarkets] = await Promise.all([
      fetchTrackedMarkets(ids, false),
      fetchTrackedMarkets(ids, true),
    ]);
    fetched.push(...openMarkets, ...closedMarkets);
  }

  applyMarketUpdates(
    dedupeById(
      fetched
        .filter((market) => market?.outcomePrices && market?.outcomes)
        .map(normalizeMarket),
      "id"
    ),
    io
  );
}

async function fetchTrackedMarkets(conditionIds, closed) {
  try {
    return await fetchGammaJson("/markets", {
      limit: conditionIds.length,
      condition_ids: conditionIds,
      closed,
    });
  } catch (err) {
    console.error(
      `Failed to refresh tracked ${closed ? "closed" : "open"} markets:`,
      err.message
    );
    return [];
  }
}

async function fetchGammaJson(path, query = {}) {
  const url = buildGammaUrl(path, query);
  console.log(`Fetching Polymarket data: ${url}`);

  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Polymarket API returned ${response.status} for ${path}`);
  }

  return response.json();
}

function buildGammaUrl(path, query = {}) {
  const url = new URL(`${config.POLYMARKET_GAMMA_URL}${path}`);

  for (const [key, value] of Object.entries(query)) {
    if (value == null || value === "") continue;

    if (Array.isArray(value)) {
      for (const item of value) {
        if (item != null && item !== "") {
          url.searchParams.append(key, item);
        }
      }
      continue;
    }

    url.searchParams.set(key, value);
  }

  return url.toString();
}

function chunkArray(items, size) {
  const chunks = [];

  for (let i = 0; i < items.length; i += size) {
    chunks.push(items.slice(i, i + size));
  }

  return chunks;
}

function dedupeById(items, key) {
  const seen = new Set();

  return items.filter((item) => {
    const value = item?.[key];
    if (value == null || seen.has(value)) return false;
    seen.add(value);
    return true;
  });
}

function isSportsMarket(m) {
  if (
    m?.sportsMarketType ||
    m?.gameStartTime ||
    m?.teamAID ||
    m?.teamBID ||
    m?.gameId
  ) {
    return true;
  }

  const textValues = [
    m?.category,
    m?.subcategory,
    m?.sportsMarketType,
    ...(Array.isArray(m?.tags)
      ? m.tags.map((tag) => (typeof tag === "string" ? tag : tag?.label))
      : []),
    ...(Array.isArray(m?.categories)
      ? m.categories.map((category) => category?.label)
      : []),
    ...(Array.isArray(m?.events)
      ? m.events.flatMap((event) => [event?.category, event?.subcategory, event?.title])
      : []),
  ]
    .filter(Boolean)
    .map((value) => `${value}`.toLowerCase());

  return textValues.some((value) =>
    SPORTS_KEYWORDS.some((keyword) => value.includes(keyword))
  );
}

module.exports = {
  seedMarkets,
  refreshPrices,
  startPeriodicRefresh,
  searchMarkets,
  fetchSportsMarkets,
};
