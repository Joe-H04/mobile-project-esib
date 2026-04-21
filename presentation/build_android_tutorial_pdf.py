from html import escape

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
    LongTable,
    PageTemplate,
    Paragraph,
    Spacer,
    TableStyle,
)


OUTPUT = "presentation/BetNow_Android_App_Tutorial.pdf"


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
            spaceAfter=10,
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
            textColor=colors.HexColor("#0F172A"),
            spaceBefore=12,
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
            leading=12.6,
            textColor=colors.HexColor("#111827"),
            spaceAfter=5,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.7,
            leading=11.5,
            leftIndent=10,
            bulletIndent=0,
            textColor=colors.HexColor("#111827"),
            spaceAfter=3,
        ),
        "callout": ParagraphStyle(
            "Callout",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=9,
            leading=12.6,
            textColor=colors.HexColor("#111827"),
            borderColor=colors.HexColor("#CBD5E1"),
            borderWidth=0.7,
            borderPadding=7,
            backColor=colors.HexColor("#F8FAFC"),
            spaceBefore=5,
            spaceAfter=8,
        ),
        "code": ParagraphStyle(
            "Code",
            parent=base["Code"],
            fontName="Courier",
            fontSize=7.4,
            leading=9.2,
            textColor=colors.HexColor("#111827"),
            backColor=colors.HexColor("#F3F4F6"),
            borderPadding=5,
            spaceBefore=3,
            spaceAfter=6,
        ),
        "table": ParagraphStyle(
            "TableText",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=7.4,
            leading=9.6,
            textColor=colors.HexColor("#111827"),
        ),
        "table_head": ParagraphStyle(
            "TableHead",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=7.6,
            leading=9.6,
            textColor=colors.white,
            alignment=TA_LEFT,
        ),
    }


def on_page(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#6B7280"))
    canvas.drawString(1.55 * cm, 1.05 * cm, "BetNow Android App Tutorial")
    canvas.drawRightString(19.45 * cm, 1.05 * cm, f"Page {doc.page}")
    canvas.restoreState()


def p(text, style):
    return Paragraph(escape(text), style)


def code(text, style):
    return Paragraph(escape(text).replace("\n", "<br/>"), style)


def bullets(items, styles):
    return ListFlowable(
        [ListItem(p(item, styles["bullet"])) for item in items],
        bulletType="bullet",
        start="circle",
        leftIndent=12,
        bulletFontSize=6,
        spaceAfter=6,
    )


def numbered(items, styles):
    return ListFlowable(
        [ListItem(p(item, styles["bullet"])) for item in items],
        bulletType="1",
        leftIndent=15,
        bulletFontSize=8,
        spaceAfter=6,
    )


def table(data, styles, col_widths):
    rows = []
    for row_index, row in enumerate(data):
        rows.append([
            p(str(cell), styles["table_head"] if row_index == 0 else styles["table"])
            for cell in row
        ])
    result = LongTable(rows, colWidths=col_widths, repeatRows=1)
    result.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#111827")),
                ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#CBD5E1")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F8FAFC")]),
            ]
        )
    )
    return result


