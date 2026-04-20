from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    ListFlowable,
    ListItem,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


OUTPUT = "presentation/BetNow_Project_Presentation_Study_Guide.pdf"


def make_styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "Title",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=24,
            leading=30,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#111827"),
            spaceAfter=12,
        ),
        "subtitle": ParagraphStyle(
            "Subtitle",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=10,
            leading=14,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#4B5563"),
            spaceAfter=18,
        ),
        "h1": ParagraphStyle(
            "H1",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=16,
            leading=20,
            textColor=colors.HexColor("#111827"),
            spaceBefore=10,
            spaceAfter=7,
        ),
        "h2": ParagraphStyle(
            "H2",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=12,
            leading=15,
            textColor=colors.HexColor("#1F2937"),
            spaceBefore=8,
            spaceAfter=5,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9,
            leading=12.4,
            textColor=colors.HexColor("#111827"),
            spaceAfter=5,
        ),
        "small": ParagraphStyle(
            "Small",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8,
            leading=10.5,
            textColor=colors.HexColor("#374151"),
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.8,
            leading=11.5,
            leftIndent=10,
            bulletIndent=0,
            spaceAfter=3,
        ),
        "callout": ParagraphStyle(
            "Callout",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=9,
            leading=12.5,
            textColor=colors.HexColor("#111827"),
            borderColor=colors.HexColor("#CBD5E1"),
            borderWidth=0.75,
            borderPadding=7,
            backColor=colors.HexColor("#F8FAFC"),
            spaceBefore=5,
            spaceAfter=8,
        ),
        "code": ParagraphStyle(
            "Code",
            parent=base["Code"],
            fontName="Courier",
            fontSize=7.5,
            leading=9.5,
            textColor=colors.HexColor("#111827"),
            backColor=colors.HexColor("#F3F4F6"),
            borderPadding=5,
            spaceAfter=5,
        ),
        "table": ParagraphStyle(
            "TableText",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=7.6,
            leading=9.8,
            textColor=colors.HexColor("#111827"),
        ),
        "table_head": ParagraphStyle(
            "TableHead",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=7.8,
            leading=10,
            textColor=colors.white,
            alignment=TA_LEFT,
        ),
    }


def on_page(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#6B7280"))
    canvas.drawString(1.7 * cm, 1.05 * cm, "BetNow Project Presentation Study Guide")
    canvas.drawRightString(19.3 * cm, 1.05 * cm, f"Page {doc.page}")
    canvas.restoreState()


def p(text, style):
    return Paragraph(text, style)


def bullets(items, styles):
    return ListFlowable(
        [ListItem(p(item, styles["bullet"])) for item in items],
        bulletType="bullet",
        start="circle",
        leftIndent=12,
        bulletFontSize=6,
        spaceAfter=5,
    )


def numbered(items, styles):
    return ListFlowable(
        [ListItem(p(item, styles["bullet"])) for item in items],
        bulletType="1",
        leftIndent=15,
        bulletFontSize=8,
        spaceAfter=5,
    )


def table(data, styles, col_widths):
    converted = []
    for r, row in enumerate(data):
        converted.append([
            p(str(cell), styles["table_head"] if r == 0 else styles["table"])
            for cell in row
        ])
    t = Table(converted, colWidths=col_widths, repeatRows=1)
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#111827")),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F8FAFC")]),
            ]
        )
    )
    return t


