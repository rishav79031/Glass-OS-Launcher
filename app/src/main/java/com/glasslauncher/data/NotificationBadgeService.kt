package com.glasslauncher.data

import android.app.ActivityOptions
import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/** Notifications published by [NotificationBadgeService]: badge counts per package and the items themselves. */
object NotificationHub {
    private val badgeState = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val itemState = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val activeCallState = MutableStateFlow<ActiveCallInfo?>(null)
    private val activeTimerState = MutableStateFlow<ActiveTimerInfo?>(null)
    private val healthStepsState = MutableStateFlow<Int?>(null)
    val badges: StateFlow<Map<String, Int>> = badgeState.asStateFlow()
    val items: StateFlow<List<NotificationItem>> = itemState.asStateFlow()
    val activeCall: StateFlow<ActiveCallInfo?> = activeCallState.asStateFlow()
    val activeTimer: StateFlow<ActiveTimerInfo?> = activeTimerState.asStateFlow()
    val healthSteps: StateFlow<Int?> = healthStepsState.asStateFlow()

    @Volatile
    internal var service: WeakReference<NotificationBadgeService>? = null

    internal fun publish(items: List<NotificationItem>) {
        itemState.value = items
        badgeState.value = items.groupingBy { it.packageName }.eachCount()
    }

    internal fun publishActiveCall(call: ActiveCallInfo?) {
        activeCallState.value = call
    }

    internal fun publishActiveTimer(timer: ActiveTimerInfo?) {
        activeTimerState.value = timer
    }

    internal fun publishHealthSteps(steps: Int?) {
        healthStepsState.value = steps
    }

    /** Opens the notification's target, then removes it if it was meant to clear on tap, like the system shade does. */
    fun open(context: Context, item: NotificationItem) {
        val intent = item.intent ?: return
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // The launcher is in the foreground, so it may start the activity on the app's behalf.
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }
        runCatching { intent.send(context, 0, null, null, null, null, options) }
        if (item.autoCancel) dismiss(item.key)
    }

    fun dismiss(key: String) {
        runCatching { service?.get()?.cancelNotification(key) }
    }

    fun clearAll() {
        runCatching { service?.get()?.cancelAllNotifications() }
    }
}

/**
 * Feeds the red iOS-style badges and the Notification Centre. Only runs once the user grants notification access;
 * it also lets the Control Centre see the active media session for Now Playing. Nothing leaves the device.
 */
class NotificationBadgeService : NotificationListenerService() {
    override fun onListenerConnected() {
        NotificationHub.service = WeakReference(this)
        publish()
    }

    override fun onListenerDisconnected() {
        NotificationHub.service = null
        NotificationHub.publish(emptyList())
        NotificationHub.publishActiveCall(null)
        NotificationHub.publishActiveTimer(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = publish()

    override fun onNotificationRemoved(sbn: StatusBarNotification) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return

        val callSbn = active.firstOrNull { sbn ->
            val n = sbn.notification
            n.category == Notification.CATEGORY_CALL ||
            (sbn.isOngoing && (sbn.packageName.contains("dialer") || sbn.packageName.contains("incallui") || sbn.packageName.contains("telecom") || sbn.packageName.contains("phone")))
        }
        if (callSbn != null) {
            val extras = callSbn.notification.extras
            val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG))?.toString().orEmpty()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
            val callIntent = callSbn.notification.contentIntent
                ?: callSbn.notification.fullScreenIntent
                ?: callSbn.notification.actions?.firstOrNull()?.actionIntent
            NotificationHub.publishActiveCall(
                ActiveCallInfo(
                    packageName = callSbn.packageName,
                    caller = title.ifBlank { "Active Call" },
                    detail = text,
                    intent = callIntent,
                    startTime = if (callSbn.postTime > 0) callSbn.postTime else System.currentTimeMillis(),
                )
            )
        } else {
            NotificationHub.publishActiveCall(null)
        }

        val timerSbn = active.firstOrNull { sbn ->
            val n = sbn.notification
            val pkg = sbn.packageName.lowercase()
            val isClock = pkg.contains("clock") || pkg.contains("timer") || pkg.contains("alarm")
            val isTimerCat = n.category == Notification.CATEGORY_STOPWATCH || n.category == Notification.CATEGORY_ALARM || n.category == Notification.CATEGORY_EVENT
            val extras = n.extras
            val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
            val hasChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
            val looksLikeTimer = text.contains(":") || title.contains("Timer", ignoreCase = true) || title.contains("Stopwatch", ignoreCase = true)
            sbn.isOngoing && (isClock || isTimerCat || hasChronometer || looksLikeTimer)
        }
        if (timerSbn != null) {
            val extras = timerSbn.notification.extras
            val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG))?.toString().orEmpty()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
            val isChrono = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
            val timerIntent = timerSbn.notification.contentIntent
                ?: timerSbn.notification.fullScreenIntent
                ?: timerSbn.notification.actions?.firstOrNull()?.actionIntent
            NotificationHub.publishActiveTimer(
                ActiveTimerInfo(
                    packageName = timerSbn.packageName,
                    title = title.ifBlank { "Timer" },
                    text = text.ifBlank { "Running" },
                    intent = timerIntent,
                    isChronometer = isChrono,
                )
            )
        } else {
            NotificationHub.publishActiveTimer(null)
        }

        // Sync steps from active health / fitness tracking notifications (Google Fit, Samsung Health, OnePlus, Fitbit, etc.)
        val healthSbn = active.firstOrNull { sbn ->
            val pkg = sbn.packageName.lowercase()
            val isFitnessApp = pkg.contains("fitness") || pkg.contains("shealth") || pkg.contains("health") ||
                    pkg.contains("fitbit") || pkg.contains("wearable") || pkg.contains("zepp") || pkg.contains("huawei") || pkg.contains("pedometer")
            if (!isFitnessApp) return@firstOrNull false
            val extras = sbn.notification.extras
            val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
            val combined = "$title $text".lowercase()
            combined.contains("step")
        }
        if (healthSbn != null) {
            val extras = healthSbn.notification.extras
            val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
            val combined = "$title $text"
            val match = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*steps?""", RegexOption.IGNORE_CASE).find(combined)
                ?: Regex("""steps?[:\s]+([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)""", RegexOption.IGNORE_CASE).find(combined)
            val parsedSteps = match?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            if (parsedSteps != null && parsedSteps > 0) {
                NotificationHub.publishHealthSteps(parsedSteps)
            }
        }

        NotificationHub.publish(
            active
                .filter {
                    it.packageName != packageName &&
                    it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
                }
                .mapNotNull(::toItem)
                .sortedByDescending { it.postTime }
                .take(MAX_ITEMS),
        )
    }

    private fun toItem(sbn: StatusBarNotification): NotificationItem? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        return NotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            title = title,
            text = text,
            postTime = sbn.postTime,
            intent = sbn.notification.contentIntent,
            autoCancel = sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
        )
    }

    private companion object {
        const val MAX_ITEMS = 60
    }
}
