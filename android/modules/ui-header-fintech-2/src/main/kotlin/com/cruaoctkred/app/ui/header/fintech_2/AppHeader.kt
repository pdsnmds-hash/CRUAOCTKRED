package com.cruaoctkred.app.ui.header.fintech_2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.TimeZone
import kotlin.random.Random

/** Product-owned slider state, limits and already formatted copy. */
data class HeaderSlider(
    val label: String,
    val valueText: String,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val minText: String,
    val maxText: String,
    val onValueChange: (Float) -> Unit,
)

/** Attach [nestedScrollConnection] to the application's scrolling content or its parent. */
@Stable
class HeaderScrollState {
    var collapsedPixels by mutableFloatStateOf(0f)
        private set
    var controlsHeightPixels by mutableFloatStateOf(0f)
        private set
    var titleHeightPixels by mutableFloatStateOf(0f)
        private set

    internal fun updateControlsHeight(height: Float) { controlsHeightPixels = height }
    internal fun updateTitleHeight(height: Float) { titleHeightPixels = height }

    private fun consume(delta: Float): Float {
        val before = collapsedPixels
        collapsedPixels = (before - delta).coerceIn(0f, controlsHeightPixels + titleHeightPixels)
        return before - collapsedPixels
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

        // Expand only after the scrolling child reaches its top.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero
    }
}

@Composable
fun rememberHeaderScrollState(): HeaderScrollState = remember { HeaderScrollState() }

/** Measures full content, but exposes only its remaining height to the parent. */
private fun Modifier.collapseHeight(hidden: Float, onFullHeight: (Float) -> Unit): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val content = measurable.measure(constraints.copy(minHeight = 0))
        val visibleHeight = (content.height - hidden.toInt()).coerceAtLeast(0)
        layout(content.width, visibleHeight) { content.placeRelative(0, 0) }
    }.onSizeChanged { onFullHeight(it.height.toFloat()) }

/** Controlled search: the product owns the query and applies it to its offers. */
data class HeaderSearch(
    val query: String,
    val placeholder: String,
    val onQueryChange: (String) -> Unit,
    val settingsDescription: String? = null,
    val onSettingsClick: (() -> Unit)? = null,
)

/** Labels and symbols are ready-to-display product copy; selection stays in the product. */
data class HeaderFilter(val id: String, val label: String, val symbol: String)
data class HeaderFilters(
    val items: List<HeaderFilter>,
    val selectedId: String?,
    val onSelect: (String) -> Unit,
)
data class HeaderNotification(val description: String, val onClick: () -> Unit)

private val Ink = Color(0xFF102C30)
private val MutedInk = Color(0xFF516B73)