FILE_ROWS = [
    ("settings.gradle.kts", "Project registration", "Names the project and includes the app module."),
    ("build.gradle.kts", "Root build setup", "Declares Android/Kotlin plugins available to modules."),
    ("gradle.properties", "Build flags", "Stores Gradle and Android build configuration."),
    ("gradle/wrapper/gradle-wrapper.properties", "Gradle version", "Pins the Gradle distribution expected by the project."),
    ("app/build.gradle.kts", "App build setup", "Sets namespace, SDK versions, app id, ViewBinding, and dependencies."),
    ("AndroidManifest.xml", "App declaration", "Declares permissions, app class, theme, launcher activity, and activities."),
    ("BetNowApplication.kt", "Application class", "Runs app-wide initialization before any activity opens."),
    ("data/local/BetNowDatabase.kt", "Room database", "Defines the local database and DAO access."),
    ("data/local/CachedProfileEntity.kt", "Room table", "Represents cached profile data as a database row."),
    ("data/local/ProfileDao.kt", "Room DAO", "Defines SQL-style functions for profile cache reads/writes."),
    ("network/ApiService.kt", "REST API contract", "Declares Retrofit endpoints."),
    ("network/RetrofitClient.kt", "HTTP client", "Builds Retrofit/OkHttp and attaches authentication."),
    ("network/SocketManager.kt", "Market realtime socket", "Receives live market/odds events."),
    ("network/SupportSocket.kt", "Support chat socket", "Handles realtime customer support messages."),
    ("network/models/AuthModels.kt", "Auth data models", "Request/response classes for login/register."),
    ("network/models/BetModels.kt", "Bet data models", "Request/response classes for bets and redemption."),
    ("network/models/LeaderboardModels.kt", "Leaderboard models", "Shape of leaderboard API data."),
    ("network/models/MarketModels.kt", "Market models", "Shape of markets, categories, and odds data."),
    ("network/models/ProfileModels.kt", "Profile models", "Shape of user/profile/statistics data."),
    ("network/models/SupportModels.kt", "Support models", "Shape of support messages and chat data."),
    ("repository/AuthRepository.kt", "Auth data layer", "Calls auth endpoints and wraps results."),
    ("repository/BetRepository.kt", "Bet data layer", "Loads bets and sends place/redeem requests."),
    ("repository/LeaderboardRepository.kt", "Leaderboard data layer", "Loads leaderboard entries."),
    ("repository/MarketRepository.kt", "Market data layer", "Loads markets/categories and listens to market sockets."),
    ("repository/ProfileRepository.kt", "Profile data layer", "Loads profile data and uses the local cache."),
    ("repository/WatchlistRepository.kt", "Watchlist data layer", "Adds, removes, and loads saved markets."),
    ("ui/auth/AuthViewModel.kt", "Auth screen state", "Coordinates login/register async work."),
    ("ui/auth/LoginActivity.kt", "Login screen", "Reads credentials, runs geo check, stores token, opens main screen."),
    ("ui/auth/RegisterActivity.kt", "Register screen", "Creates an account and returns to login/main flow."),
    ("ui/main/MainActivity.kt", "Main shell", "Hosts toolbar, bottom navigation, and fragment switching."),
    ("ui/markets/MarketListFragment.kt", "Markets tab UI", "Displays search, sort, category chips, and market list."),
    ("ui/markets/MarketListViewModel.kt", "Markets state", "Loads, filters, sorts, watches, and updates market data."),
    ("ui/markets/MarketListAdapter.kt", "Market list rows", "Binds Market objects into item_market.xml cards."),
    ("ui/detail/MarketDetailFragment.kt", "Market detail UI", "Shows market details, odds, watchlist button, and place-bet form."),
    ("ui/detail/MarketDetailViewModel.kt", "Market detail state", "Loads a market, toggles watchlist, places bets."),
    ("ui/watchlist/WatchlistFragment.kt", "Watchlist tab UI", "Shows saved markets."),
    ("ui/watchlist/WatchlistViewModel.kt", "Watchlist state", "Loads/removes saved markets and handles live odds updates."),
    ("ui/bets/MyBetsFragment.kt", "Bets tab UI", "Shows user bets and redemption actions."),
    ("ui/bets/MyBetsViewModel.kt", "Bets state", "Loads bets and redeems resolved bets."),
    ("ui/bets/MyBetsAdapter.kt", "Bet list rows", "Binds Bet objects into item_bet.xml cards."),
    ("ui/leaderboard/LeaderboardFragment.kt", "Leaderboard tab UI", "Displays top users."),
    ("ui/leaderboard/LeaderboardViewModel.kt", "Leaderboard state", "Loads leaderboard data."),
    ("ui/leaderboard/LeaderboardAdapter.kt", "Leaderboard rows", "Binds leaderboard entries into item_leaderboard.xml."),
    ("ui/profile/ProfileFragment.kt", "Profile tab UI", "Shows email, balance, stats, support, and logout."),
    ("ui/profile/ProfileViewModel.kt", "Profile state", "Loads profile and statistics."),
    ("ui/support/SupportActivity.kt", "Support chat screen", "Displays support messages and sends new ones."),
    ("ui/support/SupportAdapter.kt", "Support rows", "Binds support messages into user/admin chat bubbles."),
    ("ui/geo/GeoBlockActivity.kt", "Geo block screen", "Explains region restrictions and lets the user retry."),
    ("util/Resource.kt", "State wrapper", "Represents Loading, Success, and Error states."),
    ("util/TokenManager.kt", "Local auth storage", "Stores token, user id, email, and balance."),
    ("util/UiFormatters.kt", "Formatting helpers", "Formats money, shares, prices, and dates."),
    ("util/GeoRestrictionManager.kt", "Region checks", "Determines whether the app can be used in the current location."),
]


