package com.cruaoctkred.app.navigation

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.cruaoctkred.app.ui.header.fintech_2.HeaderSearch
import com.cruaoctkred.app.ui.header.fintech_2.HeaderFilter
import com.cruaoctkred.app.ui.header.fintech_2.HeaderFilters
import com.cruaoctkred.app.ui.header.fintech_2.HeaderNotification
import com.cruaoctkred.app.ui.header.fintech_2.rememberHeaderScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.feature.offers.detail.DetailUiState
import com.cruaoctkred.app.ProductContainer
import com.cruaoctkred.app.R
import com.cruaoctkred.app.ui.DemoOfferLogo
import com.cruaoctkred.app.ui.filter.OfferFilterLimits
import com.cruaoctkred.app.ui.filter.OfferCriteria
import com.cruaoctkred.app.ui.component.ProductHeader
import com.cruaoctkred.app.ui.theme.BrandBackground
import com.cruaoctkred.app.ui.bottomnav.fintech_3.AppNavShell
import com.cruaoctkred.app.ui.bottomnav.fintech_3.BottomBarDefaults
import com.cruaoctkred.app.ui.theme.BrandPrimary
import com.cruaoctkred.app.ui.theme.BrandSecondary
import com.cruaoctkred.app.feature.compare.fintech_1.CompareScreen
import com.cruaoctkred.app.feature.offers.detail.DetailLayout
import com.cruaoctkred.app.feature.offers.detail.DetailScreen
import com.cruaoctkred.app.feature.offers.detail.DetailViewModel
import com.cruaoctkred.app.feature.offers.home.HomeEffect
import com.cruaoctkred.app.feature.offers.home.HomeScreen
import com.cruaoctkred.app.feature.offers.home.HomeViewModel
import com.cruaoctkred.app.feature.rules.RulesScreen
import com.cruaoctkred.app.feature.rules.RulesViewModel
import com.cruaoctkred.app.feature.saved.grid.SavedScreen
import com.cruaoctkred.app.ui.bottomnav.fintech_3.BottomBarItem
import com.cruaoctkred.app.ui.offercard.segmented.AppOfferCard
import com.cruaoctkred.app.ui.offercard.segmented.OfferCardDefaults
import com.cruaoctkred.app.feature.offerweb.OfferWebScreen

