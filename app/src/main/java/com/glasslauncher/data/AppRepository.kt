package com.glasslauncher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Every launchable app across the personal and work profiles, kept current as packages change. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {
    companion object {
        private const val MAX_QUICK_ACTIONS = 4
        @Volatile
        private var cachedApps: List<AppEntry> = emptyList()
        @Volatile
        private var cachedInfos: Map<String, LauncherActivityInfo> = emptyMap()
    }

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val state = MutableStateFlow<List<AppEntry>>(cachedApps)
    val apps: StateFlow<List<AppEntry>> = state.asStateFlow()

    @Volatile
    private var activityInfos: Map<String, LauncherActivityInfo> = cachedInfos
    private var reloadJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reload()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun close() = launcherApps.unregisterCallback(callback)

    private fun reload() {
        reloadJob?.cancel()
        reloadJob = scope.launch(Dispatchers.IO) {
            val infos = HashMap<String, LauncherActivityInfo>()
            val entries = launcherApps.profiles.flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                launcherApps.getActivityList(null, user)
                    .filter { it.componentName.packageName != context.packageName }
                    .map { info ->
                        val key = "${info.componentName.flattenToShortString()}#$serial"
                        infos[key] = info
                        val packageInfo = runCatching { context.packageManager.getPackageInfo(info.componentName.packageName, 0) }.getOrNull()
                        AppEntry(
                            key = key,
                            label = info.label.toString().trim(),
                            component = info.componentName,
                            user = user,
                            category = info.applicationInfo.category,
                            isSystem = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                            updatedAt = packageInfo?.lastUpdateTime ?: 0L,
                            installedAt = packageInfo?.firstInstallTime ?: 0L,
                        )
                    }
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            activityInfos = infos
            cachedInfos = infos
            cachedApps = entries
            state.value = entries
        }
    }

    fun loadIcon(app: AppEntry): Drawable =
        com.glasslauncher.icons.IOSIconProvider.getDrawable(context, app.packageName)
            ?: activityInfos[app.key]?.let { runCatching { it.getIcon(context.resources.displayMetrics.densityDpi) }.getOrNull() }
            ?: context.packageManager.defaultActivityIcon

    fun launch(app: AppEntry, sourceBounds: Rect?, options: Bundle?) {
        try {
            launcherApps.startMainActivity(app.component, app.user, sourceBounds, options)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "${app.label} can't be opened", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "${app.label} can't be opened", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** The app's shortcuts (iOS Quick Actions). Android only shares these with the default home app. */
    fun quickActions(app: AppEntry): List<QuickAction> {
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        return runCatching { launcherApps.getShortcuts(query, app.user).orEmpty() }.getOrDefault(emptyList())
            .filter { it.isEnabled && (it.activity == null || it.activity == app.component) }
            .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
            .take(MAX_QUICK_ACTIONS)
            .map { QuickAction((it.shortLabel ?: it.longLabel ?: "").toString(), it) }
    }

    fun startQuickAction(action: QuickAction) {
        runCatching { launcherApps.startShortcut(action.info, null, null) }
    }

    fun share(app: AppEntry) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${app.packageName}")
        val chooser = Intent.createChooser(send, app.label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }
}
