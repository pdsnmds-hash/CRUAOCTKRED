package com.cruaoctkred.app.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cruaoctkred.app.R
import com.cruaoctkred.app.ui.components.brandGradient
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReminderCalendar(value: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val today = remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) } }
    val initial = remember { PaymentReminder("calendar", 1, value, 1).dueCalendar()?.takeIf { it.timeInMillis >= today.timeInMillis } ?: (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) } }
    var chosen by rememberSaveable { mutableLongStateOf(initial.timeInMillis) }
    var year by rememberSaveable { mutableIntStateOf(initial.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableIntStateOf(initial.get(Calendar.MONTH)) }
    val page = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.YEAR, year); set(Calendar.MONTH, month) }
    val firstOffset = (page.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val count = page.getActualMaximum(Calendar.DAY_OF_MONTH)
    val canPrevious = year > today.get(Calendar.YEAR) || month > today.get(Calendar.MONTH)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val inset = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp
    val monthLabel = SimpleDateFormat("LLLL yyyy", locale).format(page.time).replaceFirstChar { it.titlecase(locale) }
    val weekdays = DateFormatSymbols(locale).shortWeekdays
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(horizontal = 12.dp, vertical = 16.dp).widthIn(max = 440.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp), color = Color.White, shadowElevation = 16.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(inset), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.reminder_calendar_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(DateFormat.getDateInstance(DateFormat.LONG, locale).format(java.util.Date(chosen)), fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(monthLabel, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(enabled = canPrevious, onClick = { val previous = (page.clone() as Calendar).apply { add(Calendar.MONTH, -1) }; year = previous.get(Calendar.YEAR); month = previous.get(Calendar.MONTH) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.reminder_previous_month), tint = if (canPrevious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { val next = (page.clone() as Calendar).apply { add(Calendar.MONTH, 1) }; year = next.get(Calendar.YEAR); month = next.get(Calendar.MONTH) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.reminder_next_month), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    for (index in 0..6) Text(weekdays[(index + 1) % 7 + 1].take(2).uppercase(locale), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Six fixed rows keep the calendar steady when moving between months.
                Column {
                for (week in 0..5) Row(Modifier.fillMaxWidth()) {
                    for (weekday in 0..6) {
                        val day = week * 7 + weekday - firstOffset + 1
                        if (day !in 1..count) Box(Modifier.weight(1f).height(44.dp))
                        else {
                            val dayDate = (page.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                            val enabled = dayDate.timeInMillis >= today.timeInMillis
                            val active = chosen == dayDate.timeInMillis
                            val description = DateFormat.getDateInstance(DateFormat.LONG, locale).format(dayDate.time)
                            Box(Modifier.weight(1f).height(44.dp).padding(2.dp).clip(CircleShape)
                                .then(if (active) Modifier.brandGradient(CircleShape) else if (dayDate.timeInMillis == today.timeInMillis) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier)
                                .clickable(enabled = enabled) { chosen = dayDate.timeInMillis }
                                .semantics { contentDescription = description; selected = active }, contentAlignment = Alignment.Center) {
                                Text(day.toString(), fontSize = 18.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) Color.White else if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = .5f))
                            }
                        }
                    }
                }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminder_cancel)) }
                    Button(onClick = { onSelect(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(java.util.Date(chosen))) }, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.brandGradient(RoundedCornerShape(14.dp)), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)) {
                        Text(stringResource(R.string.reminder_calendar_done), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
