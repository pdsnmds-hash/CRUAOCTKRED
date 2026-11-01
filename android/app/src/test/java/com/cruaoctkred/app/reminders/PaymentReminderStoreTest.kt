package com.cruaoctkred.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PaymentReminderStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val alarms get() = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java))
    private fun date(offset: Int) = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }.time)
    @Before fun reset() { PaymentReminderStore(context).delete(); shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS) }
    @Test fun survivesStoreRecreationAndReschedulesOnBoot() {
        val store = PaymentReminderStore(context)
        assertTrue(store.save(123456, date(10), 30))
        val saved = store.read()!!
        assertEquals(saved, PaymentReminderStore(context).read())
        assertEquals(saved.reminderTime(), alarms.scheduledAlarms.single().triggerAtTime)
        context.getSystemService(AlarmManager::class.java).cancel(requireNotNull(alarms.scheduledAlarms.single().operation))
        assertTrue(alarms.scheduledAlarms.isEmpty())
        PaymentReminderReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(saved.reminderTime(), alarms.scheduledAlarms.single().triggerAtTime)
    }
    @Test fun editsReplaceAlarmDeletesCancelAndOldBroadcastCannotNotify() {
        val store = PaymentReminderStore(context)
        store.save(10000, date(1), 30)
        val old = store.read()!!
        store.save(20000, date(1), 60)
        assertEquals(1, alarms.scheduledAlarms.size)
        store.deliver(old.id)
        assertEquals(0, notifications.size())
        val current = store.read()!!
        assertTrue(store.delete())
        assertTrue(alarms.scheduledAlarms.isEmpty())
        store.deliver(current.id)
        assertNull(store.read()); assertEquals(0, notifications.size())
    }
    @Test fun dueReminderPostsOnceAndOpensReminderScreen() {
        val store = PaymentReminderStore(context)
        store.save(10000, date(1), 30)
        val saved = store.read()!!
        PaymentReminderReceiver().onReceive(context, Intent(PaymentReminderStore.ACTION_FIRE).putExtra("id", saved.id))
        assertTrue(store.read()!!.delivered)
        assertEquals(1, notifications.size())
        assertEquals(PaymentReminderStore.ACTION_OPEN, shadowOf(notifications.getNotification(PaymentReminderStore.NOTIFICATION_ID).contentIntent).savedIntent.action)
        store.deliver(saved.id)
        assertEquals(1, notifications.size())
    }
    @Test fun disabledNotificationsDoNotFalselyMarkDelivered() {
        val store = PaymentReminderStore(context)
        store.save(10000, date(1), 30)
        notifications.setNotificationsEnabled(false)
        store.deliver(store.read()!!.id)
        assertFalse(store.read()!!.delivered)
        assertEquals(0, notifications.size())
        notifications.setNotificationsEnabled(true)
        store.reconcile()
        assertEquals(1, alarms.scheduledAlarms.size)
    }
    @Test fun futureReminderCannotFireEarly() {
        val store = PaymentReminderStore(context)
        store.save(10000, date(10), 30)
        store.deliver(store.read()!!.id)
        assertFalse(store.read()!!.delivered); assertEquals(0, notifications.size())
    }
    @Test fun disabledReminderChannelAlsoBlocksDelivery() {
        val store = PaymentReminderStore(context)
        store.save(10000, date(1), 30)
        store.ensureChannel()
        context.getSystemService(NotificationManager::class.java).deleteNotificationChannel(PaymentReminderStore.CHANNEL)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(android.app.NotificationChannel(PaymentReminderStore.CHANNEL, "Payment", NotificationManager.IMPORTANCE_NONE))
        assertFalse(store.notificationsEnabled())
        store.deliver(store.read()!!.id)
        assertFalse(store.read()!!.delivered)
        assertEquals(0, notifications.size())
    }

    @Test fun preservesTermUnitsAndMigratesOldDaysWithoutChangingDate() {
        val store = PaymentReminderStore(context)
        for (unit in ReminderTermUnit.entries) {
            assertTrue(store.save(1250000, date(10), 3, unit))
            val saved = PaymentReminderStore(context).read()!!
            assertEquals(unit, saved.termUnit)
            assertEquals(3, saved.termCount)
            assertEquals(saved.reminderTime(), alarms.scheduledAlarms.single().triggerAtTime)
        }
        context.getSharedPreferences("payment_reminder_v1", android.content.Context.MODE_PRIVATE).edit().remove("term_unit").commit()
        assertEquals(ReminderTermUnit.DAYS, store.read()!!.termUnit)
        assertEquals(3, store.read()!!.termCount)
    }

}
