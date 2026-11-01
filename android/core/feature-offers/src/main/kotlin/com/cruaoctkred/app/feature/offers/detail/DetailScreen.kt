package com.cruaoctkred.app.feature.offers.detail

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.ValueRange
import java.text.NumberFormat
import com.cruaoctkred.app.feature.offers.R
import com.cruaoctkred.app.ui.components.ErrorBlock
import com.cruaoctkred.app.ui.components.LoadingBlock
import com.cruaoctkred.app.ui.components.OfferLogo
import com.cruaoctkred.app.ui.components.OfflineBanner
import com.cruaoctkred.app.ui.components.Shapes
import com.cruaoctkred.app.ui.components.Spacing

private data class DetailOfferAccess(val allowed: Boolean = false, val open: () -> Unit = {})
private val LocalDetailOfferAccess = androidx.compose.runtime.staticCompositionLocalOf { DetailOfferAccess() }

/** How the detail body is arranged. [Sections] is the original single scroll. */
enum class DetailLayout { Sections, Compact, Premium }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    saved: Boolean,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onCta: (LoanCard) -> Unit,
    modifier: Modifier = Modifier,
    backgroundPainter: Painter? = null,
    /** 0.14 keeps a photo as a watermark; pass 1f for a backdrop meant to be seen. */
    backgroundAlpha: Float = 0.14f,
    layout: DetailLayout = DetailLayout.Sections,
    showOfflineBanner: Boolean = true,
    canOpenOffer: Boolean = false,
) {
    val card = when (state) {
        is DetailUiState.Content -> state.card
        is DetailUiState.Error -> state.cached
        DetailUiState.Loading -> null
    }
    val listState = rememberLazyListState()
    val view = androidx.compose.ui.platform.LocalView.current
    val activity = view.context as? android.app.Activity
    val heroOffscreen by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    if (layout == DetailLayout.Premium && activity != null) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(activity.window, view)
        androidx.compose.runtime.LaunchedEffect(controller, heroOffscreen) { controller.isAppearanceLightStatusBars = heroOffscreen }
    }
    val compactTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 180 }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (layout == DetailLayout.Premium) WindowInsets(0, 0, 0, 0) else androidx.compose.material3.ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (layout != DetailLayout.Premium || card == null) TopAppBar(
                title = {
                    Text(
                        if (compactTitle && card != null) card.title else stringResource(R.string.detail_title),
                        fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (card != null) {
                        IconButton(onClick = { onSave(card.id) }) {
                            Icon(
                                if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                stringResource(if (saved) R.string.saved else R.string.save),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (card != null && canOpenOffer) {
                Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        Button(
                            onClick = { onCta(card) },
                            enabled = !card.linkUrl.isNullOrBlank(),
                            modifier = Modifier.fillMaxWidth().height(56.dp).brandGradient(RoundedCornerShape(18.dp)),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        ) {
                            Text(stringResource(R.string.get_money).uppercase(), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                        Text(
                            stringResource(if (card.linkUrl.isNullOrBlank()) R.string.premium_no_link else R.string.provider_redirect_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state == DetailUiState.Loading -> LoadingBlock(stringResource(R.string.loading), Modifier.padding(padding))
            card == null -> ErrorBlock(message = stringResource(R.string.generic_error), onRetry = onBack, retryLabel = stringResource(R.string.retry), modifier = Modifier.padding(padding))
            else -> DetailBackground(backgroundPainter, backgroundAlpha, Modifier.padding(padding)) {
                val offline = showOfflineBanner && (state is DetailUiState.Error || (state is DetailUiState.Content && state.offline))
                androidx.compose.runtime.CompositionLocalProvider(LocalDetailOfferAccess provides DetailOfferAccess(canOpenOffer) { onCta(card) }) {
                when (layout) {
                    DetailLayout.Sections -> DetailContent(card, offline, listState)
                    DetailLayout.Compact -> DetailCompact(card, offline, listState)
                    DetailLayout.Premium -> PremiumDetail(card, offline, listState, saved, onBack, { onSave(card.id) })
                }
                }
            }
        }
    }
}

@Composable
private fun DetailBackground(
    backgroundPainter: Painter?,
    backgroundAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (backgroundPainter != null) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = backgroundAlpha,
            )
        }
        Box(
            Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter),
        ) {
            content()
        }
    }
}




@Composable
private fun TermRow(label: String, value: String, accented: Boolean = false, last: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifBlank { "\u2014" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (accented) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DetailCompact(card: LoanCard, offline: Boolean, listState: LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (offline) item { OfflineBanner(stringResource(R.string.offline_banner)) }
        item { OfferHero(card, compact = true) }
        item {
            Surface(
                shape = Shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                    TermRow(
                        stringResource(R.string.first_credit),
                        rangeText(card.firstCreditRange, stringResource(R.string.unit_currency), card.firstCredit),
                    )
                    TermRow(
                        stringResource(R.string.repeat_credit),
                        rangeText(card.repeatCreditRange, stringResource(R.string.unit_currency), card.repeatCredit),
                    )
                    TermRow(stringResource(R.string.rate), card.rate)
                    TermRow(
                        stringResource(R.string.term),
                        rangeText(card.termRange, stringResource(R.string.unit_days), card.term),
                    )
                    TermRow(stringResource(R.string.decision_time), card.decisionTime)
                    TermRow(
                        stringResource(R.string.age),
                        rangeText(card.ageRange, stringResource(R.string.unit_years), card.age),
                        last = card.apr.isBlank(),
                    )
                    if (card.apr.isNotBlank()) {
                        TermRow(stringResource(R.string.apr), card.apr, accented = true, last = true)
                    }
                }
            }
        }
        if (card.aprCalculation.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
                ) {
                    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                stringResource(R.string.apr_calculation),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Text(
                            card.aprCalculation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
        item { LenderDetails(card) }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                shape = Shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    stringResource(R.string.legal_disclaimer),
                    modifier = Modifier.padding(Spacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    card: LoanCard,
    offline: Boolean,
    listState: LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (offline) item { OfflineBanner(stringResource(R.string.offline_banner)) }
        item { OfferHero(card) }
        item { SectionTitle(stringResource(R.string.key_terms)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MetricTile(
                    stringResource(R.string.first_credit),
                    rangeText(card.firstCreditRange, stringResource(R.string.unit_currency), card.firstCredit),
                    Modifier.weight(1f),
                    emphasized = true,
                )
                MetricTile(
                    stringResource(R.string.repeat_credit),
                    rangeText(card.repeatCreditRange, stringResource(R.string.unit_currency), card.repeatCredit),
                    Modifier.weight(1f),
                    emphasized = true,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MetricTile(stringResource(R.string.rate), card.rate, Modifier.weight(1f))
                MetricTile(
                    stringResource(R.string.term),
                    rangeText(card.termRange, stringResource(R.string.unit_days), card.term),
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MetricTile(stringResource(R.string.decision_time), card.decisionTime, Modifier.weight(1f))
                MetricTile(
                    stringResource(R.string.age),
                    rangeText(card.ageRange, stringResource(R.string.unit_years), card.age),
                    Modifier.weight(1f),
                )
            }
        }
        item { AprCard(card) }
        item { LenderDetails(card) }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                shape = Shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            ) {
                Text(
                    stringResource(R.string.legal_disclaimer),
                    modifier = Modifier.padding(Spacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OfferHero(card: LoanCard, compact: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = Shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (compact) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Column(
            Modifier.padding(if (compact) Spacing.md else Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(if (compact) Spacing.sm else Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = Shapes.small, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) {
                    OfferLogo(
                        card,
                        Modifier
                            .size(
                                width = if (compact) 64.dp else 82.dp,
                                height = if (compact) 44.dp else 58.dp,
                            )
                            .padding(Spacing.xs),
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        card.title,
                        style = if (compact) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB020), modifier = Modifier.size(if (compact) 16.dp else 19.dp))
                        Spacer(Modifier.width(Spacing.xxs))
                        Text("%.1f".format(card.rating), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Compact puts the headline amount and the decision pill on one line; the tall
            // variant stacks them, which is what the original sections layout expects.
            if (compact) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.available_up_to),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                        Text(
                            card.repeatCredit.ifBlank { card.firstCredit },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(Spacing.xs))
                    DecisionPill(card, compact = true)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        stringResource(R.string.available_up_to),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                    Text(
                        card.repeatCredit.ifBlank { card.firstCredit },
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DecisionPill(card, compact = false)
            }
        }
    }
}

@Composable
private fun DecisionPill(card: LoanCard, compact: Boolean) {
    Surface(color = Color.White, shape = Shapes.pill, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .75f))) {
        Row(
            Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(if (compact) 15.dp else 18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.xs))
            Text(
                if (compact) card.decisionTime else stringResource(R.string.decision_in, card.decisionTime),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
}

/**
 * Renders the numeric bounds the admin entered. With both bounds it shows the span, with a
 * single bound only that side, and it falls back to the legacy display string for cards
 * saved before the ranges existed.
 */
@Composable
private fun rangeText(range: ValueRange, unit: String, fallback: String): String {
    if (range.isEmpty) return fallback
    val formatter = remember { NumberFormat.getIntegerInstance() }
    val from = range.from?.let { formatter.format(it) }
    val to = range.to?.let { formatter.format(it) }
    return when {
        from != null && to != null -> "$from – $to $unit"
        from != null -> stringResource(R.string.range_from, from, unit)
        else -> stringResource(R.string.range_to, to.orEmpty(), unit)
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier, emphasized: Boolean = false) {
    if (value.isBlank()) return
    Surface(
        modifier = modifier.height(108.dp),
        shape = Shapes.medium,
        color = if (emphasized) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(
            1.dp,
            if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
        ),
    ) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AprCard(card: LoanCard) {
    if (card.apr.isBlank() && card.aprCalculation.isBlank()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = Shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.cost_details), style = MaterialTheme.typography.titleLarge)
            if (card.apr.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.apr), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(card.apr, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (card.aprCalculation.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Text(
                    card.aprCalculation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LenderDetails(card: LoanCard) {
    val values = lenderValues(card)
    if (values.none { it.value.isNotBlank() }) return
    var expanded by rememberSaveable(card.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.about_lender), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    stringResource(if (expanded) R.string.collapse else R.string.expand),
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)) {
                    HorizontalDivider(Modifier.padding(bottom = Spacing.sm), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Contacts(values)
                }
            }
        }
    }
}

private data class LenderValue(val label: Int, val value: String, val uri: String?)

private fun lenderValues(card: LoanCard) = listOf(
    LenderValue(R.string.website, card.website, card.website.takeIf(String::isNotBlank)?.let { if (it.startsWith("http")) it else "https://$it" }),
    LenderValue(R.string.phone, card.phone, card.phone.takeIf(String::isNotBlank)?.let { "tel:$it" }),
    LenderValue(R.string.email, card.email, card.email.takeIf(String::isNotBlank)?.let { "mailto:$it" }),
    LenderValue(R.string.address, card.address, null),
    LenderValue(R.string.legal_entity, card.legalEntity, null),
    LenderValue(R.string.license, card.license, null),
)

@Composable
private fun Contacts(values: List<LenderValue>) {
    val access = LocalDetailOfferAccess.current
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        values.filter { it.value.isNotBlank() }.forEach { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (item.uri != null && (item.label != R.string.website || access.allowed)) Modifier.clickable { if (item.label == R.string.website) access.open() else runCatching { uriHandler.openUri(item.uri) } } else Modifier)
                    .padding(vertical = Spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(stringResource(item.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    item.value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.uri != null && (item.label != R.string.website || access.allowed)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PremiumDetail(card: LoanCard, offline: Boolean, listState: LazyListState, saved: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp), color = MaterialTheme.colorScheme.primary) {
                Column(Modifier.fillMaxWidth().brandGradient().statusBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                        IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            stringResource(if (saved) R.string.saved else R.string.save), tint = Color.White) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                            OfferLogo(card, Modifier.padding(6.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(card.title, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(" %.1f".format(card.rating), fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.available_up_to).uppercase(), fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val narrow = maxWidth < 320.dp
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(rangeText(card.repeatCreditRange.takeUnless { it.isEmpty } ?: card.firstCreditRange,
                                stringResource(R.string.unit_currency), card.repeatCredit.ifBlank { card.firstCredit }).ifBlank { "—" },
                                modifier = Modifier.weight(1f), fontSize = if (narrow) 22.sp else 24.sp,
                                lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            if (card.decisionTime.isNotBlank()) DecisionPill(card, compact = narrow)
                        }
                    }
                }
            }
        }
        if (offline) item { OfflineBanner(stringResource(R.string.offline_banner), Modifier.padding(horizontal = 16.dp)) }
        item {
            Surface(modifier = Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    PremiumTermsRow(Icons.Default.Payments, R.string.first_credit,
                        rangeText(card.firstCreditRange, stringResource(R.string.unit_currency), card.firstCredit),
                        Icons.Default.Sync, R.string.repeat_credit,
                        rangeText(card.repeatCreditRange, stringResource(R.string.unit_currency), card.repeatCredit))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PremiumTermsRow(Icons.Default.Percent, R.string.rate, card.rate,
                        Icons.Default.CalendarMonth, R.string.term, rangeText(card.termRange, stringResource(R.string.unit_days), card.term))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PremiumTermsRow(Icons.Default.Schedule, R.string.decision_time, card.decisionTime,
                        Icons.Default.AccountCircle, R.string.age, rangeText(card.ageRange, stringResource(R.string.unit_years), card.age))
                }
            }
        }
        item {
            Surface(modifier = Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.brandGradient(strength = .18f).padding(16.dp)) {
                    Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.premium_company), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text(stringResource(R.string.premium_company_hint), fontSize = 12.sp, lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (card.apr.isNotBlank() || card.aprCalculation.isNotBlank()) item { Box(Modifier.padding(horizontal = 16.dp)) { AprCard(card) } }
        item { Box(Modifier.padding(horizontal = 16.dp)) { PremiumLenderSection(card, false) } }
        item { Box(Modifier.padding(horizontal = 16.dp)) { PremiumLenderSection(card, true) } }
        item { Text(stringResource(R.string.legal_disclaimer), fontSize = 10.sp, lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 19.dp)) }
    }
}

@Composable
private fun PremiumTermsRow(icon: ImageVector, label: Int, value: String, secondIcon: ImageVector, secondLabel: Int, secondValue: String) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        PremiumTerm(icon, label, value, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 10.dp).background(MaterialTheme.colorScheme.outlineVariant))
        PremiumTerm(secondIcon, secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun PremiumTerm(icon: ImageVector, label: Int, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 8.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(30.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.brandGradient(strength = .18f), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(label), fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(value.ifBlank { "—" }, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumLenderSection(card: LoanCard, documents: Boolean) {
    val values = lenderValues(card).filter { if (documents) it.label == R.string.legal_entity || it.label == R.string.license
        else it.label != R.string.legal_entity && it.label != R.string.license }
    var expanded by rememberSaveable(card.id, documents) { mutableStateOf(!documents) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (documents) R.string.premium_documents else R.string.premium_contacts),
                    modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    stringResource(if (expanded) R.string.collapse else R.string.expand))
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(bottom = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))
                    if (values.any { it.value.isNotBlank() }) PremiumContacts(values)
                    else Text(stringResource(R.string.premium_missing), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PremiumContacts(values: List<LenderValue>) {
    val access = LocalDetailOfferAccess.current
    val uriHandler = LocalUriHandler.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Column {
        values.filter { it.value.isNotBlank() }.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.brandGradient(strength = .18f), contentAlignment = Alignment.Center) {
                        Icon(when(item.label) { R.string.website -> Icons.Default.Language; R.string.phone -> Icons.Default.Call
                            R.string.email -> Icons.Default.MailOutline; else -> Icons.Default.Description }, null,
                            Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(item.label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.value, fontSize = 13.sp, lineHeight = 17.sp)
                }
                if (item.uri != null && (item.label != R.string.website || access.allowed)) IconButton(onClick = { if (item.label == R.string.website) access.open() else runCatching { uriHandler.openUri(item.uri) } }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.OpenInNew, stringResource(R.string.premium_open), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(item.value)) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, stringResource(R.string.premium_copy), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