/** Soft finance header. No product theme, resources or offer filtering logic are read here. */
@Composable
fun AppHeader(
    title: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    badge: String? = null,
    trustLine: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    titleMaxLines: Int = 1,
    titleStyle: TextStyle? = null,
    bottomContent: @Composable () -> Unit = {},
    amountSlider: HeaderSlider? = null,
    termSlider: HeaderSlider? = null,
    buttonText: String? = null,
    onShowOffers: () -> Unit = {},
    accent: Color = Color(0xFF15945B),
    scrollState: HeaderScrollState? = null,
    search: HeaderSearch? = null,
    filters: HeaderFilters? = null,
    notification: HeaderNotification? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val medium = maxWidth < 400.dp
        val inset = if (compact) 16.dp else 20.dp
        val controlsHidden = scrollState?.collapsedPixels ?: 0f
        val titleHidden = (controlsHidden - (scrollState?.controlsHeightPixels ?: 0f)).coerceAtLeast(0f)
        // A single supplied colour is a valid solid header background.
        val colors = gradient.ifEmpty { listOf(Color.White) }.let { if (it.size == 1) it + it else it }
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().collapseHeight(titleHidden) { scrollState?.updateTitleHeight(it) }
                    .padding(horizontal = inset, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = titleStyle?.color?.takeUnless { it == Color.Unspecified } ?: Ink, style = titleStyle ?: TextStyle(
                            fontSize = (if (compact) 23 else 25).sp, lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold), maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
                        trustLine?.let { Text(it, Modifier.padding(top = 6.dp), color = MutedInk, fontSize = 15.sp, lineHeight = 21.sp) }
                        badge?.let { Text(it, Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = .7f)).padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Ink, fontSize = 12.sp) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        notification?.let { notice ->
                            IconButton(onClick = notice.onClick, modifier = Modifier.clip(CircleShape)
                                .background(Color.White.copy(alpha = .7f))) {
                                Icon(Icons.Outlined.Notifications, notice.description, tint = Ink)
                            }
                        }
                        actions?.invoke(this)
                    }
                }
                Column(Modifier.fillMaxWidth().collapseHeight(controlsHidden) { scrollState?.updateControlsHeight(it) }) {
                    search?.let { field ->
                        Row(Modifier.padding(horizontal = inset).fillMaxWidth().heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Search, null, tint = Ink)
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(field.query, field.onQueryChange, singleLine = true,
                                textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                                modifier = Modifier.weight(1f).semantics { contentDescription = field.placeholder },
                                decorationBox = { input ->
                                    Box {
                                        if (field.query.isEmpty()) Text(field.placeholder, color = MutedInk, fontSize = 13.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        input()
                                    }
                                })
                            if (field.onSettingsClick != null && field.settingsDescription != null) {
                                IconButton(onClick = field.onSettingsClick) { Icon(Icons.Outlined.Tune, field.settingsDescription, tint = Ink) }
                            } else Spacer(Modifier.width(12.dp))
                        }
                    }
                    if (amountSlider != null || termSlider != null) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            amountSlider?.let { Box(Modifier.weight(1f)) { SliderBlock(it, accent, compact, medium) } }
                            termSlider?.let { Box(Modifier.weight(1f)) { SliderBlock(it, accent, compact, medium) } }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.fillMaxWidth().background(Color.White)) {
                        filters?.let { group ->
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = inset, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                group.items.forEach { filter ->
                                    val active = filter.id == group.selectedId
                                    Column(Modifier.width(88.dp).heightIn(min = 88.dp).clip(RoundedCornerShape(18.dp))
                                        .background(if (active) accent else accent.copy(alpha = .06f))
                                        .semantics { selected = active }
                                        .clickable(role = androidx.compose.ui.semantics.Role.Tab) { group.onSelect(filter.id) }
                                        .padding(horizontal = 4.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(filter.symbol, color = if (active) Color.White else accent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                        Text(filter.label, color = if (active) Color.White else Ink, fontSize = 12.sp, lineHeight = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        }
                        // Optional calculator CTA retained for existing product wiring.
                        Column(Modifier.fillMaxWidth().padding(horizontal = inset)) {
                                buttonText?.let {
                                    Button(onClick = onShowOffers, modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)) {
                                        Text(it, Modifier.weight(1f), fontSize = 13.sp)
                                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                                    }
                                }
                        }
                    }
                }
            }
            bottomContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderBlock(slider: HeaderSlider, accent: Color, compact: Boolean, medium: Boolean) {
    Column {
        Text(slider.label, color = MutedInk, fontSize = (if (compact) 10 else 12).sp, lineHeight = if (compact) 14.sp else if (medium) 16.sp else TextUnit.Unspecified)
        Text(slider.valueText, color = Ink, fontSize = (if (compact) 16 else 18).sp, lineHeight = if (compact) 20.sp else if (medium) 22.sp else TextUnit.Unspecified, fontWeight = FontWeight.Bold)
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides if (compact) 22.dp else if (medium) 24.dp else 28.dp) {
            Slider(
                value = slider.value,
                onValueChange = slider.onValueChange,
                valueRange = slider.range,
                modifier = Modifier.fillMaxWidth().height(if (compact) 22.dp else if (medium) 24.dp else 28.dp),
                thumb = { Box(Modifier.size(if (compact) 10.dp else 12.dp).background(Color.White, CircleShape)) },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.height(4.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = accent,
                            inactiveTrackColor = accent.copy(alpha = .18f),
                        ),
                        thumbTrackGapSize = 3.dp,
                        drawStopIndicator = null,
                    )
                },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(slider.minText, color = MutedInk, fontSize = (if (compact) 9 else 10).sp, lineHeight = if (compact) 12.sp else if (medium) 14.sp else TextUnit.Unspecified)
            Text(slider.maxText, color = MutedInk, fontSize = (if (compact) 9 else 10).sp, lineHeight = if (compact) 12.sp else if (medium) 14.sp else TextUnit.Unspecified)
        }
    }
}

