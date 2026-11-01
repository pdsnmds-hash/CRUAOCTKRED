package com.cruaoctkred.app.ui.feedactions.dropdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruaoctkred.app.domain.model.LoanCard

private val NUMBER_RE = Regex("\\d+[.,]?\\d*")

private fun numbersIn(text: String): List<Double> =
    NUMBER_RE.findAll(text.replace(" ", "").replace(" ", ""))
        .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }
        .toList()

/** Upper bound of the first loan, preferring the admin's numeric range over the display text. */
private fun cardAmount(card: LoanCard): Double? =
    card.firstCreditRange.to?.toDouble()
        ?: card.firstCreditRange.from?.toDouble()
        ?: numbersIn(card.firstCredit).maxOrNull()

private fun cardRate(card: LoanCard): Double? = numbersIn(card.rate).maxOrNull()

enum class OfferSort { Default, AmountDesc, AmountAsc, RateAsc, RatingDesc }

/** null keeps the order the admin set, which is what [OfferSort.Default] means. */
fun sortComparator(sort: OfferSort): Comparator<LoanCard>? = when (sort) {
    OfferSort.Default -> null
    OfferSort.AmountDesc -> compareByDescending { cardAmount(it) ?: Double.NEGATIVE_INFINITY }
    OfferSort.AmountAsc -> compareBy { cardAmount(it) ?: Double.POSITIVE_INFINITY }
    OfferSort.RateAsc -> compareBy { cardRate(it) ?: Double.POSITIVE_INFINITY }
    OfferSort.RatingDesc -> compareByDescending { it.rating }
}

private fun sortLabel(sort: OfferSort): Int = when (sort) {
    OfferSort.Default -> R.string.sort_default
    OfferSort.AmountDesc -> R.string.sort_amount_desc
    OfferSort.AmountAsc -> R.string.sort_amount_asc
    OfferSort.RateAsc -> R.string.sort_rate_asc
    OfferSort.RatingDesc -> R.string.sort_rating_desc
}

/**
 * Two equal-width buttons above the offers list. Each opens its panel as a dropdown anchored
 * to itself, so the feed never shifts. The left one sorts; the right one is a slot the product
 * fills — keeping this module free of any feature dependency.
 *
 * While a non-default sort is active the left button is highlighted and carries a clear
 * affordance, so the order can be reset without opening the panel.
 *
 * @param secondaryContent panel shown under the right button; omit [secondaryLabel] to hide it.
 */
@Composable
fun FeedActions(
    sort: OfferSort,
    onSortChange: (OfferSort) -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    secondaryIcon: ImageVector? = null,
    secondaryContent: @Composable () -> Unit = {},
) {
    var sortOpen by remember { mutableStateOf(false) }
    var secondaryOpen by remember { mutableStateOf(false) }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val panelWidth = (screenWidth - 24).coerceAtMost(360).dp

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) {
            ActionButton(
                label = stringResource(R.string.sort_button),
                icon = Icons.Default.SwapVert,
                highlighted = sort != OfferSort.Default,
                onClick = { sortOpen = true },
                onClear = { onSortChange(OfferSort.Default) }.takeIf { sort != OfferSort.Default },
            )
            DropdownMenu(
                expanded = sortOpen,
                onDismissRequest = { sortOpen = false },
                modifier = Modifier.width(panelWidth),
            ) {
                OfferSort.entries.forEach { option ->
                    SortOption(
                        label = stringResource(sortLabel(option)),
                        selected = option == sort,
                        onClick = { onSortChange(option); sortOpen = false },
                    )
                }
            }
        }
        if (secondaryLabel != null && secondaryIcon != null) {
            Box(Modifier.weight(1f)) {
                ActionButton(
                    label = secondaryLabel,
                    icon = secondaryIcon,
                    highlighted = false,
                    onClick = { secondaryOpen = true },
                )
                DropdownMenu(
                    expanded = secondaryOpen,
                    onDismissRequest = { secondaryOpen = false },
                    modifier = Modifier.width(panelWidth),
                ) {
                    secondaryContent()
                }
            }
        }
    }
}

@Composable
private fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            },
            contentColor = if (highlighted) Color.White else MaterialTheme.colorScheme.secondary,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClear != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.sort_reset),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
            )
        }
    }
}
