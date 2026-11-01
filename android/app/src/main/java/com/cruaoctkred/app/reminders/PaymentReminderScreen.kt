package com.cruaoctkred.app.reminders

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cruaoctkred.app.R
import com.cruaoctkred.app.ui.components.brandGradient
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
private fun termText(count: Int, unit: ReminderTermUnit): String = pluralStringResource(
    when (unit) { ReminderTermUnit.DAYS -> R.plurals.reminder_days; ReminderTermUnit.MONTHS -> R.plurals.reminder_months; ReminderTermUnit.PAYMENTS -> R.plurals.reminder_payments }, count, count)

@Composable
fun PaymentReminderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val store = remember { PaymentReminderStore(context.applicationContext) }
    var saved by remember { mutableStateOf(store.read()) }
    var editing by rememberSaveable { mutableStateOf(saved == null) }
    var amount by rememberSaveable { mutableStateOf(saved?.let { BigDecimal(it.amountMinor).movePointLeft(2).stripTrailingZeros().toPlainString() }.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(saved?.date.orEmpty()) }
    var term by rememberSaveable { mutableStateOf(saved?.termCount?.toString().orEmpty()) }
    var unitName by rememberSaveable { mutableStateOf((saved?.termUnit ?: ReminderTermUnit.DAYS).name) }
    val unit = ReminderTermUnit.valueOf(unitName)
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(store.notificationsEnabled()) }
    var message by remember { mutableStateOf<Int?>(null) }
    var errors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = store.notificationsEnabled(); store.reconcile()
    }
    DisposableEffect(store) {
        val unsubscribe = store.observe { saved = store.read() }
        onDispose { unsubscribe() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { enabled = store.notificationsEnabled(); store.reconcile(); saved = store.read() } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val scroll = rememberScrollState()
    var headerHeight by remember { mutableIntStateOf(0) }
    val statusBarHeight = WindowInsets.statusBars.getTop(androidx.compose.ui.platform.LocalDensity.current)
    val darkStatusIcons by remember(statusBarHeight) { derivedStateOf { headerHeight > 0 && scroll.value >= headerHeight - statusBarHeight } }
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(darkStatusIcons) { (view.context as? android.app.Activity)?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars = darkStatusIcons } }
    val amountValue = parsePaymentAmount(amount)
    val termValue = term.toIntOrNull()
    val draft = PaymentReminder("draft", amountValue ?: 0, date, termValue ?: 0, termUnit = unit)
    val fieldShape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding().verticalScroll(scroll)) {
        Surface(modifier = Modifier.onSizeChanged { headerHeight = it.height }, shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp), color = Color.Transparent) {
            Column(Modifier.fillMaxWidth().brandGradient().statusBarsPadding().padding(16.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.reminder_title), fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(stringResource(R.string.reminder_intro), color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!enabled) Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(Modifier.brandGradient(strength = .15f).padding(14.dp)) {
                    Text(stringResource(R.string.reminder_permission_hint), fontSize = 13.sp)
                    TextButton(onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }) { Text(stringResource(R.string.reminder_enable_notifications)) }
                }
            }
            if (editing) {
                OutlinedTextField(amount, { amount = it; message = null }, label = { Text(stringResource(R.string.reminder_amount)) }, suffix = { Text(stringResource(R.string.unit_currency)) }, shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), isError = errors && amountValue == null)
                OutlinedButton(onClick = { keyboard?.hide(); calendarOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White), shape = fieldShape) {
                    Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(12.dp))
                    Text(if (date.isBlank()) stringResource(R.string.reminder_date) else draft.dueCalendar()?.let { DateFormat.getDateInstance(DateFormat.LONG, locale).format(it.time) }.orEmpty())
                }
                OutlinedTextField(term, { term = it.filter(Char::isDigit).take(4); message = null }, label = { Text(stringResource(R.string.reminder_term)) }, shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), isError = errors && termValue !in 1..3650)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ReminderTermUnit.entries.forEachIndexed { index, choice ->
                        SegmentedButton(selected = choice == unit, onClick = { unitName = choice.name; message = null },
                            shape = SegmentedButtonDefaults.itemShape(index, ReminderTermUnit.entries.size), icon = {},
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = Color.Transparent, activeContentColor = Color.White, inactiveContainerColor = Color.White),
                            modifier = if (choice == unit) Modifier.brandGradient(SegmentedButtonDefaults.itemShape(index, ReminderTermUnit.entries.size)) else Modifier) {
                            Text(stringResource(when (choice) { ReminderTermUnit.DAYS -> R.string.reminder_unit_days; ReminderTermUnit.MONTHS -> R.string.reminder_unit_months; ReminderTermUnit.PAYMENTS -> R.string.reminder_unit_payments }), fontSize = 12.sp)
                        }
                    }
                }
                if (errors && !draft.valid(System.currentTimeMillis())) Text(stringResource(R.string.reminder_validation), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (draft.valid(System.currentTimeMillis())) ReminderSchedule(draft, locale)
                Button(onClick = {
                    errors = true
                    if (draft.valid(System.currentTimeMillis())) {
                        if (store.save(draft.amountMinor, date, draft.termCount, unit)) {
                            saved = store.read(); editing = false; message = R.string.reminder_saved; keyboard?.hide()
                            scope.launch { withFrameNanos { }; scroll.animateScrollTo(scroll.maxValue) }
                            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else message = R.string.reminder_save_error
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).brandGradient(RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)) {
                    Text(stringResource(if (saved == null) R.string.reminder_save else R.string.reminder_save_changes), fontWeight = FontWeight.Bold)
                }
                if (saved != null) TextButton(onClick = { editing = false; errors = false; message = null; keyboard?.hide() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reminder_cancel)) }
            }
            message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
            saved?.let { reminder ->
                Text(stringResource(R.string.reminder_saved_entity), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(reminder.amountMinor / 100.0) + " " + stringResource(R.string.unit_currency), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(reminder.dueCalendar()?.let { DateFormat.getDateInstance(DateFormat.LONG, locale).format(it.time) }.orEmpty(), fontSize = 15.sp)
                        }
                        Text(stringResource(R.string.reminder_entity_term, termText(reminder.termCount, reminder.termUnit)), fontSize = 14.sp)
                        if (reminder.expired(System.currentTimeMillis())) Text(stringResource(R.string.reminder_expired), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else if (reminder.delivered) Text(stringResource(R.string.reminder_delivered), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        else ReminderSchedule(reminder, locale)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                amount = BigDecimal(reminder.amountMinor).movePointLeft(2).stripTrailingZeros().toPlainString()
                                date = reminder.date; term = reminder.termCount.toString(); unitName = reminder.termUnit.name
                                editing = true; errors = false; message = null
                                scope.launch { withFrameNanos { }; scroll.animateScrollTo(headerHeight.coerceAtMost(scroll.maxValue)) }
                            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.reminder_edit), fontSize = 12.sp) }
                            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.reminder_entity_delete), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
    if (calendarOpen) ReminderCalendar(date, { calendarOpen = false }) { date = it; calendarOpen = false; message = null }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.reminder_delete_question)) },
        text = { Text(stringResource(R.string.reminder_delete_hint)) }, confirmButton = { TextButton(onClick = {
            if (store.delete()) { saved = null; amount = ""; date = ""; term = ""; unitName = ReminderTermUnit.DAYS.name; editing = true; errors = false; message = R.string.reminder_deleted }
            else message = R.string.reminder_save_error
            confirmDelete = false
        }) { Text(stringResource(R.string.reminder_delete)) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.reminder_cancel)) } })
}

@Composable
private fun ReminderSchedule(reminder: PaymentReminder, locale: Locale) {
    val trigger = reminder.reminderTime() ?: return
    val text = if (trigger <= System.currentTimeMillis()) stringResource(R.string.reminder_soon) else stringResource(R.string.reminder_schedule, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(java.util.Date(trigger)))
    Text(text, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