/** Generated daily number; retained for existing callers, not a measured statistic. */
@Composable
fun rememberDailyNumber(min: Int, max: Int): Int = remember(min, max) {
    require(min <= max) { "min ($min) must not exceed max ($max)" }
    val now = System.currentTimeMillis()
    val localDay = (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    Random(localDay).nextInt(min, max + 1)
}

/** Pinned action row. The caller supplies copy, colours and application actions/menus. */
@Composable
fun HeaderFeedActions(
    countText: String,
    sortText: String,
    languageText: String,
    calculatorDescription: String,
    languageDescription: String,
    onSort: () -> Unit,
    onCalculator: () -> Unit,
    onLanguage: () -> Unit,
    countBackground: Color,
    buttonBackground: Color,
    activeButtonBackground: Color,
    activeButtonForeground: Color,
    modifier: Modifier = Modifier,
    sortActive: Boolean = false,
    foreground: Color = Color(0xFF67E8BF),
    borderColor: Color = foreground.copy(alpha = .55f),
    indicatorColor: Color = Color(0xFFF59E0B),
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val medium = maxWidth >= 360.dp && maxWidth < 400.dp
        val gap = if (compact || medium) 4.dp else 6.dp
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 4.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = countBackground,
                border = BorderStroke(1.dp, borderColor)) {
                Row(Modifier.padding(horizontal = if (compact) 8.dp else if (medium) 10.dp else 12.dp,
                    vertical = if (compact || medium) 7.dp else 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(if (compact) 6.dp else 9.dp).background(indicatorColor, CircleShape))
                    Text(countText, fontSize = (if (compact) 12 else if (medium) 14 else 16).sp,
                        lineHeight = (if (compact) 16 else if (medium) 18 else 24).sp,
                        fontWeight = FontWeight.Bold, color = foreground, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                HeaderActionChip(sortText, sortText, Icons.Default.SwapVert, onSort,
                    if (sortActive) activeButtonBackground else buttonBackground,
                    if (sortActive) activeButtonForeground else foreground, borderColor, compact, medium)
                HeaderActionChip(null, calculatorDescription, Icons.Default.Calculate, onCalculator,
                    buttonBackground, foreground, borderColor, compact, medium)
                HeaderActionChip(languageText, languageDescription, null, onLanguage,
                    buttonBackground, foreground, borderColor, compact, medium)
            }
        }
    }
}

@Composable
private fun HeaderActionChip(
    label: String?, description: String, icon: ImageVector?, onClick: () -> Unit,
    background: Color, foreground: Color, border: Color, compact: Boolean, medium: Boolean,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides if (compact) 30.dp else if (medium) 32.dp else 48.dp) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.height(if (compact) 30.dp else if (medium) 32.dp else 36.dp)
                .widthIn(min = if (compact || medium) 1.dp else 64.dp)
                .semantics { contentDescription = description },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = if (compact || medium) 8.dp else 10.dp),
            border = BorderStroke(1.dp, border),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = background, contentColor = foreground),
        ) {
            if (icon != null) Icon(icon, contentDescription = null,
                modifier = Modifier.size(if (compact) 14.dp else if (medium) 15.dp else 16.dp))
            if (icon != null && label != null) Spacer(Modifier.width(4.dp))
            if (label != null) Text(label, fontSize = (if (compact) 11 else 12).sp,
                lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
