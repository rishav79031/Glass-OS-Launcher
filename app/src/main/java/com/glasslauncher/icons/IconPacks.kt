package com.glasslauncher.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Xml
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File

@Immutable
data class IconPack(val packageName: String, val label: String)

/**
 * Third-party icon packs in the common ADW/Nova format: the pack app ships an appfilter.xml that maps each app's
 * component to one of its drawables. Any pack from the Play Store (including iOS-style ones) works.
 */
class IconPackRepository(private val context: Context) {
    fun installed(): List<IconPack> {
        val pm = context.packageManager
        return PACK_ACTIONS
            .flatMap { action -> runCatching { pm.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList()) }
            .map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { pkg ->
                runCatching { IconPack(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()) }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Reads the pack's appfilter once; returns null if the pack is gone or unreadable. */
    fun load(packageName: String): LoadedIconPack? = runCatching {
        val res = context.packageManager.getResourcesForApplication(packageName)
        val parser = res.getIdentifier("appfilter", "xml", packageName).takeIf { it != 0 }?.let { res.getXml(it) }
            ?: Xml.newPullParser().apply { setInput(res.assets.open("appfilter.xml"), "UTF-8") }
        val byComponent = HashMap<String, String>()
        val byPackage = HashMap<String, String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
            val component = parser.getAttributeValue(null, "component") ?: continue
            val drawable = parser.getAttributeValue(null, "drawable") ?: continue
            // "ComponentInfo{com.android.settings/com.android.settings.Settings}"
            val flat = component.substringAfter("{", "").substringBefore("}", "")
            val name = ComponentName.unflattenFromString(flat) ?: continue
            byComponent[name.flattenToString()] = drawable
            byPackage.putIfAbsent(name.packageName, drawable)
        }
        LoadedIconPack(res, packageName, byComponent, byPackage, context.resources.displayMetrics.densityDpi)
    }.getOrNull()

    private companion object {
        val PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "org.adw.launcher.THEME",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme"
        )
    }
}

class LoadedIconPack(
    private val res: Resources,
    val packageName: String,
    private val byComponent: Map<String, String>,
    private val byPackage: Map<String, String>,
    private val density: Int,
) {
    /** The pack's icon for this app: an exact activity match first, then any icon the pack has for the package. */
    fun icon(component: ComponentName): Drawable? {
        val name = byComponent[component.flattenToString()] ?: byPackage[component.packageName] ?: byRole(component.packageName) ?: return null
        val id = res.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }
            ?: res.getIdentifier(name, "mipmap", packageName).takeIf { it != 0 }
            ?: return null
        return runCatching { res.getDrawableForDensity(id, density, null) }.getOrNull()
    }

    /**
     * Packs list Google's and Samsung's apps, but OnePlus, Oppo, Xiaomi and others ship their own clock, camera,
     * gallery and so on. Those get the pack's icon for the same kind of app.
     */
    private fun byRole(pkg: String): String? {
        val role = ROLES.firstOrNull { (_, words) -> words.any { it in pkg.lowercase() } }?.first ?: return null
        return roleIcons.getOrPut(role) {
            val words = ROLES.first { it.first == role }.second
            byPackage.entries.firstOrNull { (p, _) -> words.any { it in p.lowercase() } }?.value
        }
    }

    private val roleIcons = HashMap<String, String?>()

    private companion object {
        val ROLES = listOf(
            "clock" to listOf("deskclock", "alarmclock", ".clock"),
            "camera" to listOf("camera"),
            "gallery" to listOf("gallery", "photos", ".album"),
            "calculator" to listOf("calculator"),
            "contacts" to listOf("contacts"),
            "phone" to listOf("dialer", ".phone", "incallui"),
            "messages" to listOf(".mms", "messaging", ".message"),
            "calendar" to listOf("calendar"),
            "files" to listOf("filemanager", "documentsui", ".files", "myfiles"),
            "weather" to listOf("weather"),
            "notes" to listOf("note"),
            "music" to listOf(".music"),
            "recorder" to listOf("recorder", "soundrecorder"),
            "compass" to listOf("compass"),
            "settings" to listOf(".settings"),
            "store" to listOf("market", "appstore", ".store"),
        )
    }
}

/** Icons the user picked for single apps ("Edit Icon"), stored on the device. */
class CustomIconStore(private val context: Context) {
    private val dir = File(context.filesDir, "icons")
    private val prefs = context.getSharedPreferences("custom_icons", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    /** App key → stamp; the stamp changes on every edit so the icon re-renders. */
    val icons: StateFlow<Map<String, Long>> = state.asStateFlow()

    fun drawable(key: String): Drawable? {
        val file = fileFor(key)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.path)?.let { BitmapDrawable(context.resources, it) }
    }

    suspend fun set(key: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dir.mkdirs()
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val side = minOf(info.size.width, info.size.height).coerceAtLeast(1)
                val scale = (ICON_SIDE.toFloat() / side).coerceAtMost(1f)
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            }
            // Centre square crop, like picking an icon on iOS Shortcuts.
            val side = minOf(bitmap.width, bitmap.height)
            val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
            fileFor(key).outputStream().use { square.compress(Bitmap.CompressFormat.PNG, 100, it) }
            update(key, System.currentTimeMillis())
            true
        }.getOrDefault(false)
    }

    fun reset(key: String) {
        fileFor(key).delete()
        update(key, null)
    }

    private fun update(key: String, stamp: Long?) {
        val next = if (stamp == null) state.value - key else state.value + (key to stamp)
        state.value = next
        prefs.edit { if (stamp == null) remove(key) else putLong(key, stamp) }
    }

    private fun read(): Map<String, Long> = prefs.all.mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap()

    // App keys contain '/' and '#', so the file name is a hash of the key.
    private fun fileFor(key: String) = File(dir, "${key.hashCode().toUInt()}.png")

    private companion object {
        const val ICON_SIDE = 256
    }
}