@Composable
fun AppNavGraph(container: ProductContainer, currentLanguage: String, onToggleLanguage: () -> Unit, openReminder: Boolean = false, onReminderHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(openReminder) {
        if (openReminder) { navController.navigate("payment-reminder") { launchSingleTop = true }; onReminderHandled() }
    }
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
        container.cardsRepository, container.selectionRepository, container.ctaCoordinator,
        container.binom, container.attributionRepository, container.pushTokenRepository,
    ))
    val homeState by homeViewModel.state.collectAsState()
    LaunchedEffect(currentLanguage) { homeViewModel.refresh() }
    val routing by homeViewModel.routing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) homeViewModel.registerPushToken()
    }

    LaunchedEffect(resources) {
        homeViewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenDetail -> navController.navigate(Routes.detail(effect.cardId)) { launchSingleTop = true }
                is HomeEffect.OpenWeb -> navController.navigate(Routes.web(effect.cardId, Uri.encode(effect.url)))
                is HomeEffect.Message -> snackbarHostState.showSnackbar(
                    resources.getString(when (effect.kind) {
                        HomeEffect.Message.Kind.Offline -> R.string.web_offline
                        HomeEffect.Message.Kind.InvalidLink -> R.string.web_invalid_url
                        HomeEffect.Message.Kind.CompareLimit -> R.string.compare_limit
                    }),
                )
                HomeEffect.RequestPushPermission -> {
                    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else homeViewModel.registerPushToken()
                }
            }
        }
    }

    NavHost(navController, startDestination = Routes.MAIN) {
        composable("payment-reminder") {
            com.cruaoctkred.app.reminders.PaymentReminderScreen { navController.popBackStack() }
        }
        composable(Routes.MAIN) {
            MainShell(
                container = container,
                homeViewModel = homeViewModel,
                snackbarHostState = snackbarHostState,
                currentLanguage = currentLanguage,
                onToggleLanguage = onToggleLanguage,
                onOpenReminder = { navController.navigate("payment-reminder") },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("cardId") { type = NavType.StringType })) { entry ->
            val cardId = entry.arguments?.getString("cardId").orEmpty()
            val detailViewModel: DetailViewModel = viewModel(key = cardId, factory = DetailViewModel.Factory(container.cardsRepository, cardId))
            val state by detailViewModel.state.collectAsState()
            DetailScreen(
                state = when (val detail = state) {
                    is DetailUiState.Content -> detail.copy(card = localizeOfferCard(detail.card, context))
                    is DetailUiState.Error -> detail.copy(cached = detail.cached?.let { localizeOfferCard(it, context) })
                    DetailUiState.Loading -> detail
                },
                saved = cardId in homeState.savedIds,
                onBack = { navController.popBackStack() },
                onSave = homeViewModel::toggleSaved,
                onCta = { homeViewModel.openOffer(it, "detail") },
                backgroundPainter = painterResource(R.drawable.bg_abstract),
                backgroundAlpha = 1f,
                layout = DetailLayout.Premium,
                showOfflineBanner = false,
                canOpenOffer = when (val detail = state) {
                    is DetailUiState.Content -> routing.allows(detail.card)
                    is DetailUiState.Error -> detail.cached?.let(routing::allows) == true
                    else -> false
                },
            )
        }
        composable(Routes.WEB, arguments = listOf(navArgument("url") { type = NavType.StringType }, navArgument("cardId") { type = NavType.StringType })) { entry ->
            val cardId = entry.arguments?.getString("cardId").orEmpty()
            val card = homeState.cards.firstOrNull { it.id == cardId }
            val allowed = card?.let(routing::allows) == true
            if (allowed) {
                OfferWebScreen(entry.arguments?.getString("url").orEmpty(), onClose = { navController.popBackStack() })
            } else {
                LaunchedEffect(cardId) {
                    navController.navigate(Routes.detail(cardId)) {
                        popUpTo(Routes.WEB) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

private data class Destination(val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun MainShell(
    container: ProductContainer,
    homeViewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    currentLanguage: String,
    onToggleLanguage: () -> Unit,
    onOpenReminder: () -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val shellView = androidx.compose.ui.platform.LocalView.current
    val shellActivity = shellView.context as? android.app.Activity
    androidx.compose.runtime.LaunchedEffect(selected) {
        shellActivity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, shellView).isAppearanceLightStatusBars = selected != 0 }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var filterId by rememberSaveable { mutableStateOf("all") }
    var amountFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var daysFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var rateFilter by rememberSaveable { mutableStateOf<Double?>(null) }
    val criteria = OfferCriteria(amountFilter, daysFilter, rateFilter)
    val headerScrollState = rememberHeaderScrollState()
    val context = LocalContext.current
    val homeState by homeViewModel.state.collectAsState()
    val routing by homeViewModel.routing.collectAsState()
    val cards = homeState.cards.map { localizeOfferCard(it, context) }
    val visibleCards = com.cruaoctkred.app.ui.filter.sortOfferCategory(
        cards.filter { card -> card.title.contains(query.trim(), ignoreCase = true) && criteria.matches(card) },
        filterId,
    )
    val rulesViewModel: RulesViewModel = viewModel(factory = RulesViewModel.Factory(container.pagesRepository))
    val rulesState by rulesViewModel.state.collectAsState()
    val destinations = listOf(
        Destination(R.string.nav_discover, Icons.Default.Home),
        Destination(R.string.nav_compare, Icons.AutoMirrored.Filled.CompareArrows),
        Destination(R.string.nav_saved, Icons.Default.Bookmark),
        Destination(R.string.nav_rules, Icons.Default.Info),
    )
    // Navigation shell comes from the chosen ui-bottomnav-* variant; it owns where the
    // bar sits and how much room the content gives up. Swapping variants changes the import.
    AppNavShell(
        items = destinations.map { BottomBarItem(stringResource(it.label), it.icon) },
        selected = selected,
        onSelect = { selected = it },
        colors = BottomBarDefaults.colors(
            container = BrandBackground,
            selectedIcon = BrandSecondary,
            selectedText = BrandSecondary,
            indicator = BrandPrimary.copy(alpha = 0.20f),
            unselectedIcon = BrandSecondary,
            unselectedText = BrandSecondary,
        ),
    ) { reservation ->
        Scaffold(
            modifier = Modifier.fillMaxSize().paint(
                painterResource(R.drawable.bg_abstract),
                contentScale = ContentScale.Crop,
            ),
            containerColor = Color.Transparent,
            topBar = {
                if (selected == 0) {
                    ProductHeader(
                        criteria = criteria,
                        filterLimits = OfferFilterLimits.fromCards(cards),
                        onCriteriaChange = {
                            amountFilter = it.amount
                            daysFilter = it.days
                            rateFilter = it.maxRate
                        },
                        search = if (selected == 0) HeaderSearch(query, stringResource(R.string.search_offers), { query = it }) else null,
                        filters = if (selected == 0) HeaderFilters(
                            listOf(
                                HeaderFilter("all", stringResource(R.string.filter_all), "≡"),
                                HeaderFilter("hot", stringResource(R.string.category_hot), ""),
                                HeaderFilter("new", stringResource(R.string.category_new), ""),
                                HeaderFilter("best_rate", stringResource(R.string.category_best_rate), ""),
                            ), filterId, { filterId = it },
                        ) else null,
                        notification = HeaderNotification(stringResource(R.string.reminder_title), onOpenReminder),
                        scrollState = headerScrollState,
                        currentLanguage = currentLanguage,
                        onToggleLanguage = onToggleLanguage,
                        offerCount = visibleCards.size.takeIf { selected == 0 },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(end = reservation)) {
                val contentModifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
                when (selected) {
                    0 -> HomeScreen(
                        homeState.copy(cards = visibleCards), homeViewModel::refresh,
                        homeViewModel::openDetail, homeViewModel::toggleSaved, homeViewModel::toggleCompared,
                        onCta = { homeViewModel.openOffer(it, "home") }, onStorefrontVisible = homeViewModel::storefrontVisible,
                        modifier = contentModifier.nestedScroll(headerScrollState.nestedScrollConnection),
                        showOfflineBanner = false,
                        showFeedHeader = false,
                        offerCard = { card, saved, compared ->
                            AppOfferCard(
                                card = card,
                                amountText = card.firstCreditRange.from?.let { amount ->
                                    stringResource(R.string.range_from,
                                        java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
                                            .format(amount).replace(',', ' '),
                                        stringResource(R.string.unit_currency))
                                } ?: card.firstCredit,
                                canOpenOffer = routing.allows(card),
                                saved = saved,
                                compared = compared,
                                onOpenDetail = { homeViewModel.openDetail(card.id) },
                                onSave = { homeViewModel.toggleSaved(card.id) },
                                onCompare = { homeViewModel.toggleCompared(card.id) },
                                onCta = { homeViewModel.openOffer(card, "home") },
                                colors = OfferCardDefaults.colors(
                                    accent = BrandPrimary,
                                    accentDeep = BrandSecondary,
                                    gradientStart = BrandSecondary,
                                    gradientEnd = BrandPrimary,
                                ),
                                icons = OfferCardDefaults.icons.copy(
                                    term = R.drawable.ic_offer_calendar,
                                    decisionTime = R.drawable.ic_offer_stopwatch,
                                ),
                                logoContent = if (card.logoUrl?.startsWith("android.resource://") == true && (card.id == "demo-moneycat" || card.id == "demo-kviku")) {
                                    { DemoOfferLogo(card.id, BrandPrimary.copy(alpha = 0.45f)) }
                                } else null,
                            )
                        },
                    )
                    1 -> CompareScreen(
                        cards, homeState.comparedIds, homeViewModel::toggleCompared, homeViewModel::openDetail,
                        onCta = { homeViewModel.openOffer(it, "compare") },
                        onBrowse = { selected = 0 },
                        modifier = contentModifier,
                    )
                    2 -> SavedScreen(
                        cards, homeState.savedIds, homeState.comparedIds, homeViewModel::openDetail,
                        homeViewModel::toggleSaved, homeViewModel::toggleCompared,
                        onCta = { homeViewModel.openOffer(it, "saved") }, modifier = contentModifier,
                        canOpenOffer = routing::allows,
                    )
                    else -> RulesScreen(rulesState, onRetry = {}, modifier = contentModifier)
                }
            }
        }
    }
}

private fun localizeOfferCard(card: LoanCard, context: Context): LoanCard {
    val language = context.resources.configuration.locales[0].language
    fun text(value: String) = com.cruaoctkred.app.ui.localization.localizeOfferText(value, language)
    return card.copy(
        firstCredit = text(card.firstCredit), repeatCredit = text(card.repeatCredit),
        term = text(card.term), age = text(card.age), decisionTime = text(card.decisionTime),
        rate = text(card.rate),
        aprCalculation = com.cruaoctkred.app.ui.localization.localizeCurrency(card.aprCalculation),
    )
}