RESOURCE_ROWS = [
    ("layout/activity_*.xml", "Full-screen layouts for activities like login, register, main, support, and geo block."),
    ("layout/fragment_*.xml", "Layouts for screens hosted inside MainActivity tabs."),
    ("layout/item_*.xml", "Reusable row/card layouts used by RecyclerView adapters."),
    ("menu/bottom_nav_menu.xml", "Defines the bottom navigation tabs."),
    ("menu/toolbar_menu.xml", "Defines toolbar actions such as logout."),
    ("values/strings.xml", "All user-facing text. Keep copy here instead of hardcoding strings."),
    ("values/colors.xml", "Named colors used throughout the app."),
    ("values/styles.xml", "Reusable Material styles for cards, buttons, chips, inputs, and navigation."),
    ("values/themes.xml", "Global app theme applied by the manifest."),
    ("drawable/bg_*.xml", "Shape drawables for backgrounds, badges, toolbar, chat bubbles, and cards."),
    ("drawable/ic_*.xml", "Vector icons used in navigation, cards, profile, and watchlist controls."),
    ("color/*.xml", "State-list colors for selected/focused/default UI states."),
]


def build():
    styles = make_styles()
    doc = BaseDocTemplate(
        OUTPUT,
        pagesize=A4,
        leftMargin=1.55 * cm,
        rightMargin=1.55 * cm,
        topMargin=1.45 * cm,
        bottomMargin=1.65 * cm,
    )
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="normal")
    doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=on_page)])

    story = []
    story.append(Spacer(1, 1.8 * cm))
    story.append(p("BetNow Android App Tutorial", styles["title"]))
    story.append(
        p(
            "A compact file-by-file guide to how this native Android app is structured and how to build one like it.",
            styles["subtitle"],
        )
    )
    story.append(
        p(
            "Core idea: screens should own UI behavior, ViewModels should own screen state, repositories should own data access, and resources should own design/text assets.",
            styles["callout"],
        )
    )

    story.append(p("1. The Android App Shape", styles["h1"]))
    story.append(
        p(
            "This project is a native Android app written in Kotlin with XML layouts. It uses Activities for full screens, Fragments for tab content, ViewModels for state, Retrofit for HTTP, Socket.IO for realtime events, Room for local caching, and Material Components for UI.",
            styles["body"],
        )
    )
    story.append(
        code(
            "Fragment -> ViewModel -> Repository -> API / Database\n"
            "XML layout -> ViewBinding -> Kotlin screen controller\n"
            "RecyclerView -> Adapter -> item_*.xml row layout",
            styles["code"],
        )
    )

    story.append(p("2. Project And Build Files", styles["h1"]))
    story.append(
        p(
            "Before writing screens, Gradle needs to know what the app is, which SDK to compile against, and which libraries are available.",
            styles["body"],
        )
    )
    story.append(
        table(
            [["File", "Purpose", "What you write there"]] + FILE_ROWS[:5],
            styles,
            [5.1 * cm, 4.0 * cm, 8.1 * cm],
        )
    )

    story.append(p("3. Entry Point And Navigation", styles["h1"]))
    story.append(
        p(
            "Android starts from the launcher Activity declared in the manifest. In this app, LoginActivity starts first, then MainActivity hosts the tabbed experience after login.",
            styles["body"],
        )
    )
    story.append(
        code(
            "binding.bottomNav.setOnItemSelectedListener { item ->\n"
            "    when (item.itemId) {\n"
            "        R.id.nav_markets -> loadFragment(MarketListFragment())\n"
            "        R.id.nav_profile -> loadFragment(ProfileFragment())\n"
            "    }\n"
            "    true\n"
            "}",
            styles["code"],
        )
    )

    story.append(p("4. Activities, Fragments, And Layouts", styles["h1"]))
    story.append(
        p(
            "Activities and Fragments are Kotlin controllers. XML files define what those controllers render. ViewBinding connects them safely by generating binding classes from XML ids.",
            styles["body"],
        )
    )
    story.append(
        code(
            "private var _binding: FragmentExampleBinding? = null\n"
            "private val binding get() = _binding!!\n\n"
            "override fun onDestroyView() {\n"
            "    super.onDestroyView()\n"
            "    _binding = null\n"
            "}",
            styles["code"],
        )
    )
    story.append(
        p(
            "Lifecycle rule: do not touch binding after onDestroyView. If you post delayed UI work, re-check _binding before using views.",
            styles["callout"],
        )
    )

    story.append(p("5. ViewModels And Repositories", styles["h1"]))
    story.append(
        p(
            "A ViewModel exposes LiveData to the screen. A repository hides where data comes from. This separation keeps UI code smaller and makes loading/error handling consistent.",
            styles["body"],
        )
    )
    story.append(
        code(
            "class ExampleViewModel : ViewModel() {\n"
            "    private val _state = MutableLiveData<Resource<Data>>()\n"
            "    val state: LiveData<Resource<Data>> = _state\n\n"
            "    fun load() = viewModelScope.launch {\n"
            "        _state.value = Resource.Loading()\n"
            "        _state.value = repository.getData()\n"
            "    }\n"
            "}",
            styles["code"],
        )
    )

    story.append(p("6. Networking, Models, And Local Cache", styles["h1"]))
    story.append(
        bullets(
            [
                "ApiService declares REST endpoints using Retrofit annotations.",
                "RetrofitClient creates the HTTP client and adds auth headers.",
                "Model files describe request and response JSON shapes.",
                "SocketManager and SupportSocket handle realtime updates.",
                "Room database files cache local profile data.",
            ],
            styles,
        )
    )

    story.append(p("7. RecyclerView Lists", styles["h1"]))
    story.append(
        p(
            "Any repeating list usually needs a RecyclerView, an Adapter, and an item XML layout. The adapter receives data objects and binds their values into row views.",
            styles["body"],
        )
    )
    story.append(
        code(
            "class MarketListAdapter : ListAdapter<Market, ViewHolder>(Diff()) {\n"
            "    fun bind(market: Market) {\n"
            "        binding.marketQuestion.text = market.question\n"
            "    }\n"
            "}",
            styles["code"],
        )
    )

    story.append(p("8. Android Resources", styles["h1"]))
    story.append(
        p(
            "Android resources keep design assets outside Kotlin. This makes UI reusable, localizable, and easier to theme.",
            styles["body"],
        )
    )
    story.append(
        table(
            [["Resource group", "What it does"]] + RESOURCE_ROWS,
            styles,
            [5.2 * cm, 12.0 * cm],
        )
    )

    story.append(p("9. File-By-File Reference", styles["h1"]))
    story.append(
        p(
            "Use this table as a map when opening the codebase. Most Android apps have the same layers even when the names differ.",
            styles["body"],
        )
    )
    story.append(
        table(
            [["File", "Role", "What to learn from it"]] + FILE_ROWS,
            styles,
            [5.2 * cm, 3.6 * cm, 8.4 * cm],
        )
    )

    story.append(p("10. Build Your Own Android App", styles["h1"]))
    story.append(
        numbered(
            [
                "Start with the Gradle app module and enable ViewBinding.",
                "Create a Manifest with your launcher Activity and app theme.",
                "Create XML layouts for screens and reusable list rows.",
                "Write Activities or Fragments to control those layouts.",
                "Create ViewModels for loading state and screen actions.",
                "Create Repositories to call APIs or databases.",
                "Define Retrofit models and endpoints for backend data.",
                "Use RecyclerView adapters for lists.",
                "Move colors, strings, shapes, icons, and styles into res/.",
                "Test lifecycle edges: rotate, switch tabs quickly, leave and return to the app.",
            ],
            styles,
        )
    )

    story.append(
        KeepTogether(
            [
                p("Key Patterns To Memorize", styles["h1"]),
                bullets(
                    [
                        "Activity: a full screen and navigation host.",
                        "Fragment: a screen section, often used inside tabs.",
                        "ViewModel: state holder that survives configuration changes.",
                        "Repository: data access boundary.",
                        "Resource: Loading / Success / Error wrapper.",
                        "Adapter: turns a list of data into rows.",
                        "XML resources: layout, theme, colors, strings, icons, and state styling.",
                    ],
                    styles,
                ),
            ]
        )
    )

    doc.build(story)


if __name__ == "__main__":
    build()
