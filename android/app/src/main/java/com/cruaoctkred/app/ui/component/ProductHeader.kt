package com.cruaoctkred.app.ui.component

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruaoctkred.app.ui.filter.OfferFilterLimits
import com.cruaoctkred.app.ui.filter.OfferCriteria
import com.cruaoctkred.app.R
import com.cruaoctkred.app.ui.theme.BrandBackground
import com.cruaoctkred.app.ui.theme.BrandPrimary
import com.cruaoctkred.app.ui.theme.BrandOnSurface
import com.cruaoctkred.app.ui.theme.BrandSplash
import com.cruaoctkred.app.ui.theme.BrandSecondary
import com.cruaoctkred.app.ui.header.fintech_2.HeaderSearch
import com.cruaoctkred.app.ui.header.fintech_2.HeaderFilters
import com.cruaoctkred.app.ui.header.fintech_2.HeaderNotification
import com.cruaoctkred.app.ui.header.fintech_2.HeaderScrollState
import com.cruaoctkred.app.ui.header.fintech_2.AppHeader

@Composable
fun ProductHeader(
    search: HeaderSearch?,
    filters: HeaderFilters?,
    criteria: OfferCriteria,
    filterLimits: OfferFilterLimits,
    onCriteriaChange: (OfferCriteria) -> Unit,
    notification: HeaderNotification,
    scrollState: HeaderScrollState,
    currentLanguage: String,
    onToggleLanguage: () -> Unit,
    offerCount: Int?,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val configuredSearch = if (search != null && filters != null) {
        search.copy(
            settingsDescription = if (criteria.activeCount == 0) stringResource(R.string.offer_filters)
                else stringResource(R.string.filter_active_description, criteria.activeCount),
            onSettingsClick = {
                filtersOpen = true
            },
        )
    } else search

    AppHeader(
        title = stringResource(R.string.app_name),
        titleStyle = TextStyle(color = Color.White, fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold),
        titleMaxLines = 1,
        gradient = listOf(BrandSecondary, BrandPrimary),
        modifier = modifier,
        actions = {
            val targetLanguage = stringResource(if (currentLanguage == "uk") R.string.language_russian else R.string.language_ukrainian)
            val currentLanguageDescription = stringResource(
                if (currentLanguage == "uk") R.string.current_language_ukrainian else R.string.current_language_russian,
            )
            val shape = RoundedCornerShape(9.6.dp)
            val actionModifier = Modifier.size(38.4.dp).clip(shape)
                .background(Color.White.copy(alpha = 0.7f))
                .border(1.dp, BrandOnSurface.copy(alpha = 0.75f), shape)
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.4.dp)) {
                    IconButton(onClick = notification.onClick, modifier = actionModifier) {
                        Icon(Icons.Outlined.Notifications, notification.description,
                            tint = BrandOnSurface, modifier = Modifier.size(19.2.dp))
                    }
                    IconButton(
                        onClick = onToggleLanguage,
                        modifier = actionModifier.semantics {
                            contentDescription = currentLanguageDescription
                            onClick(label = targetLanguage, action = null)
                        },
                    ) {
                        Text(if (currentLanguage == "uk") "UA" else "RU", fontSize = 9.6.sp, color = BrandOnSurface)
                    }
                }
            }
        },
        bottomContent = {
            Column(Modifier.fillMaxWidth().background(BrandBackground)) {
                if (filters != null) CompactHeaderFilters(filters)
                offerCount?.let {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AvailabilityDot(it > 0)
                            Text(stringResource(R.string.offers_count, it), color = BrandOnSurface,
                                fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        if (criteria.activeCount > 0) {
                            Text(stringResource(R.string.filter_active_count, criteria.activeCount),
                                Modifier.clickable(role = Role.Button) { filtersOpen = true },
                                color = BrandOnSurface, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        },
        search = configuredSearch,
        accent = BrandPrimary,
        scrollState = scrollState,
    )

    if (filtersOpen && search != null) {
        OfferFilterDialog(
            criteria = criteria,
            limits = filterLimits,
            onApply = { onCriteriaChange(it); filtersOpen = false },
            onReset = { onCriteriaChange(OfferCriteria()); filtersOpen = false },
            onDismiss = { filtersOpen = false },
        )
    }
}

// The module's filters have fixed 88dp tiles. The product's bottom slot hosts
// this compact presentation while keeping the same controlled selection state.
@Composable
private fun CompactHeaderFilters(filters: HeaderFilters) {
    val compact = LocalConfiguration.current.screenWidthDp < 360
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.items.forEach { filter ->
            val active = filter.id == filters.selectedId
            Column(
                Modifier.weight(when (filter.id) {
                    "all" -> 0.65f
                    "new" -> 0.9f
                    "best_rate" -> 1.5f
                    else -> 1f
                }).height(56.dp).clip(RoundedCornerShape(12.dp))
                    .then(if (active) Modifier.brandGradient() else Modifier.background(Color.White))
                    .border(1.dp, BrandOnSurface.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .semantics { selected = active }
                    .clickable(role = Role.Tab) { filters.onSelect(filter.id) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                if (filter.id == "all") {
                    Text(filter.symbol, color = if (active) Color.White else Color.Black,
                        fontSize = 18.sp, lineHeight = 22.sp)
                } else {
                    val icon = when (filter.id) {
                        "hot" -> R.drawable.ic_category_hot
                        "new" -> R.drawable.ic_category_new
                        else -> R.drawable.ic_category_best_rate
                    }
                    Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Text(filter.label, color = if (active) Color.White else Color.Black,
                    fontSize = if (compact && filter.id == "best_rate") 9.sp else 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AvailabilityDot(available: Boolean) {
    val transition = rememberInfiniteTransition(label = "offer availability")
    val pulse by transition.animateFloat(initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "availability pulse")
    val description = stringResource(if (available) R.string.offers_available else R.string.empty_title)
    Box(Modifier.size(6.dp).graphicsLayer { alpha = if (available) pulse else 1f }
        .background(if (available) Color(0xFF22C55E) else BrandOnSurface.copy(alpha = 0.3f), CircleShape)
        .semantics { contentDescription = description })
}