def build():
    styles = make_styles()
    doc = BaseDocTemplate(
        OUTPUT,
        pagesize=A4,
        leftMargin=1.7 * cm,
        rightMargin=1.7 * cm,
        topMargin=1.55 * cm,
        bottomMargin=1.65 * cm,
    )
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="normal")
    doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=on_page)])

    story = []

    story.append(Spacer(1, 2.0 * cm))
    story.append(p("BetNow Project Presentation Study Guide", styles["title"]))
    story.append(
        p(
            "Prepared for a 2 minute pitch, 5 minute demo, and 3 minute question session. "
            "Based on the BetNow codebase and Android/Kotlin course slides 01-12.",
            styles["subtitle"],
        )
    )
    story.append(
        p(
            "Memorize the short pitch, understand the architecture flow, and practice the demo path exactly. "
            "The examiner will likely test whether you understand why each Android component was used, not only whether the app runs.",
            styles["callout"],
        )
    )
    story.append(p("One-Sentence Project Definition", styles["h1"]))
    story.append(
        p(
            "BetNow is a native Android prediction-market app where users can create an account, browse live markets, "
            "search/filter/sort them, open market details, place YES or NO bets, save markets to a watchlist, review their bets, "
            "redeem resolved bets, and monitor balance, profile statistics, and leaderboard ranking.",
            styles["body"],
        )
    )
    story.append(p("Core Tech Stack", styles["h1"]))
    story.append(
        table(
            [
                ["Area", "What We Used", "What To Say"],
                ["Language", "Kotlin", "Modern Android language with null safety, data classes, coroutines, lambdas, and concise syntax."],
                ["UI", "XML layouts + Material Components + ViewBinding", "Stable Android UI approach; ViewBinding gives type-safe view references."],
                ["Navigation", "Login/Register/Main activities + feature fragments", "Activities own major flows; fragments own tabs/screens inside the authenticated app shell."],
                ["Architecture", "ViewModel + LiveData + Repository", "Fragments render UI; ViewModels manage state; repositories handle data access."],
                ["Networking", "Retrofit + OkHttp + Gson", "Retrofit turns HTTP endpoints into Kotlin functions; OkHttp adds JWT headers."],
                ["Real-time", "Socket.io", "Receives odds-update and market-resolved events without polling every screen."],
                ["Lists", "RecyclerView + ListAdapter + DiffUtil", "Efficient scrolling and efficient row updates when markets change."],
                ["Images", "Glide", "Loads and caches market images asynchronously off the UI thread."],
                ["Persistence", "SharedPreferences", "Stores session token, user email, and balance. Room was not necessary for this lightweight local state."],
                ["Backend", "Node.js + Express + Socket.io + JSON persistence", "Handles auth, markets, bets, watchlist, leaderboard, support, and real-time events."],
            ],
            styles,
            [3.0 * cm, 4.0 * cm, 9.0 * cm],
        )
    )

    story.append(PageBreak())
    story.append(p("2 Minute Pitch Script", styles["h1"]))
    story.append(
        p(
            "BetNow is a native Android app for prediction markets. Users can register or log in, browse real markets, "
            "search and filter them, open a market detail page, see YES and NO prices, place a bet, save markets to a watchlist, "
            "review their bets, redeem resolved bets, and view profile statistics and leaderboard rankings.",
            styles["body"],
        )
    )
    story.append(
        p(
            "On the frontend, we used Kotlin, XML layouts, Material components, Fragments, ViewModels, LiveData, Retrofit, "
            "RecyclerView, Glide, and Socket.io. The main design idea was separation of concerns: fragments handle UI, "
            "ViewModels manage screen state, repositories handle data access, and Retrofit or Socket.io communicate with the backend.",
            styles["body"],
        )
    )
    story.append(
        p(
            "The backend is an Express server. It handles authentication with JWT, market search and sorting, betting, redemption, "
            "watchlist, leaderboard, support chat, and persistence. It also pulls market data from Polymarket and pushes live price "
            "or resolution updates to the app through sockets.",
            styles["body"],
        )
    )
    story.append(
        p(
            "The mobile-development value of the project is that it demonstrates real Android app structure: lifecycle-aware UI, "
            "reusable lists, asynchronous networking, local session persistence, error/loading states, and a complete user flow from login "
            "to placing and tracking bets.",
            styles["body"],
        )
    )
    story.append(p("Pitch Timing", styles["h2"]))
    story.append(
        table(
            [
                ["Time", "Content"],
                ["0:00-0:25", "Problem/product: prediction market mobile app."],
                ["0:25-0:55", "Core user flow: login, browse, detail, bet, watchlist, bets/profile."],
                ["0:55-1:30", "Architecture: Fragment -> ViewModel -> Repository -> Retrofit/Socket -> Backend."],
                ["1:30-1:50", "Backend: Express, JWT, Polymarket, real-time sockets, persistence."],
                ["1:50-2:00", "Close: lifecycle-aware, responsive, complete Android course project."],
            ],
            styles,
            [3.0 * cm, 13.0 * cm],
        )
    )
    story.append(p("What To Emphasize", styles["h2"]))
    story.append(
        bullets(
            [
                "Do not only list technologies. Say what problem each one solved.",
                "Say 'separation of concerns' when explaining ViewModels, repositories, and UI code.",
                "Say 'lifecycle-aware' when explaining LiveData, fragments, sockets, and binding cleanup.",
                "Say 'efficient lists' when explaining RecyclerView, ListAdapter, and DiffUtil.",
                "Say 'lightweight persistence' when explaining SharedPreferences instead of Room.",
            ],
            styles,
        )
    )

    story.append(PageBreak())
    story.append(p("5 Minute Demo Script", styles["h1"]))
    story.append(
        p(
            "Practice this exact path. The goal is to show a complete product flow without clicking risky or unrelated areas.",
            styles["callout"],
        )
    )
    story.append(
        table(
            [
                ["Time", "Action", "What To Say"],
                ["Before demo", "Start backend with npm start from backend/", "The emulator uses 10.0.2.2:3000 to reach the local server."],
                ["0:00-0:45", "Login or register", "Auth returns a JWT. TokenManager stores token/user/balance in SharedPreferences."],
                ["0:45-1:45", "Markets screen: scroll, search, sort, category", "This uses RecyclerView/ListAdapter/DiffUtil, ViewModel state, Retrofit, and Glide."],
                ["1:45-3:00", "Open a market, choose YES/NO, enter amount, show share preview, place bet", "The UI validates input, calculates estimated shares, sends POST /api/bets/place, then updates balance."],
                ["3:00-3:35", "Tap star and open Watchlist", "Watchlist is user-specific shared state visible from list and detail screens."],
                ["3:35-4:20", "Open My Bets, Profile, Leaderboard", "These are fragments inside MainActivity with separate ViewModels and repositories."],
                ["4:20-5:00", "Mention real-time and geoblock", "Socket.io updates prices/resolution. Georestriction uses runtime location permission and country classification."],
            ],
            styles,
            [2.2 * cm, 5.0 * cm, 8.8 * cm],
        )
    )
    story.append(p("Demo Safety Checklist", styles["h2"]))
    story.append(
        bullets(
            [
                "Use an allowed emulator location. The current geoblock list blocks LB and US, so Lebanon or United States locations may block login.",
                "Make sure the backend is running before launching the app.",
                "Use a fresh account or an account with enough balance.",
                "Do not waste time trying to force a resolved bet unless you already prepared data for it.",
                "If live prices do not visibly change during the demo, explain the socket behavior from code instead of waiting.",
                "Open Android Studio and run a clean build before presentation. This checkout does not include gradlew, and system gradle was not available here.",
            ],
            styles,
        )
    )

    story.append(PageBreak())
    story.append(p("Architecture You Must Be Able To Explain", styles["h1"]))
    story.append(
        p(
            "High-level flow: Activity/Fragment -> ViewModel -> Repository -> ApiService/SocketManager -> Backend -> Data source.",
            styles["callout"],
        )
    )
    story.append(p("Frontend Layers", styles["h2"]))
    story.append(
        table(
            [
                ["Layer", "Project Examples", "Responsibility"],
                ["Activity", "LoginActivity, RegisterActivity, MainActivity, GeoBlockActivity", "Owns major screen flows and app shell. MainActivity owns toolbar, bottom navigation, and fragment container."],
                ["Fragment", "MarketListFragment, MarketDetailFragment, MyBetsFragment, WatchlistFragment, ProfileFragment, LeaderboardFragment", "Inflates XML, observes ViewModel state, renders loading/success/error UI, handles user clicks."],
                ["ViewModel", "MarketListViewModel, MarketDetailViewModel, AuthViewModel, MyBetsViewModel", "Holds screen state and launches coroutines in viewModelScope."],
                ["Repository", "MarketRepository, BetRepository, AuthRepository, WatchlistRepository", "Wraps API calls and returns Resource.Loading/Success/Error style results."],
                ["Network", "ApiService, RetrofitClient, SocketManager", "Defines HTTP endpoints, attaches auth headers, receives socket events."],
                ["Local persistence", "TokenManager", "Stores JWT, email, and balance in SharedPreferences."],
            ],
            styles,
            [3.0 * cm, 5.0 * cm, 8.0 * cm],
        )
    )
    story.append(p("Backend Flow", styles["h2"]))
    story.append(
        bullets(
            [
                "Express routes expose /api/auth, /api/markets, /api/bets, /api/watchlist, /api/leaderboard, and /api/support.",
                "JWT middleware protects user-specific routes such as bets, watchlist, profile, and support history.",
                "Polymarket service seeds and refreshes market data, then Socket.io emits odds-update and market-resolved events.",
                "JSON persistence stores users, bets, watchlist, and support chats in backend/data/db.json.",
            ],
            styles,
        )
    )
    story.append(p("Code File Map", styles["h2"]))
    story.append(
        table(
            [
                ["Question", "Best File To Mention"],
                ["Where is bottom navigation handled?", "android/app/src/main/java/com/betnow/app/ui/main/MainActivity.kt"],
                ["Where is search debounce?", "android/app/src/main/java/com/betnow/app/ui/markets/MarketListViewModel.kt"],
                ["Where is market list rendering?", "android/app/src/main/java/com/betnow/app/ui/markets/MarketListAdapter.kt"],
                ["Where is bet validation and share preview?", "android/app/src/main/java/com/betnow/app/ui/detail/MarketDetailFragment.kt"],
                ["Where are Retrofit endpoints?", "android/app/src/main/java/com/betnow/app/network/ApiService.kt"],
                ["Where is JWT attached?", "android/app/src/main/java/com/betnow/app/network/RetrofitClient.kt"],
                ["Where is token persistence?", "android/app/src/main/java/com/betnow/app/util/TokenManager.kt"],
                ["Where are socket events handled?", "android/app/src/main/java/com/betnow/app/network/SocketManager.kt"],
                ["Where are backend bets handled?", "backend/routes/bets.js"],
                ["Where are backend market search/sort/filter handled?", "backend/routes/markets.js"],
            ],
            styles,
            [5.2 * cm, 10.8 * cm],
        )
    )

    story.append(PageBreak())
    story.append(p("Course PDF Mapping", styles["h1"]))
    story.append(
        table(
            [
                ["PDF", "Must Know", "How It Appears In BetNow"],
                ["01 Kotlin Basics", "val/var, types, null safety, strings, conditionals, collections", "Models use Kotlin data classes; UI logic uses immutable values where possible and null-safe checks like getOrNull."],
                ["02 Functions in Kotlin", "fun, default args, lambdas, higher-order functions, Unit", "Click callbacks, adapter lambdas, coroutine blocks, and helper functions like selectedSide()."],
                ["03 Classes and Objects", "classes, objects, constructors, data classes, properties", "RetrofitClient, TokenManager, SocketManager are objects; network models are data classes."],
                ["04 First Android App", "manifest, resources, Gradle, activities, XML, R class", "AndroidManifest declares permissions and activities; res/layout contains all screens; Gradle declares dependencies."],
                ["05 UI Layouts", "dp, margins/padding, ConstraintLayout, nested layout cost", "XML layouts structure screens with Material views, RecyclerViews, inputs, toolbar, and bottom navigation."],
                ["06 App Navigation", "intents, menus, app bar, bottom nav, fragments", "Login/Register use intents; MainActivity swaps fragments; toolbar menu handles logout."],
                ["07 Lifecycles", "onCreate/onStart/onResume/onPause/onStop/onDestroyView", "Sockets connect in lifecycle methods; fragments observe LiveData with viewLifecycleOwner; binding is cleared."],
                ["08 UI Architecture", "separation of concerns, ViewModel, LiveData, MVVM idea", "Fragments stay mostly UI-focused; ViewModels own state and call repositories."],
                ["09 Persistence", "preferences, SQLite, Room, DAO/entity/database", "BetNow uses SharedPreferences only. Room would be future work for offline cache."],
                ["10 Retrofit and Glide", "permissions, Retrofit service interfaces, GET/POST, images", "ApiService defines routes; RetrofitClient builds client; Glide loads market images."],
                ["11 Repository Pattern and WorkManager", "repository abstracts data sources; WorkManager for background work", "Repositories wrap Retrofit/socket access. WorkManager is not implemented; could be used later for background refresh."],
                ["12 Advanced RecyclerView", "RecyclerView recycling, ViewHolder, Adapter, ListAdapter, DiffUtil", "MarketListAdapter, MyBetsAdapter, LeaderboardAdapter, and SupportAdapter use efficient list patterns."],
            ],
            styles,
            [3.0 * cm, 5.0 * cm, 8.0 * cm],
        )
    )
    story.append(p("One Strong Sentence Per Course Area", styles["h2"]))
    story.append(
        bullets(
            [
                "Kotlin: We used data classes for API models, objects for singleton managers, and coroutines for async work.",
                "Layouts: XML layouts with Material components gave a stable UI structure for forms, lists, and detail screens.",
                "Navigation: MainActivity keeps one authenticated shell while fragments represent each major tab.",
                "Lifecycle: We attach observers to viewLifecycleOwner and connect/disconnect sockets with visible lifecycle states.",
                "Architecture: ViewModels prevent business logic from being buried inside fragments.",
                "Networking: Retrofit and OkHttp centralize HTTP requests and authentication headers.",
                "Persistence: SharedPreferences stores small session values; Room would be overkill unless we add offline structured caching.",
                "RecyclerView: ListAdapter and DiffUtil make large or changing market lists efficient.",
            ],
            styles,
        )
    )

    story.append(PageBreak())
    story.append(p("Feature Notes To Study", styles["h1"]))
    feature_rows = [
        ["Feature", "User Flow", "Implementation Point", "Likely Question"],
        ["Authentication", "Register/login -> token saved -> enter MainActivity", "AuthViewModel calls AuthRepository; TokenManager stores JWT/user/balance.", "How do you keep the user logged in?"],
        ["Markets", "Browse -> search -> category -> sort", "MarketListViewModel stores search/sort/category and calls MarketRepository.", "How do you prevent too many search requests?"],
        ["Market detail", "Open market -> view prices/image/description", "Fragment argument passes marketId; ViewModel loads one market.", "Why use fragment arguments?"],
        ["Betting", "Choose side -> enter amount -> preview shares -> place", "Frontend validates amount and side; backend validates again and deducts balance.", "Why validate on both frontend and backend?"],
        ["Watchlist", "Star market -> view saved markets", "WatchlistRepository adds/removes market ids; UI updates star state.", "How is shared user state reflected in multiple screens?"],
        ["My Bets", "View bets -> redeem if resolved", "GET /api/bets/my enriches bets with market resolution; redeem updates balance.", "How do you handle resolved markets?"],
        ["Profile", "Show email, balance, stats", "ProfileViewModel calls /api/auth/me and updates toolbar balance.", "How do you keep balance consistent?"],
        ["Leaderboard", "Show ranked users", "LeaderboardRepository loads entries into RecyclerView.", "Why is this a separate ViewModel?"],
        ["Live updates", "Prices/resolution refresh without manual reload", "SocketManager listens to odds-update and market-resolved.", "Why not just poll?"],
        ["Geoblock", "Request location -> classify country -> allow/block", "GeoRestrictionManager checks permissions, location, and country code blocklist.", "How do runtime permissions work?"],
    ]
    story.append(table(feature_rows, styles, [2.7 * cm, 3.8 * cm, 5.8 * cm, 3.7 * cm]))

    story.append(p("Important Tradeoffs", styles["h2"]))
    story.append(
        bullets(
            [
                "XML instead of Compose: deliberate scope decision; XML is stable, familiar, and works well with ViewBinding.",
                "SharedPreferences instead of Room: enough for token and balance; Room would be useful for offline market caching.",
                "Socket.io instead of only refresh: real-time prices/resolution are better pushed than repeatedly fetched.",
                "Local JSON backend persistence: practical for course project; production would use a real database.",
                "Cleartext HTTP to 10.0.2.2: acceptable for emulator demo; production requires HTTPS and environment-based config.",
            ],
            styles,
        )
    )

    story.append(PageBreak())
    story.append(p("Q&A Bank: Fast Answers", styles["h1"]))
    qa = [
        ("Why did you use native Android with Kotlin?",
         "Because this is a mobile Android project, and Kotlin is the modern standard Android language. It works naturally with ViewModel, LiveData, coroutines, XML/ViewBinding, and RecyclerView."),
        ("Why XML and not Jetpack Compose?",
         "XML was a stable and predictable choice for the project scope. Compose is valid, but XML plus ViewBinding reduced risk and matched the course material."),
        ("What is your architecture?",
         "A layered architecture: Activity/Fragment for UI, ViewModel for state, Repository for data access, Retrofit/Socket.io for network, and Express backend for server logic."),
        ("Why ViewModel?",
         "It keeps screen state and logic out of the Fragment and handles lifecycle/configuration changes better than putting everything in UI classes."),
        ("Why LiveData?",
         "LiveData lets the UI observe loading/success/error state in a lifecycle-aware way, so fragments update only while their view lifecycle is active."),
        ("Why repository pattern?",
         "It keeps data access out of UI code and gives ViewModels a clean interface for markets, bets, auth, watchlist, profile, and leaderboard."),
        ("Why Retrofit?",
         "Retrofit turns REST endpoints into Kotlin interface functions and automatically maps JSON to model classes using Gson."),
        ("Why OkHttp interceptor?",
         "It attaches the JWT Authorization header in one central place instead of repeating token logic in every request."),
        ("Why RecyclerView?",
         "RecyclerView recycles item views, which is much more efficient than creating a new view for every list row."),
        ("Why ListAdapter and DiffUtil?",
         "They compute differences between old and new lists and update only changed rows, helping performance when markets update."),
        ("Why Glide?",
         "Glide loads images asynchronously and handles caching and placeholders, which keeps image work off the main UI thread."),
        ("Why SharedPreferences?",
         "The app only needs lightweight local session data: token, email, and balance. For sensitive production data, encrypted storage would be better."),
        ("How do you handle loading and errors?",
         "Repositories return a Resource state: Loading, Success, or Error. Fragments observe this and show progress, content, empty state, retry, or Snackbar."),
        ("How does search work?",
         "MarketListViewModel stores the query and debounces for 300ms before loading markets, preventing a network request on every keystroke."),
        ("How do real-time updates work?",
         "MainActivity connects Socket.io, and active ViewModels register listeners for odds-update and market-resolved events, then post updated LiveData."),
        ("Why lifecycle handling for sockets?",
         "A screen may pause or stop. Removing listeners prevents duplicate callbacks, wasted resources, and updates to views that are not active."),
        ("How do you place a bet?",
         "The detail screen validates amount/side, previews shares as amount divided by price, then calls POST /api/bets/place. Backend validates again and returns the new balance."),
        ("What would you improve next?",
         "Encrypted token storage, Room cache for offline markets, WorkManager for background refresh, more tests, production database, and HTTPS configuration."),
    ]
    for q, a in qa:
        story.append(KeepTogether([p(q, styles["h2"]), p(a, styles["body"])]))

    story.append(PageBreak())
    story.append(p("Deep Questions They Might Ask", styles["h1"]))
    story.append(p("Explain the full data path when the user searches for a market.", styles["h2"]))
    story.append(
        numbered(
            [
                "User types in the MarketListFragment search field.",
                "TextWatcher calls MarketListViewModel.onSearchChanged(query).",
                "The ViewModel cancels the previous search job and waits 300ms.",
                "ViewModel calls loadMarkets(), setting Resource.Loading.",
                "MarketRepository calls ApiService.getMarkets(search, category, sort).",
                "Retrofit sends GET /api/markets with query parameters.",
                "Backend routes/markets.js filters, optionally fetches remote Polymarket search results, sorts, and returns JSON.",
                "Repository wraps the response in Resource.Success or Resource.Error.",
                "Fragment observes markets LiveData and renders RecyclerView, empty state, or error state.",
            ],
            styles,
        )
    )
    story.append(p("Explain the full data path when the user places a bet.", styles["h2"]))
    story.append(
        numbered(
            [
                "User selects YES/NO and enters amount.",
                "MarketDetailFragment validates positive amount and selected side.",
                "Shares preview is calculated as amount / selected side price.",
                "MarketDetailViewModel.placeBet() calls BetRepository.placeBet().",
                "Retrofit sends POST /api/bets/place with marketId, side, and amount.",
                "Backend checks auth, market active/resolved state, amount, side, and user balance.",
                "Backend deducts balance, creates a bet record, persists it, and returns bet plus newBalance.",
                "Fragment clears input, shows Snackbar, and calls MainActivity.updateBalance(newBalance).",
            ],
            styles,
        )
    )
    story.append(p("Explain how configuration/lifecycle issues are reduced.", styles["h2"]))
    story.append(
        bullets(
            [
                "ViewModels hold screen state outside direct view code.",
                "LiveData observations are tied to viewLifecycleOwner in fragments.",
                "Bindings are cleared in onDestroyView to avoid retaining destroyed views.",
                "Socket listeners start/stop with visible lifecycle methods, avoiding duplicate listeners.",
                "Network calls run in coroutines through viewModelScope/lifecycleScope instead of blocking the main thread.",
            ],
            styles,
        )
    )

    story.append(PageBreak())
    story.append(p("Definitions To Memorize", styles["h1"]))
    definitions = [
        ["Term", "Definition You Can Say"],
        ["Activity", "A top-level Android component that owns a major app window or flow."],
        ["Fragment", "A reusable UI section hosted inside an activity, useful for multiple screens/tabs."],
        ["ViewModel", "Lifecycle-aware class that stores and prepares UI data; it should not hold direct view references."],
        ["LiveData", "Observable data holder that updates UI in a lifecycle-aware way."],
        ["Repository", "A class that abstracts data sources and exposes clean data operations to ViewModels."],
        ["Retrofit", "Networking library that turns HTTP APIs into Kotlin/Java interfaces."],
        ["OkHttp Interceptor", "A component that can inspect or modify HTTP requests, such as adding an Authorization header."],
        ["RecyclerView", "Efficient Android list component that recycles row views."],
        ["ViewHolder", "Object that holds references to one list row's views."],
        ["ListAdapter", "RecyclerView adapter that computes list differences on a background thread."],
        ["DiffUtil", "Compares old and new list items to update only what changed."],
        ["Glide", "Image loading/caching library for Android."],
        ["SharedPreferences", "Simple key-value local storage for small app data."],
        ["Room", "SQLite abstraction with entities, DAOs, and database classes. Not used in BetNow."],
        ["WorkManager", "Jetpack tool for reliable background work. Not used; possible future improvement."],
        ["Socket.io", "Real-time bidirectional event communication between app and server."],
        ["JWT", "Signed token used by the backend to authenticate protected requests."],
        ["Snackbar", "Temporary feedback message shown at the bottom of the screen."],
    ]
    story.append(table(definitions, styles, [4.0 * cm, 12.0 * cm]))

    story.append(p("Strong Answer Formula", styles["h2"]))
    story.append(
        p(
            "Use this structure: 'We used X because Y. In our app, it appears in Z. The benefit is W.' "
            "Example: 'We used ViewModel because screen logic should not be inside fragments. In our app, MarketListViewModel owns search, sort, category, and loading state. The benefit is cleaner lifecycle-aware UI code.'",
            styles["callout"],
        )
    )

    story.append(PageBreak())
    story.append(p("What Not To Say", styles["h1"]))
    story.append(
        table(
            [
                ["Weak Answer", "Better Answer"],
                ["We used ViewModel because Android says so.", "We used ViewModel to separate screen state from UI rendering and make lifecycle behavior cleaner."],
                ["We used RecyclerView to show a list.", "We used RecyclerView because market lists can be long and changing; recycling and DiffUtil keep updates efficient."],
                ["We used SharedPreferences because it is easy.", "We used SharedPreferences because the local data is lightweight session data. For production secrets, encrypted storage is better."],
                ["We used Retrofit for API.", "Retrofit maps backend endpoints to typed Kotlin functions and converts JSON responses into model objects."],
                ["The backend validates the bet.", "Both frontend and backend validate. Frontend improves UX; backend enforces security and correctness."],
                ["Socket updates prices.", "Socket.io pushes odds-update and market-resolved events, and active screens update LiveData from those events."],
            ],
            styles,
            [6.0 * cm, 10.0 * cm],
        )
    )
    story.append(p("Known Risks / Honest Limitations", styles["h2"]))
    story.append(
        bullets(
            [
                "The app uses cleartext HTTP to a local backend for emulator/demo convenience. Production should use HTTPS.",
                "SharedPreferences is simple but not the strongest place for sensitive tokens. Production should use encrypted storage.",
                "The app does not currently use Room or WorkManager; mention them as possible improvements, not implemented features.",
                "Live real-time changes depend on backend refresh timing and Polymarket data changes, so do not wait for visible price changes during the demo.",
                "Run the Android app from Android Studio before presenting because this checkout does not include the gradlew executable.",
            ],
            styles,
        )
    )

    story.append(PageBreak())
    story.append(p("Last-Minute Cheat Sheet", styles["h1"]))
    story.append(p("Memorize These 10 Lines", styles["h2"]))
    story.append(
        numbered(
            [
                "BetNow is a native Kotlin Android app for prediction markets.",
                "The main flow is login -> markets -> detail -> bet -> watchlist/my bets/profile.",
                "The architecture is Fragment -> ViewModel -> Repository -> Retrofit/Socket -> Backend.",
                "ViewModel keeps UI state out of fragments.",
                "LiveData lets screens react to loading, success, and error states.",
                "Retrofit handles HTTP; OkHttp attaches JWT tokens.",
                "RecyclerView/ListAdapter/DiffUtil make changing lists efficient.",
                "Glide loads and caches market images asynchronously.",
                "SharedPreferences stores lightweight session data.",
                "Socket.io pushes odds and market resolution events in real time.",
            ],
            styles,
        )
    )
    story.append(p("If You Panic During Q&A", styles["h2"]))
    story.append(
        p(
            "Start with the layer: UI, ViewModel, Repository, Network, Backend, Persistence, or Lifecycle. "
            "Then explain the responsibility of that layer and name one concrete file or feature.",
            styles["callout"],
        )
    )
    story.append(p("Final Demo Order", styles["h2"]))
    story.append(
        p(
            "Backend running -> Login/Register -> Markets search/sort/category -> Open detail -> Place bet -> Watchlist -> My Bets -> Profile -> Leaderboard -> mention sockets/geoblock.",
            styles["body"],
        )
    )
    story.append(p("Verification", styles["h2"]))
    story.append(
        bullets(
            [
                "Backend tests passed locally with node --test test/*.test.js: 5 passing tests.",
                "Android build was not run here because the project has gradle-wrapper.properties but no gradlew executable and no system gradle command.",
            ],
            styles,
        )
    )

    doc.build(story)


if __name__ == "__main__":
    build()
