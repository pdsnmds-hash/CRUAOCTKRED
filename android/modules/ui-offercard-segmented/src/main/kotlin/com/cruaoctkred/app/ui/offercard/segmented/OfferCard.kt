package com.cruaoctkred.app.ui.offercard.segmented

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.annotation.DrawableRes
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cruaoctkred.app.domain.model.LoanCard

@Immutable
data class OfferCardColors(
    val surface: Color,
    val outline: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val accentFaint: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val ink: Color,
    val inkMuted: Color,
    val star: Color,
    val logoBackdrop: Color,
)

@Immutable
data class OfferCardIcons(
    @DrawableRes val term: Int,
    @DrawableRes val age: Int,
    @DrawableRes val decisionTime: Int,
)

object OfferCardDefaults {

    /**
     * Every tint is derived from [accent] unless overridden, so a product normally only
     * supplies its brand colour. The soft/faint/outline steps are alpha variants of the
     * accent, which land on the right tone over the white card surface for any hue.
     */
    @Composable
    fun colors(
        accent: Color = MaterialTheme.colorScheme.primary,
        accentDeep: Color = MaterialTheme.colorScheme.secondary,
        surface: Color = Color.White,
        outline: Color = accent.copy(alpha = 0.35f),
        accentSoft: Color = accent.copy(alpha = 0.16f),
        accentFaint: Color = accent.copy(alpha = 0.08f),
        gradientStart: Color = lerp(accent, Color.Black, 0.12f),
        gradientEnd: Color = accent,
        ink: Color = Color(0xFF0F172A),
        inkMuted: Color = Color(0xFF64748B),
        star: Color = Color(0xFFF59E0B),
        logoBackdrop: Color = accent.copy(alpha = 0.06f),
    ) = OfferCardColors(
        surface = surface,
        outline = outline,
        accent = accent,
        accentDeep = accentDeep,
        accentSoft = accentSoft,
        accentFaint = accentFaint,
        gradientStart = gradientStart,
        gradientEnd = gradientEnd,
        ink = ink,
        inkMuted = inkMuted,
        star = star,
        logoBackdrop = logoBackdrop,
    )

    /** Line-art set shipped with the module; pass your own ids to swap the whole row. */
    val icons = OfferCardIcons(
        term = R.drawable.calendar,
        age = R.drawable.age,
        decisionTime = R.drawable.time,
    )
}

/** Premium Compact 03 layout, adapted to the product's existing offer-card contract. */
@Composable
fun AppOfferCard(
    canOpenOffer: Boolean = false,
    card: LoanCard,
    saved: Boolean,
    compared: Boolean,
    onOpenDetail: () -> Unit,
    onSave: () -> Unit,
    onCompare: () -> Unit,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    colors: OfferCardColors = OfferCardDefaults.colors(),
    icons: OfferCardIcons = OfferCardDefaults.icons,
    logoContent: (@Composable () -> Unit)? = null,
    amountText: String = card.firstCredit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 330.dp
        val actionSize = if (compact) 36.dp else 40.dp
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp),
                        color = colors.logoBackdrop) {
                        Box(contentAlignment = Alignment.Center) {
                            if (logoContent != null) logoContent()
                            else if (!card.logoUrl.isNullOrBlank()) {
                                AsyncImage(model = card.logoUrl, contentDescription = card.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(48.dp).padding(4.dp))
                            } else {
                                Text(card.title.take(1).uppercase(), fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold, color = colors.accentDeep)
                            }
                        }
                    }
                    Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card.title, color = colors.ink,
                            fontSize = if (compact) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.first_credit), color = colors.inkMuted,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("★ %.1f".format(card.rating), fontSize = if (compact) 12.sp else 13.sp,
                        color = colors.accentDeep, maxLines = 1)
                    IconButton(onClick = onCompare, modifier = Modifier.size(actionSize)) {
                        Icon(if (compared) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = stringResource(if (compared) R.string.remove_compare else R.string.compare),
                            tint = if (compared) colors.accentDeep else colors.ink,
                            modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onSave, modifier = Modifier.size(actionSize)) {
                        Icon(if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(if (saved) R.string.remove_saved else R.string.save),
                            tint = if (saved) colors.accentDeep else colors.ink,
                            modifier = Modifier.size(22.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (compact) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Metric(stringResource(R.string.amount), amountText, colors, true, valueFontSize = 16.sp, fitValue = true)
                                Metric(stringResource(R.string.offer_daily_rate), card.rate, colors, true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OfferCondition(icons.term, stringResource(R.string.term), card.term,
                                    colors, Modifier.weight(1f))
                                OfferCondition(icons.decisionTime, stringResource(R.string.decision_time),
                                    card.decisionTime, colors, Modifier.weight(1f))
                            }
                        }
                    } else {
                        val amountShare = 0.6f
                        val dividerColor = colors.accentDeep.copy(alpha = 0.40f)
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Metric(stringResource(R.string.amount), amountText,
                                    colors, false, Modifier.weight(amountShare).padding(end = 8.dp), valueFontSize = 20.sp, fitValue = true)
                                Metric(stringResource(R.string.offer_daily_rate), card.rate,
                                    colors, false, Modifier.weight(1f - amountShare).padding(start = 8.dp))
                            }
                            HorizontalDivider(thickness = 1.dp, color = dividerColor)
                            Row(Modifier.fillMaxWidth().drawBehind {
                                drawLine(dividerColor, Offset(size.width * amountShare, 0f),
                                    Offset(size.width * amountShare, size.height), strokeWidth = 1.dp.toPx())
                            }.padding(vertical = 6.dp)) {
                                OfferCondition(icons.term, stringResource(R.string.term), card.term,
                                    colors, Modifier.weight(amountShare).padding(end = 8.dp))
                                OfferCondition(icons.decisionTime, stringResource(R.string.decision_time),
                                    card.decisionTime, colors, Modifier.weight(1f - amountShare).padding(start = 8.dp))
                            }
                        }
                    }
                    val label = stringResource(if (canOpenOffer) R.string.get_money else R.string.details)
                    Button(
                        onClick = onCta,
                        modifier = Modifier.width(if (compact) 96.dp else 106.dp).heightIn(min = 86.dp).brandGradient(RoundedCornerShape(17.dp))
                            .semantics { contentDescription = label.replace("\n", " ") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(17.dp),
                        border = BorderStroke(1.dp, colors.ink.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
                            contentColor = colors.ink),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(23.dp), tint = Color.White)
                            Text(label.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                lineHeight = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, colors: OfferCardColors,
    compact: Boolean, modifier: Modifier = Modifier,
    valueFontSize: TextUnit = if (compact) 14.sp else 16.sp,
    fitValue: Boolean = false,
) {
    Column(modifier) {
        if (fitValue) BasicText(value.ifBlank { "—" },
            style = TextStyle(fontSize = valueFontSize, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = colors.ink),
            maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = valueFontSize, stepSize = 0.5.sp))
        else Text(value.ifBlank { "—" }, fontSize = valueFontSize, lineHeight = 24.sp,
            fontWeight = FontWeight.Bold, color = colors.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 11.sp, color = colors.inkMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OfferCondition(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    colors: OfferCardColors,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), contentDescription = label,
            tint = colors.accentDeep, modifier = Modifier.size(18.dp))
        Text(value.ifBlank { "—" }, Modifier.weight(1f),
            color = colors.inkMuted, fontSize = 11.sp, lineHeight = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
