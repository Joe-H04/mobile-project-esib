const fs = require("fs");
const path = require("path");

const DB_PATH = path.join(__dirname, "db.json");

const PERSISTED_KEYS = ["users", "bets", "watchlist", "supportChats"];

function load() {
  try {
    if (!fs.existsSync(DB_PATH)) return null;
    const raw = fs.readFileSync(DB_PATH, "utf-8");
    if (!raw.trim()) return null;
    return JSON.parse(raw);
  } catch (err) {
    console.error("Failed to load db.json:", err.message);
    return null;
  }
}

function save(store) {
  const snapshot = {};
  for (const key of PERSISTED_KEYS) {
    snapshot[key] = store[key] ?? [];
  }
  try {
    const tmp = `${DB_PATH}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(snapshot, null, 2));
    fs.renameSync(tmp, DB_PATH);
  } catch (err) {
    console.error("Failed to save db.json:", err.message);
  }
}

function hydrate(store) {
  const data = load();
  if (!data) return;
  for (const key of PERSISTED_KEYS) {
    if (Array.isArray(data[key])) store[key] = data[key];
  }
  console.log(
    `Loaded from db.json: ${store.users.length} users, ${store.bets.length} bets, ${store.watchlist.length} watchlist entries, ${store.supportChats.length} chats`
  );
}

function attachAutoSave(store) {
  let pending = false;
  const flush = () => {
    pending = false;
    save(store);
  };
  store.persist = () => {
    if (pending) return;
    pending = true;
    setImmediate(flush);
  };
}

module.exports = { hydrate, save, attachAutoSave };
