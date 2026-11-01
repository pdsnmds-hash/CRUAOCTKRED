package com.cruaoctkred.app.feature.saved.grid

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.ui.components.OfferLogo
import com.cruaoctkred.app.ui.components.Shapes
import com.cruaoctkred.app.ui.components.Spacing

private val TILE_SHAPE = RoundedCornerShape(18.dp)

/**
 * Saved offers as a two-column tile grid: logo-first, one headline amount per tile.
 * Tap opens the detail screen, long press opens a quick-action sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    cards: List<LoanCard>,
    savedIds: Set<String>,
    comparedIds: List<String>,
    onDetail: (String) -> Unit,
    onSave: (String) -> Unit,
    onCompare: (String) -> Unit,
    onCta: (LoanCard) -> Unit,
    modifier: Modifier = Modifier,
    canOpenOffer: (LoanCard) -> Boolean = { false },
) {
    val saved = cards.filter { it.id in savedIds }
    var sheetCardId by remember { mutableStateOf<String?>(null) }
    val sheetCard = saved.firstOrNull { it.id == sheetCardId }

    if (saved.isEmpty()) {
        Column(modifier.fillMaxSize()) {
            Header()
            EmptyState(Modifier.weight(1f).padding(Spacing.xl))
        }
        return
    }

    Column(modifier.fillMaxSize()) {
        Header()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(saved, key = { it.id }) { card ->
                OfferTile(
                    card = card,
                    compared = card.id in comparedIds,
                    onOpen = { onDetail(card.id) },
                    onLongPress = { sheetCardId = card.id },
                    onToggleCompare = { onCompare(card.id) },
                    onUnsave = { onSave(card.id) },
                )
            }
        }
    }

    if (sheetCard != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetCardId = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            QuickActions(
                card = sheetCard,
                canOpenOffer = canOpenOffer(sheetCard),
                compared = sheetCard.id in comparedIds,
                onDetail = { sheetCardId = null; onDetail(sheetCard.id) },
                onCompare = { sheetCardId = null; onCompare(sheetCard.id) },
                onCta = { sheetCardId = null; onCta(sheetCard) },
                onUnsave = { sheetCardId = null; onSave(sheetCard.id) },
            )
        }
    }
}

@Composable
private fun Header() {
    Text(
        stringResource(R.string.saved_title),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OfferTile(
    card: LoanCard,
    compared: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleCompare: () -> Unit,
    onUnsave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(TILE_SHAPE)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        shape = TILE_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleCompare, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (compared) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.CompareArrows,
                        stringResource(if (compared) R.string.remove_compare else R.string.compare),
                        Modifier.size(26.dp),
                        tint = if (compared) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUnsave, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Bookmark,
                        stringResource(R.string.remove_saved),
                        Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OfferLogo(card, Modifier.size(width = 84.dp, height = 38.dp))
            Spacer(Modifier.height(Spacing.xs))

            Text(
                card.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                card.firstCredit.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFB020))
                Spacer(Modifier.width(3.dp))
                Text(
                    "%.1f".format(card.rating),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    canOpenOffer: Boolean,
    card: LoanCard,
    compared: Boolean,
    onDetail: () -> Unit,
    onCompare: () -> Unit,
    onCta: () -> Unit,
    onUnsave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = Spacing.lg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OfferLogo(card, Modifier.size(width = 60.dp, height = 32.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Spacing.xs))

        ActionRow(Icons.Default.Info, stringResource(R.string.details), onDetail)
        ActionRow(
            icon = if (compared) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.CompareArrows,
            label = stringResource(if (compared) R.string.remove_compare else R.string.compare),
            onClick = onCompare,
            tint = if (compared) MaterialTheme.colorScheme.primary else null,
        )
        // Only with a link: the sheet already lists Details, and a menu should not offer the
        // same destination twice. The card's own CTA covers the no-link case.
        if (canOpenOffer) {
            ActionRow(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.get_money), onCta)
        }
        ActionRow(Icons.Default.BookmarkBorder, stringResource(R.string.remove_saved), onUnsave)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color? = null) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.sm))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(Modifier.size(76.dp), shape = Shapes.large, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
            Box(Modifier.brandGradient(strength = .12f), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    null,
                    Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.saved_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
