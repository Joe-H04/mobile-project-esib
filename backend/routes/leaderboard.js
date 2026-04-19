const express = require("express");
const store = require("../data/store");

const router = express.Router();

router.get("/", (req, res) => {
  const entries = store.users.map((user) => {
    const userBets = store.bets.filter((b) => b.userId === user.id);
    const wagered = userBets.reduce((sum, b) => sum + (b.totalCost || 0), 0);
    const redeemed = userBets
      .filter((b) => b.redeemed)
      .reduce((sum, b) => sum + (b.payout || 0), 0);
    const netProfit = user.balance + wagered - 1000;

    return {
      userId: user.id,
      email: maskEmail(user.email),
      balance: round2(user.balance),
      betsCount: userBets.length,
      wagered: round2(wagered),
      redeemed: round2(redeemed),
      netProfit: round2(netProfit),
    };
  });

  entries.sort((a, b) => b.netProfit - a.netProfit);
  const top = entries.slice(0, 50).map((e, i) => ({ rank: i + 1, ...e }));

  res.json({ leaderboard: top });
});

function maskEmail(email) {
  if (!email || !email.includes("@")) return email || "";
  const [name, domain] = email.split("@");
  if (name.length <= 2) return `${name[0]}*@${domain}`;
  return `${name.slice(0, 2)}${"*".repeat(name.length - 2)}@${domain}`;
}

function round2(n) {
  return Math.round(n * 100) / 100;
}

module.exports = router;
