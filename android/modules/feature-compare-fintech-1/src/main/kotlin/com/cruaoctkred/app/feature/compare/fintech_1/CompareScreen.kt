package com.cruaoctkred.app.feature.compare.fintech_1

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.ui.components.OfferLogo

private val Ink = Color(0xFF151827)
private val Muted = Color(0xFF85879F)
private val Accent = Color(0xFF0095FF)
private val AccentStrong = Color(0xFF0066CC)
private val Tint = Color(0xFFCCE9FF)
private val Line = Color(0xFF99D5FF)
private val Background = Color(0xFFF2FAFF)

/** Offer selection stays with the caller; only the user's calculation inputs are local. */
@Composable
fun CompareScreen(
    cards: List<LoanCard>,
    comparedIds: List<String>,
    onRemove: (String) -> Unit,
    onDetail: (String) -> Unit,
    onCta: (LoanCard) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = comparedIds.mapNotNull { id -> cards.find { it.id == id } }.take(2)
    var requestedAmount by rememberSaveable { mutableLongStateOf(10_000L) }
    var requestedDays by rememberSaveable { mutableLongStateOf(10L) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val medium = maxWidth > 320.dp && maxWidth <= 360.dp && maxHeight >= 650.dp
        val compact = maxWidth <= 360.dp || maxHeight < 650.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 104.dp)) {
            Text(stringResource(R.string.cft1_compare_title), color = Ink, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.cft1_compare_subtitle), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(12.dp))
            if (selected.size < 2) {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) { EmptyComparison(onBrowse) }
            } else {
                val left = selected[0]
                val right = selected[1]
                val amountRange = commonRange(left.firstCreditRange, right.firstCreditRange)
                val daysRange = commonRange(left.termRange, right.termRange)
                val amount = amountRange?.let { requestedAmount.coerceIn(it) }
                val days = daysRange?.let { requestedDays.coerceIn(it) }
                val leftUnit = currencyUnit(left.firstCredit)
                val rightUnit = currencyUnit(right.firstCredit)
                val sameCurrency = leftUnit == rightUnit
                val unit = if (sameCurrency) leftUnit else ""
                val a = if (sameCurrency) calculateComparison(amount, days, left.rate) else null
                val b = if (sameCurrency) calculateComparison(amount, days, right.rate) else null
                val leftBest = a != null && b != null && a.total < b.total
                val rightBest = a != null && b != null && b.total < a.total
                val winner = when {
                    leftBest -> left
                    rightBest -> right
                    else -> null
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScenarioChip(Icons.Default.Payments, stringResource(R.string.cft1_amount), amount, amountRange, unit, false,
                        { requestedAmount = it }, Modifier.weight(1f))
                    ScenarioChip(Icons.Default.CalendarMonth, stringResource(R.string.cft1_days_label), days, daysRange, "", true,
                        { requestedDays = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth()) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        color = Color.White, border = BorderStroke(1.dp, Line)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Box(Modifier.fillMaxWidth().height(if (compact) 48.dp else 56.dp)) {
                                VerticalDivider(Modifier.align(Alignment.Center).fillMaxHeight(), color = Line)
                                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                    OfferSelector(left, leftBest, { onDetail(left.id) },
                                        Modifier.weight(1f).padding(end = 20.dp))
                                    OfferSelector(right, rightBest, { onDetail(right.id) },
                                        Modifier.weight(1f).padding(start = 20.dp))
                                }
                                Surface(shape = CircleShape, color = Tint,
                                    modifier = Modifier.align(Alignment.Center).size(32.dp)) {
                                    Box(Modifier.brandGradient(strength = .2f), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.cft1_vs), color = AccentStrong,
                                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider(color = Line)
                            ComparisonMetric(stringResource(R.string.cft1_rate), left.rate, right.rate, leftBest, rightBest, compact, Modifier.heightIn(min = if (compact) 50.dp else 56.dp))
                            HorizontalDivider(color = Line)
                            ComparisonMetric(stringResource(R.string.cft1_overpayment), money(a?.overpayment, unit), money(b?.overpayment, unit), leftBest, rightBest, compact, Modifier.heightIn(min = if (compact) 50.dp else 56.dp))
                            HorizontalDivider(color = Line)
                            ComparisonMetric(stringResource(R.string.cft1_total), money(a?.total, unit), money(b?.total, unit), leftBest, rightBest, compact, Modifier.heightIn(min = if (compact) 50.dp else 56.dp))
                            Spacer(Modifier.height(8.dp))
                            Surface(color = Tint, shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)) {
                                Row(Modifier.fillMaxWidth().brandGradient(strength = .2f).padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(Icons.AutoMirrored.Filled.CompareArrows, null, Modifier.size(18.dp), tint = AccentStrong)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (a != null && b != null) stringResource(R.string.cft1_difference, money(kotlin.math.abs(a.total - b.total), unit))
                                        else stringResource(R.string.cft1_unavailable),
                                        color = AccentStrong, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    if (a == null || b == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.cft1_missing_note), color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    Button(
                        onClick = onBrowse,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).brandGradient(RoundedCornerShape(12.dp), .12f), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = AccentStrong),
                        border = BorderStroke(1.dp, Accent.copy(alpha = 0.20f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = if (compact) 6.dp else 10.dp),
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.cft1_replace), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                    if (winner != null) {
                        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                        WinnerPreview(winner, onDetail, compact, medium)
                    }
                }

            }
        }
    }
}

