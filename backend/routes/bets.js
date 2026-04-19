const express = require('express');
const { v4: uuidv4 } = require('uuid');
const authMiddleware = require('../middleware/auth');
const store = require('../data/store');

const router = express.Router();

router.post('/place', authMiddleware, (req, res) => {
  const { marketId, side, amount } = req.body;

  if (!marketId || !side || !amount) {
    return res
      .status(400)
      .json({ error: 'marketId, side, and amount are required' });
  }

  const normalizedSide = side.toUpperCase();
  if (normalizedSide !== 'YES' && normalizedSide !== 'NO') {
    return res.status(400).json({ error: 'Side must be YES or NO' });
  }

  const numAmount = parseFloat(amount);
  if (isNaN(numAmount) || numAmount <= 0) {
    return res.status(400).json({ error: 'Amount must be a positive number' });
  }

  const market = store.markets.find((m) => m.id === marketId);
  if (!market) {
    return res.status(404).json({ error: 'Market not found' });
  }
  if (market.closed || !market.active || market.resolved) {
    return res.status(400).json({ error: 'Market is not active' });
  }

  const user = store.users.find((u) => u.id === req.user.userId);
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }
  if (user.balance < numAmount) {
    return res.status(400).json({ error: 'Insufficient balance' });
  }

  const pricePerShare =
    normalizedSide === 'YES'
      ? market.outcomePrices[0]
      : market.outcomePrices[1];

  const shares = numAmount / pricePerShare;

  user.balance = round2(user.balance - numAmount);

  const bet = {
    id: uuidv4(),
    userId: user.id,
    marketId,
    marketQuestion: market.question,
    side: normalizedSide,
    shares: round2(shares),
    pricePerShare: parseFloat(pricePerShare.toFixed(4)),
    totalCost: round2(numAmount),
    createdAt: new Date().toISOString(),
    redeemed: false,
    payout: 0,
    redeemedAt: null,
  };

  store.bets.push(bet);
  store.persist();

  res.status(201).json({ bet, newBalance: user.balance });
});

router.get('/my', authMiddleware, (req, res) => {
  const userBets = store.bets
    .filter((b) => b.userId === req.user.userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  const enriched = userBets.map((bet) => {
    const market = store.markets.find((m) => m.id === bet.marketId);
    return {
      ...bet,
      marketResolved: market?.resolved === true,
      marketWinningOutcome: market?.winningOutcome || null,
    };
  });

  res.json({ bets: enriched });
});

router.post('/:betId/redeem', authMiddleware, (req, res) => {
  const { betId } = req.params;
  const bet = store.bets.find(
    (b) => b.id === betId && b.userId === req.user.userId
  );
  if (!bet) return res.status(404).json({ error: 'Bet not found' });
  if (bet.redeemed) {
    return res.status(400).json({ error: 'Bet already redeemed' });
  }

  const market = store.markets.find((m) => m.id === bet.marketId);
  if (!market) return res.status(404).json({ error: 'Market not found' });
  if (!market.resolved || !market.winningOutcome) {
    return res.status(400).json({ error: 'Market not resolved yet' });
  }

  const user = store.users.find((u) => u.id === req.user.userId);
  if (!user) return res.status(404).json({ error: 'User not found' });

  const isWinner = bet.side === market.winningOutcome;
  const payout = isWinner ? round2(bet.shares) : 0;

  bet.redeemed = true;
  bet.payout = payout;
  bet.redeemedAt = new Date().toISOString();

  if (payout > 0) {
    user.balance = round2(user.balance + payout);
  }
  store.persist();

  res.json({
    bet,
    payout,
    won: isWinner,
    newBalance: user.balance,
  });
});

function round2(n) {
  return Math.round(n * 100) / 100;
}

module.exports = router;
