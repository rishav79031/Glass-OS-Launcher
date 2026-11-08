package com.glasslauncher.ui

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.glasslauncher.data.NotificationBadgeService

/** Every special access the launcher asks for, with what it's for. Each check is cheap enough to run on resume. */
object Permissions {
    /** False in the Lite build, which leaves out notification access entirely. */
    val notificationsSupported: Boolean = com.glasslauncher.BuildConfig.NOTIFICATIONS

    fun isDefaultHome(context: Context): Boolean =
        context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

    fun hasNotificationAccess(context: Context): Boolean =
        notificationsSupported && context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun hasCalendar(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasActivityRecognition(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun canWriteSettings(context: Context): Boolean = Settings.System.canWrite(context)

    fun canControlDnd(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun homeRoleIntent(context: Context): Intent {
        val roles = context.getSystemService(RoleManager::class.java)
        return if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) roles.createRequestRoleIntent(RoleManager.ROLE_HOME) else Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun notificationAccessIntent(context: Context): Intent {
        val component = ComponentName(context, NotificationBadgeService::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
    }

    fun writeSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun openWriteSettings(context: Context) {
        val detail = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        val fallback = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        open(context, detail, fallback)
    }

    fun dndIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /** Opens a settings screen, falling back to the general list when the detail screen isn't available. */
    /** The top-edge helper exists only in the full version, alongside notification access. */
    val panelGuardSupported: Boolean = com.glasslauncher.BuildConfig.PANEL_GUARD

    fun hasPanelGuard(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals("${context.packageName}/${com.glasslauncher.GlassPanelGuard::class.java.name}", ignoreCase = true) }
    }

    /** This app's App Info page, where Android 13+ hides "Allow restricted settings" for sideloaded apps. */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))

    /**
     * Android blocks notification access for apps installed from an APK ("Restricted setting") until it's allowed
     * on the App Info page. That's a system safeguard, not a fault in the app, so we explain the step instead.
     */
    /** Apps installed from Google Play aren't restricted, so the hint is only for APK installs. */
    val showRestrictedHint: Boolean = com.glasslauncher.BuildConfig.FLAVOR != "play"

    const val RESTRICTED_HINT = "If Android says \"Restricted setting\" or \"App was denied access\": tap Open App Info, tap ⋮ (top right), choose Allow restricted settings, confirm, then turn the setting on again. (Top-Edge Swipe is under Accessibility → Installed apps → Glass Launcher.)"

    fun open(context: Context, intent: Intent, fallback: Intent? = null) {
        if (!context.startSafely(intent) && fallback != null) context.startSafely(fallback)
    }
}