@Composable
private fun WinnerPreview(card: LoanCard, onDetail: (String) -> Unit, compact: Boolean, medium: Boolean) {
    Surface(
        onClick = { onDetail(card.id) },
        modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 80.dp else 88.dp),
        color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f)),
    ) {
        Row(Modifier.padding(if (medium) 13.6.dp else if (compact) 10.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Background, shape = RoundedCornerShape(12.dp)) {
                OfferLogo(card, Modifier.size(width = if (medium) 68.dp else if (compact) 64.dp else 64.dp, height = if (medium) 51.dp else if (compact) 48.dp else 48.dp).padding(4.dp))
            }
            Spacer(Modifier.width(if (medium) 12.dp else if (compact) 10.dp else 14.dp))
            Column(Modifier.weight(1f)) {
                Text(card.title, color = AccentStrong, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(if (compact && !medium) R.string.cft1_best_offer_compact else R.string.cft1_best_offer), color = Ink.copy(alpha = 0.8f),
                    fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ScenarioChip(icon: ImageVector, label: String, value: Long?, range: LongRange?, unit: String,
    days: Boolean, onChange: (Long) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    val display = if (days && value != null) stringResource(R.string.cft1_days, value) else money(value, unit)
    BoxWithConstraints(modifier) {
        val narrow = maxWidth < 150.dp
        Surface(Modifier.fillMaxWidth().clickable(enabled = range != null) { expanded = true },
            color = Color.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line)) {
            Row(Modifier.heightIn(min = 48.dp).padding(horizontal = if (narrow) 4.dp else 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(if (narrow) 12.dp else 18.dp), tint = Muted)
                Spacer(Modifier.width(if (narrow) 2.dp else 4.dp))
                Text(
                    buildAnnotatedString {
                        append(label)
                        append(": ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(display) }
                    },
                    modifier = Modifier.weight(1f), color = Ink, fontSize = if (narrow) 11.sp else 12.sp,
                    letterSpacing = 0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ExpandMore, null, Modifier.size(if (narrow) 12.dp else 18.dp), tint = Muted)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
            if (range != null && value != null) {
                scenarioChoices(range, value, days).forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(if (days) stringResource(R.string.cft1_days, choice) else money(choice, unit), color = Ink) },
                        onClick = { onChange(choice); expanded = false },
                    )
                }
                DropdownMenuItem(text = { Text(stringResource(R.string.cft1_custom_value), color = AccentStrong) },
                    onClick = { expanded = false; custom = true })
            }
        }
    }
    if (custom && range != null && value != null) {
        ScenarioInputDialog(label, value, range, { custom = false }, { onChange(it); custom = false })
    }
}

@Composable
private fun ScenarioInputDialog(label: String, current: Long, range: LongRange, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var input by rememberSaveable(current) { mutableStateOf(current.toString()) }
    val parsed = input.toLongOrNull()
    val valid = parsed != null && parsed in range
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color.White,
        title = { Text(label, color = Ink) },
        text = {
            OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !valid,
                supportingText = { Text(stringResource(R.string.cft1_allowed_range, range.first, range.last)) })
        },
        confirmButton = { TextButton(onClick = { if (parsed != null && valid) onConfirm(parsed) }, enabled = valid) {
            Text(stringResource(R.string.cft1_apply), color = if (valid) AccentStrong else Muted)
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cft1_cancel), color = Muted) } },
    )
}

@Composable
private fun OfferSelector(card: LoanCard, best: Boolean, onDetail: () -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxHeight()) {
        val titleSize = if (maxWidth < 140.dp) 14.sp else 16.sp
        Column(
            Modifier.fillMaxSize().clickable(onClick = onDetail).padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(card.title, color = if (best) AccentStrong else Ink, fontSize = titleSize, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ComparisonMetric(label: String, left: String, right: String, leftBest: Boolean, rightBest: Boolean,
    compact: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Ink.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            MetricValue(left, leftBest, compact, Modifier.weight(1f))
            VerticalDivider(Modifier.heightIn(min = 28.dp), color = Line)
            MetricValue(right, rightBest, compact, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricValue(value: String, best: Boolean, compact: Boolean, modifier: Modifier) {
    Text(value.ifBlank { stringResource(R.string.cft1_no_value) }, modifier.padding(horizontal = 2.dp),
        color = if (best) Accent else Ink, fontSize = if (compact) 16.sp else 20.sp, lineHeight = if (compact) 20.sp else 24.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
}

@Composable
private fun EmptyComparison(onBrowse: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.CompareArrows, null, Modifier.size(48.dp), tint = Accent)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.cft1_compare_empty_title), color = Ink, fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.cft1_compare_empty), color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBrowse, modifier = Modifier.brandGradient(androidx.compose.foundation.shape.CircleShape), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)) {
            Text(stringResource(R.string.cft1_browse_offers), fontSize = 14.sp)
        }
    }
}

private fun currencyUnit(value: String): String {
    Regex("""\p{Sc}""").find(value)?.let { return it.value }
    return Regex("""\d\s+([\p{L}]{1,4})\s*$""").find(value)?.groupValues?.get(1).orEmpty()
}

@Composable
private fun money(value: Long?, unit: String): String {
    if (value == null) return stringResource(R.string.cft1_no_value)
    val grouped = value.toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (unit.isBlank()) grouped else "$grouped $unit"
}
