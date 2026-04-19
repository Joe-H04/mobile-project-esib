const express = require("express");
const store = require("../data/store");
const { searchMarkets, fetchSportsMarkets } = require("../services/polymarket");

const router = express.Router();

router.get("/", async (req, res) => {
  let markets = store.markets.slice();
  const search = `${req.query.search || ""}`.trim();
  const selectedCategory = `${req.query.category || ""}`.trim();

  if (search) {
    try {
      const remotelyFetchedMarkets = await searchMarkets(search);
      const localMatches = store.markets.filter((market) =>
        isVisibleSearchMarket(market) && matchesSearch(market, search)
      );
      markets = dedupeMarkets([...remotelyFetchedMarkets, ...localMatches]);
    } catch (err) {
      console.error(`Failed to search Polymarket for "${search}":`, err.message);
      markets = store.markets.filter(
        (market) => isVisibleSearchMarket(market) && matchesSearch(market, search)
      );
    }
  }

  if (isSportsCategory(selectedCategory) && !search) {
    try {
      const sportsMarkets = await fetchSportsMarkets();
      markets = dedupeMarkets([...sportsMarkets, ...markets]);
    } catch (err) {
      console.error("Failed to fetch sports markets:", err.message);
    }
  }

  if (selectedCategory && selectedCategory !== "All") {
    if (isSportsCategory(selectedCategory)) {
      markets = markets.filter((market) => market.isSports === true);
    } else {
      const cat = selectedCategory.toLowerCase();
      markets = markets.filter(
        (m) => (m.category || "").toLowerCase() === cat
      );
    }
  }

  if (req.query.active === "true") {
    markets = markets.filter((m) => m.active && !m.closed);
  }

  switch (req.query.sort) {
    case "volume":
      markets.sort(
        (a, b) => parseFloat(b.volume || 0) - parseFloat(a.volume || 0)
      );
      break;
    case "liquidity":
      markets.sort(
        (a, b) => parseFloat(b.liquidity || 0) - parseFloat(a.liquidity || 0)
      );
      break;
    case "ending":
      markets.sort((a, b) => {
        const ad = a.endDate ? new Date(a.endDate).getTime() : Infinity;
        const bd = b.endDate ? new Date(b.endDate).getTime() : Infinity;
        return ad - bd;
      });
      break;
  }

  res.json({ markets });
});

router.get("/categories", (req, res) => {
  const counts = new Map();
  let sportsCount = 0;
  for (const m of store.markets) {
    if (m.isSports) sportsCount += 1;
    const key = m.category || "Other";
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  const categories = Array.from(counts.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count);

  if (sportsCount > 0 && !categories.some((category) => isSportsCategory(category.name))) {
    categories.unshift({ name: "Sports", count: sportsCount });
  }

  res.json({ categories });
});

router.get("/:id", (req, res) => {
  const market = store.markets.find((m) => m.id === req.params.id);
  if (!market) {
    return res.status(404).json({ error: "Market not found" });
  }
  res.json({ market });
});

module.exports = router;

function matchesSearch(market, search) {
  const normalizedSearch = search.toLowerCase();
  const question = `${market.question || ""}`.toLowerCase();
  const description = `${market.description || ""}`.toLowerCase();

  return (
    question.includes(normalizedSearch) || description.includes(normalizedSearch)
  );
}

function dedupeMarkets(markets) {
  const seen = new Set();

  return markets.filter((market) => {
    if (!market?.id || seen.has(market.id)) return false;
    seen.add(market.id);
    return true;
  });
}

function isSportsCategory(category) {
  return `${category || ""}`.trim().toLowerCase() === "sports";
}

function isVisibleSearchMarket(market) {
  return market?.active !== false && market?.closed !== true;
}
