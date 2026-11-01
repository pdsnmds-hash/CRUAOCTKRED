package com.cruaoctkred.app.feature.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import com.cruaoctkred.app.domain.usecase.LoanCalculator
import com.cruaoctkred.app.ui.components.Shapes
import com.cruaoctkred.app.ui.components.Spacing

data class CalculatorParams(
    val dailyRate: Double,
    val minAmount: Long,
    val maxAmount: Long,
    val amountStep: Long,
    val minTerm: Int,
    val maxTerm: Int,
    val currencyLocale: Locale,
    /** Daily rate bounds in percent, matching LoanCalculator's unit. */
    val minRate: Double = dailyRate,
    val maxRate: Double = dailyRate,
)

@Composable
fun CalculatorLens(
    params: CalculatorParams,
    modifier: Modifier = Modifier,
    initialAmount: Long = DEFAULT_INITIAL_AMOUNT,
) {
    val backgroundColor = MaterialTheme.colorScheme.onBackground
    val contentColor = MaterialTheme.colorScheme.primaryContainer

    var expanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().clip(Shapes.medium).background(backgroundColor).padding(Spacing.md)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.calculator_title), color = contentColor, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.calculator_hint), color = contentColor.copy(alpha = .68f), style = MaterialTheme.typography.labelMedium)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = contentColor)
        }
        AnimatedVisibility(expanded) {
            CalculatorPanel(
                params = params,
                modifier = Modifier.padding(top = Spacing.md),
                contentColor = contentColor,
                initialAmount = initialAmount,
            )
        }
    }
}

/**
 * The bare calculator body: two sliders and the resulting figures, with no chrome of its own.
 * Host it wherever the product wants — inline, in a sheet, on its own screen.
 */
@Composable
fun CalculatorPanel(
    params: CalculatorParams,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    initialAmount: Long = DEFAULT_INITIAL_AMOUNT,
    sliderColors: SliderColors = SliderDefaults.colors(),
) {
    var amount by remember {
        mutableFloatStateOf(initialAmount.toFloat().coerceIn(params.minAmount.toFloat(), params.maxAmount.toFloat()))
    }
    var days by remember { mutableFloatStateOf(params.minTerm.toFloat()) }
    var rate by remember {
        mutableFloatStateOf(params.dailyRate.toFloat().coerceIn(params.minRate.toFloat(), params.maxRate.toFloat()))
    }
    val rateAdjustable = params.maxRate > params.minRate
    val estimate = remember(amount, days, rate) {
        LoanCalculator().calculate(amount.toLong(), rate.toDouble(), days.toInt())
    }
    val formatter = remember(params.currencyLocale) { NumberFormat.getCurrencyInstance(params.currencyLocale) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ValueRow(stringResource(R.string.amount), formatter.format(amount.toLong()), contentColor)
        CompactSlider(
            value = amount,
            onValueChange = { amount = (it / params.amountStep).toInt() * params.amountStep.toFloat() },
            range = params.minAmount.toFloat()..params.maxAmount.toFloat(),
            colors = sliderColors,
        )
        ValueRow(stringResource(R.string.days), days.toInt().toString(), contentColor)
        CompactSlider(
            value = days,
            onValueChange = { days = it.toInt().toFloat() },
            range = params.minTerm.toFloat()..params.maxTerm.toFloat(),
            colors = sliderColors,
        )
        if (rateAdjustable) {
            ValueRow(stringResource(R.string.daily_rate), "%.2f %%".format(rate), contentColor)
            CompactSlider(
                value = rate,
                onValueChange = { rate = (it * 100).toInt() / 100f },
                range = params.minRate.toFloat()..params.maxRate.toFloat(),
                colors = sliderColors,
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Figure(stringResource(R.string.estimated_overpayment), formatter.format(estimate.overpayment), contentColor, Modifier.weight(1f))
            Figure(stringResource(R.string.estimated_total), formatter.format(estimate.total), contentColor, Modifier.weight(1f), emphasized = true)
            Figure(stringResource(R.string.per_day), formatter.format(estimate.perDay), contentColor, Modifier.weight(1f))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.preliminary_calculation),
            color = contentColor.copy(alpha = .68f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    colors: SliderColors,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        colors = colors,
        modifier = Modifier.fillMaxWidth().height(28.dp),
    )
}

@Composable
private fun Figure(
    label: String,
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Column(modifier) {
        Text(label, color = contentColor.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(
            value,
            color = contentColor,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

// Legacy default carried over from the reference product; products pass their own.
private const val DEFAULT_INITIAL_AMOUNT = 5_000_000L

@Composable
private fun ValueRow(label: String, value: String, contentColor: Color, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = contentColor.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = contentColor, style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
