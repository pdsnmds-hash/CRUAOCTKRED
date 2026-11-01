package com.cruaoctkred.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AppCompatDelegate
import com.cruaoctkred.app.MainActivity
import com.cruaoctkred.app.R
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class PaymentReminderStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("payment_reminder_v1", Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    fun observe(onChange: () -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun read(): PaymentReminder? = preferences.getString("id", null)?.let {
        PaymentReminder(it, preferences.getLong("amount", 0), preferences.getString("date", "").orEmpty(), preferences.getInt("term", 0), preferences.getBoolean("delivered", false),
            ReminderTermUnit.entries.firstOrNull { unit -> unit.name == preferences.getString("term_unit", null) } ?: ReminderTermUnit.DAYS)
    }
    fun save(amount: Long, date: String, term: Int, unit: ReminderTermUnit = ReminderTermUnit.DAYS): Boolean {
        val reminder = PaymentReminder(UUID.randomUUID().toString(), amount, date, term, termUnit = unit)
        if (!reminder.valid(System.currentTimeMillis())) return false
        if (!preferences.edit().putString("id", reminder.id).putLong("amount", amount).putString("date", date).putInt("term", term).putString("term_unit", unit.name).putString("language", AppCompatDelegate.getApplicationLocales()[0]?.language ?: "uk").putBoolean("delivered", false).commit()) return false
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        reconcile()
        return true
    }
    fun delete(): Boolean {
        if (!preferences.edit().clear().commit()) return false
        alarms.cancel(pending(null))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        return true
    }
    private fun pending(id: String?): PendingIntent = PendingIntent.getBroadcast(context, 1719,
        Intent(context, PaymentReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun reconcile() {
        val reminder = read()
        if (reminder == null || reminder.delivered || reminder.expired(System.currentTimeMillis())) {
            alarms.cancel(pending(null)); return
        }
        val time = reminder.reminderTime() ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, maxOf(time, System.currentTimeMillis() + 1000), pending(reminder.id))
    }
    fun notificationsEnabled(): Boolean {
        ensureChannel()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_title), NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun deliver(id: String?) {
        val reminder = read() ?: return
        val now = System.currentTimeMillis()
        if (id != reminder.id || reminder.delivered || reminder.expired(now) || now < (reminder.reminderTime() ?: return) || !notificationsEnabled()) return
        val language = AppCompatDelegate.getApplicationLocales()[0]?.language ?: preferences.getString("language", "uk").orEmpty()
        val localized = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) })
        val amount = NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 2 }.format(reminder.amountMinor / 100.0)
        val contentIntent = PendingIntent.getActivity(context, 1720, Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val displayDate = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.forLanguageTag(language)).format(requireNotNull(reminder.dueCalendar()).time).trimEnd('.')
        val body = localized.getString(R.string.reminder_notification_body, amount, displayDate)
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.reminder_notification_title)).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(contentIntent)
            .setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            preferences.edit().putBoolean("delivered", true).commit()
            alarms.cancel(pending(null))
        } catch (_: SecurityException) { /* Permission can be revoked between the check and delivery. */ }
    }
    companion object {
        const val ACTION_OPEN = "com.cruaoctkred.app.OPEN_PAYMENT_REMINDER"
        const val ACTION_FIRE = "com.cruaoctkred.app.PAYMENT_REMINDER"
        const val CHANNEL = "payment_reminders"
        const val NOTIFICATION_ID = 1719
    }
}

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = PaymentReminderStore(context.applicationContext)
        if (intent.action == PaymentReminderStore.ACTION_FIRE) store.deliver(intent.getStringExtra("id"))
        else if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) store.reconcile()
    }
}
