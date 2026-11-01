package com.cruaoctkred.app.ui.component

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruaoctkred.app.R
import com.cruaoctkred.app.ui.filter.OfferCriteria
import com.cruaoctkred.app.ui.filter.OfferFilterLimits
import java.text.NumberFormat
import kotlin.math.roundToLong
import kotlin.math.roundToInt

@Composable
fun OfferFilterDialog(
    criteria: OfferCriteria,
    limits: OfferFilterLimits,
    onApply: (OfferCriteria) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountEnabled by rememberSaveable { mutableStateOf(criteria.amount != null) }
    var daysEnabled by rememberSaveable { mutableStateOf(criteria.days != null) }
    var rateEnabled by rememberSaveable { mutableStateOf(criteria.maxRate != null) }
    var amount by rememberSaveable { mutableStateOf((criteria.amount ?: 10_000L).coerceIn(limits.amountMin, limits.amountMax)) }
    var days by rememberSaveable { mutableStateOf((criteria.days ?: 30L).coerceIn(1L, limits.daysMax)) }
    var rate by rememberSaveable { mutableStateOf((criteria.maxRate ?: limits.rateMax).coerceIn(0.0, limits.rateMax)) }
    val locale = LocalConfiguration.current.locales[0]
    val integers = NumberFormat.getIntegerInstance(locale)
    val decimals = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }

    AlertDialog(
        modifier = Modifier.brandGradient(androidx.compose.foundation.shape.RoundedCornerShape(28.dp), .1f),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.offer_filters), fontSize = 18.sp, lineHeight = 22.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterSlider(stringResource(R.string.filter_amount),
                    integers.format(amount) + " " + stringResource(R.string.unit_currency),
                    amountEnabled, { amountEnabled = it }, amount.toFloat(),
                    limits.amountMin.toFloat()..limits.amountMax.toFloat()) {
                    amount = ((it / 500f).roundToLong() * 500L).coerceIn(limits.amountMin, limits.amountMax)
                    amountEnabled = true
                }
                FilterSlider(stringResource(R.string.filter_days),
                    integers.format(days) + " " + stringResource(R.string.unit_days),
                    daysEnabled, { daysEnabled = it }, days.toFloat(), 1f..limits.daysMax.toFloat()) {
                    days = it.roundToLong().coerceIn(1L, limits.daysMax)
                    daysEnabled = true
                }
                FilterSlider(stringResource(R.string.filter_max_rate), decimals.format(rate) + " %",
                    rateEnabled, { rateEnabled = it }, rate.toFloat(), 0f..limits.rateMax.toFloat()) {
                    rate = (it * 100f).roundToInt() / 100.0
                    rateEnabled = true
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(OfferCriteria(
                amount.takeIf { amountEnabled }, days.takeIf { daysEnabled }, rate.takeIf { rateEnabled },
            )) }) { Text(stringResource(R.string.filter_apply)) }
        },
        dismissButton = { TextButton(onClick = onReset) { Text(stringResource(R.string.filter_reset)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSlider(
    label: String,
    valueText: String,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = if (active) scheme.primary else scheme.onSurfaceVariant,
        activeTrackColor = if (active) scheme.primary else scheme.outline,
        inactiveTrackColor = scheme.primaryContainer,
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column {
            Row(Modifier.fillMaxWidth().height(24.dp).clickable(role = Role.Checkbox) { onActiveChange(!active) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp)
                Text(if (active) valueText else stringResource(R.string.filter_unrestricted),
                    fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                Checkbox(active, onCheckedChange = null, modifier = Modifier.size(18.dp))
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = range,
                modifier = Modifier.fillMaxWidth().height(28.dp).semantics { contentDescription = label },
                colors = sliderColors,
                interactionSource = interaction,
                thumb = { SliderDefaults.Thumb(interactionSource = interaction, colors = sliderColors,
                    thumbSize = DpSize(12.dp, 24.dp)) })
        }
    }
}
