# BetNow - Remaining Work

## Completed

- **Backend** — fully built and tested (auth, markets, bets, Socket.io, Polymarket sync)
- **Android project setup** — Gradle, dependencies, manifest
- **Android network layer** — Retrofit, SocketManager, models, TokenManager, Resource
- **Android auth screens** — LoginActivity, RegisterActivity, AuthViewModel, XML layouts
- **Android market list** — MarketListFragment, Adapter, ViewModel, MainActivity w/ BottomNav
- **Market detail layout + ViewModel** — fragment_market_detail.xml, MarketDetailViewModel.kt
- **Market detail screen** — MarketDetailFragment, market binding, bet flow, success/error handling, toolbar balance updates
- **My Bets screen** — fragment, adapter, view model, layouts, empty/error states
- **UI consistency pass** — shared number/date formatting, retry states on market list/detail/bets

## TODO

### 1. Real-time Updates Verification

- Socket.io connect/disconnect already in MainActivity onStart/onStop
- Odds listeners already in MarketListViewModel and MarketDetailViewModel
- Verify prices update live on both screens without manual refresh

### 2. Verification

- Run an Android compile/build once Gradle is available (`gradlew` is missing from repo, and `gradle` is not installed locally)
- Exercise the bet flow end-to-end against the backend to confirm payloads and screen state

### 3. Remaining Polish

- App icon / launcher icon (currently using default)
- Optional: add toolbar/up navigation affordance on the detail screen in addition to system back
