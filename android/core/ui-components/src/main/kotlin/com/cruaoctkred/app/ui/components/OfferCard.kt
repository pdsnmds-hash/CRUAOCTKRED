package com.cruaoctkred.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cruaoctkred.app.domain.model.LoanCard

@Composable
fun OfferCard(
    canOpenOffer: Boolean = false,
    card: LoanCard,
    saved: Boolean,
    compared: Boolean,
    onOpenDetail: () -> Unit,
    onSave: () -> Unit,
    onCompare: () -> Unit,
    onCta: () -> Unit,
    showRepeatCredit: Boolean = true,
    accentColor: Color = Color(0xFF22E6A8),
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenDetail).semantics {
                    contentDescription = "${card.title}. ${card.rating}"
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OfferLogo(card, Modifier.size(width = 76.dp, height = 48.dp))
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(card.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB020), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xxs))
                        Text("%.1f".format(card.rating), style = MaterialTheme.typography.labelLarge)
                    }
                }
                CompareButton(compared = compared, onClick = onCompare)
                IconButton(onClick = onSave, modifier = Modifier.size(48.dp)) {
                    Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, stringResource(if (saved) R.string.saved else R.string.save))
                }
            }

            if (showRepeatCredit) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    CreditBlock(stringResource(R.string.first_credit), card.firstCredit, Modifier.weight(1f), highlighted = true, accentColor = accentColor)
                    CreditBlock(stringResource(R.string.repeat_credit), card.repeatCredit, Modifier.weight(1f), highlighted = false, accentColor = accentColor)
                }
            } else {
                CreditBlock(
                    stringResource(R.string.first_credit),
                    card.firstCredit,
                    Modifier.fillMaxWidth(),
                    highlighted = true,
                    accentColor = accentColor,
                    centeredValue = true,
                )
            }

            Column {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    TermCell(stringResource(R.string.rate), card.rate, Modifier.weight(1f).fillMaxHeight())
                    Box(Modifier.width(Spacing.xs).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        VerticalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
                    }
                    TermCell(stringResource(R.string.term), card.term, Modifier.weight(1f).fillMaxHeight())
                }
                Box(Modifier.fillMaxWidth().height(Spacing.xs), contentAlignment = Alignment.Center) {
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    TermCell(stringResource(R.string.decision_time), card.decisionTime, Modifier.weight(1f).fillMaxHeight())
                    Box(Modifier.width(Spacing.xs).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        VerticalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
                    }
                    TermCell(stringResource(R.string.age), card.age, Modifier.weight(1f).fillMaxHeight())
                }
            }

            Button(onClick = onCta, modifier = Modifier.fillMaxWidth().height(48.dp).brandGradient(Shapes.small), shape = Shapes.small, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)) {
                Text(
                    stringResource(if (canOpenOffer) R.string.get_money else R.string.details).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun CompareButton(compared: Boolean, onClick: () -> Unit) {
    val actionDescription = stringResource(if (compared) R.string.remove_compare else R.string.compare)
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = actionDescription },
        shape = Shapes.small,
        border = BorderStroke(1.5.dp, if (compared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = if (compared) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (compared) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            if (compared) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.CompareArrows,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun OfferLogo(card: LoanCard, modifier: Modifier = Modifier) {
    var failed by remember(card.logoUrl) { mutableStateOf(false) }
    Box(
        modifier.clip(Shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        if (card.logoUrl != null && !failed) {
            AsyncImage(
                model = card.logoUrl,
                contentDescription = stringResource(R.string.logo_description, card.title),
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            )
        } else {
            Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(card.title.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CreditBlock(
    label: String,
    value: String,
    modifier: Modifier,
    highlighted: Boolean,
    accentColor: Color,
    centeredValue: Boolean = false,
) {
    Box(
        modifier
            .clip(Shapes.small)
            .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        CreditBlockContent(
            label = label,
            value = value,
            highlighted = highlighted,
            accentColor = accentColor,
            centeredValue = false,
            modifier = Modifier
                .padding(Spacing.xs)
                .then(if (centeredValue) Modifier.alpha(0f).clearAndSetSemantics { } else Modifier),
        )
        if (centeredValue) {
            CreditBlockContent(
                label = label,
                value = value,
                highlighted = highlighted,
                accentColor = accentColor,
                centeredValue = true,
                modifier = Modifier.matchParentSize().padding(Spacing.xs),
            )
        }
    }
}

@Composable
private fun CreditBlockContent(
    label: String,
    value: String,
    highlighted: Boolean,
    accentColor: Color,
    centeredValue: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = if (centeredValue) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (centeredValue) Arrangement.Center else Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            label,
            modifier = if (centeredValue) Modifier.fillMaxWidth() else Modifier,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (centeredValue) TextAlign.Center else TextAlign.Start,
        )
        if (centeredValue) Spacer(Modifier.height(2.dp))
        Text(
            value,
            modifier = if (centeredValue) Modifier.fillMaxWidth() else Modifier,
            style = if (centeredValue) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (centeredValue) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = if (centeredValue) 1 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centeredValue) TextAlign.Center else TextAlign.Start,
        )
        if (centeredValue) Spacer(Modifier.height(2.dp))
        if (highlighted) {
            Box(Modifier.fillMaxWidth(.34f).height(3.dp).clip(Shapes.pill).background(accentColor))
        }
    }
}

@Composable
private fun TermCell(label: String, value: String, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
