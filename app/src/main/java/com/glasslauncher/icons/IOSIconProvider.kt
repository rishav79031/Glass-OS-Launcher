package com.glasslauncher.icons

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.glasslauncher.R

/**
 * Built-in Apple HIG default icon pack provider.
 * Maps common Android package names across Google Pixel, Samsung Galaxy, Xiaomi, OnePlus,
 * Motorola, AOSP, and popular apps to authentic iOS-style vector drawables.
 */
object IOSIconProvider {

    private val defaultIconMap: Map<String, Int> = mapOf(
        // Phone / Dialer
        "com.google.android.dialer" to R.drawable.ic_ios_phone,
        "com.android.dialer" to R.drawable.ic_ios_phone,
        "com.samsung.android.dialer" to R.drawable.ic_ios_phone,
        "com.android.phone" to R.drawable.ic_ios_phone,
        "com.sh.smart.caller" to R.drawable.ic_ios_phone,
        "com.truecaller" to R.drawable.ic_ios_phone,
        "com.oneplus.dialer" to R.drawable.ic_ios_phone,
        "com.coloros.phoneno" to R.drawable.ic_ios_phone,
        "com.asus.contacts" to R.drawable.ic_ios_phone,

        // Camera
        "com.google.android.GoogleCamera" to R.drawable.ic_ios_camera,
        "com.android.camera" to R.drawable.ic_ios_camera,
        "com.android.camera2" to R.drawable.ic_ios_camera,
        "com.sec.android.app.camera" to R.drawable.ic_ios_camera,
        "com.motorola.camera2" to R.drawable.ic_ios_camera,
        "com.motorola.camera3" to R.drawable.ic_ios_camera,
        "com.oneplus.camera" to R.drawable.ic_ios_camera,
        "com.oppo.camera" to R.drawable.ic_ios_camera,
        "com.miui.camera" to R.drawable.ic_ios_camera,
        "org.codeaurora.snapcam" to R.drawable.ic_ios_camera,

        // Messages / SMS
        "com.google.android.apps.messaging" to R.drawable.ic_ios_messages,
        "com.android.mms" to R.drawable.ic_ios_messages,
        "com.samsung.android.messaging" to R.drawable.ic_ios_messages,
        "com.oneplus.mms" to R.drawable.ic_ios_messages,
        "com.motorola.messaging" to R.drawable.ic_ios_messages,
        "com.simplemobiletools.smsmessenger" to R.drawable.ic_ios_messages,

        // Web Browser / Chrome
        "com.android.chrome" to R.drawable.ic_ios_browser,
        "com.chrome.beta" to R.drawable.ic_ios_browser,
        "com.chrome.dev" to R.drawable.ic_ios_browser,
        "com.chrome.canary" to R.drawable.ic_ios_browser,
        "org.mozilla.firefox" to R.drawable.ic_ios_browser,
        "com.sec.android.app.sbrowser" to R.drawable.ic_ios_browser,
        "com.microsoft.emmx" to R.drawable.ic_ios_browser,
        "com.opera.browser" to R.drawable.ic_ios_browser,
        "com.brave.browser" to R.drawable.ic_ios_browser,
        "com.duckduckgo.mobile.android" to R.drawable.ic_ios_browser,
        "com.google.android.browser" to R.drawable.ic_ios_browser,
        "com.android.browser" to R.drawable.ic_ios_browser,
        "com.vivo.browser" to R.drawable.ic_ios_browser,
        "com.coloros.browser" to R.drawable.ic_ios_browser,
        "com.mi.globalbrowser" to R.drawable.ic_ios_browser,

        // Settings / Preferences
        "com.android.settings" to R.drawable.ic_ios_settings,
        "com.google.android.settings" to R.drawable.ic_ios_settings,

        // Files / Documents
        "com.google.android.apps.nbu.files" to R.drawable.ic_ios_files,
        "com.android.documentsui" to R.drawable.ic_ios_files,
        "com.sec.android.app.myfiles" to R.drawable.ic_ios_files,
        "com.oneplus.filemanager" to R.drawable.ic_ios_files,
        "com.mi.android.globalFileexplorer" to R.drawable.ic_ios_files,
        "com.coloros.filemanager" to R.drawable.ic_ios_files,

        // Photos / Gallery
        "com.google.android.apps.photos" to R.drawable.ic_ios_photos,
        "com.google.android.apps.photosgo" to R.drawable.ic_ios_photos,
        "com.sec.android.gallery3d" to R.drawable.ic_ios_photos,
        "com.android.gallery3d" to R.drawable.ic_ios_photos,
        "com.android.gallery" to R.drawable.ic_ios_photos,
        "com.oneplus.gallery" to R.drawable.ic_ios_photos,
        "com.miui.gallery" to R.drawable.ic_ios_photos,
        "com.coloros.gallery3d" to R.drawable.ic_ios_photos,
        "com.simplemobiletools.gallery" to R.drawable.ic_ios_photos,
        "com.simplemobiletools.gallery.pro" to R.drawable.ic_ios_photos,

        // Clock / Alarm
        "com.google.android.deskclock" to R.drawable.ic_ios_clock,
        "com.android.deskclock" to R.drawable.ic_ios_clock,
        "com.sec.android.app.clockpackage" to R.drawable.ic_ios_clock,
        "com.oneplus.deskclock" to R.drawable.ic_ios_clock,

        // App Store / Google Play
        "com.android.vending" to R.drawable.ic_playstore,
        "com.sec.android.app.samsungapps" to R.drawable.ic_ios_appstore,
        "com.xiaomi.mipicks" to R.drawable.ic_ios_appstore,

        // Email / Mail
        "com.google.android.gm" to R.drawable.ic_ios_mail,
        "com.microsoft.office.outlook" to R.drawable.ic_ios_mail,
        "com.samsung.android.email.provider" to R.drawable.ic_ios_mail,
        "com.android.email" to R.drawable.ic_ios_mail,
        "ru.yandex.mail" to R.drawable.ic_ios_mail,
        "com.yahoo.mobile.client.android.mail" to R.drawable.ic_ios_mail,

        // Music
        "com.google.android.apps.youtube.music" to R.drawable.ic_ios_music,
        "com.spotify.music" to R.drawable.ic_ios_music,
        "com.apple.android.music" to R.drawable.ic_ios_music,
        "com.sec.android.app.music" to R.drawable.ic_ios_music,
        "com.miui.player" to R.drawable.ic_ios_music,
        "com.amazon.mp3" to R.drawable.ic_ios_music,
        "deezer.android.app" to R.drawable.ic_ios_music,

        // Calendar
        "com.google.android.calendar" to R.drawable.ic_ios_calendar,
        "com.android.calendar" to R.drawable.ic_ios_calendar,
        "com.samsung.android.calendar" to R.drawable.ic_ios_calendar,

        // Calculator
        "com.google.android.calculator" to R.drawable.ic_ios_calculator,
        "com.android.calculator2" to R.drawable.ic_ios_calculator,
        "com.sec.android.app.popupcalculator" to R.drawable.ic_ios_calculator,
        "com.oneplus.calculator" to R.drawable.ic_ios_calculator,
        "com.miui.calculator" to R.drawable.ic_ios_calculator,

        // Notes
        "com.google.android.keep" to R.drawable.ic_ios_notes,
        "com.samsung.android.app.notes" to R.drawable.ic_ios_notes,
        "com.miui.notes" to R.drawable.ic_ios_notes,
        "com.oneplus.note" to R.drawable.ic_ios_notes,

        // Maps
        "com.google.android.apps.maps" to R.drawable.ic_ios_maps,
        "com.here.app.maps" to R.drawable.ic_ios_maps,
        "com.waze" to R.drawable.ic_ios_maps,

        // Contacts
        "com.google.android.contacts" to R.drawable.ic_ios_contacts,
        "com.android.contacts" to R.drawable.ic_ios_contacts,
        "com.samsung.android.app.contacts" to R.drawable.ic_ios_contacts,

        // Weather
        "com.google.android.apps.weather" to R.drawable.ic_ios_weather,
        "com.sec.android.daemonapp" to R.drawable.ic_ios_weather,
        "com.miui.weather2" to R.drawable.ic_ios_weather,
        "com.accuweather.android" to R.drawable.ic_ios_weather,

        // Health / Fitness
        "com.google.android.apps.fitness" to R.drawable.ic_ios_fitness,
        "com.sec.android.app.shealth" to R.drawable.ic_ios_fitness,
        "com.google.android.apps.healthdata" to R.drawable.ic_ios_fitness,
        "com.fitbit.FitbitMobile" to R.drawable.ic_ios_fitness,
        "com.xiaomi.wearable" to R.drawable.ic_ios_fitness,
    )

    /** Returns the resource ID of the built-in iOS asset for this package, if available. */
    fun getCustomIcon(packageName: String): Int? {
        val exact = defaultIconMap[packageName]
        if (exact != null) return exact
        val lower = packageName.lowercase()

        // Safety checks: exclude banking, payment, e-commerce, utilities, and known non-dialer apps
        if (lower.contains("phonepe") || lower.contains("pharmeasy") || lower.contains("phonemanager") ||
            lower.contains("phoneclone") || lower.contains("manager") || lower.contains("cleaner") ||
            lower.contains("optimizer") || lower.contains("pay") || lower.contains("wallet")) {
            return null
        }

        return when {
            lower == "com.android.phone" || lower.endsWith(".dialer") || lower.contains(".dialer.") ||
            lower.endsWith(".phone") || lower.contains(".incallui") || lower.contains(".telecom") -> R.drawable.ic_ios_phone
            lower.contains("camera") || lower.contains("snapcam") -> R.drawable.ic_ios_camera
            lower.contains("gallery") || lower.contains("photos") || lower.contains("album") -> R.drawable.ic_ios_photos
            lower.contains("chrome") -> R.drawable.ic_ios_browser
            lower.contains("vending") || lower.contains("play.store") -> R.drawable.ic_playstore
            lower.contains("mms") || lower.contains("messaging") || lower.contains("messages") -> R.drawable.ic_ios_messages
            lower == "com.android.settings" || lower == "com.google.android.settings" || lower.endsWith(".settings") -> R.drawable.ic_ios_settings
            lower.contains("deskclock") || lower.contains("alarmclock") || lower.endsWith(".clock") -> R.drawable.ic_ios_clock
            lower.contains("email") || lower.endsWith(".mail") || lower.contains(".mail.") || lower.contains("outlook") -> R.drawable.ic_ios_mail
            lower.contains("calendar") -> R.drawable.ic_ios_calendar
            lower.contains("calculator") -> R.drawable.ic_ios_calculator
            lower.contains("notes") || lower.contains(".note") || lower.contains("keep") || lower.contains("memo") -> R.drawable.ic_ios_notes
            lower.endsWith(".maps") || lower.contains(".maps.") || lower.contains("google.android.apps.maps") || lower == "com.waze" -> R.drawable.ic_ios_maps
            lower.endsWith(".contacts") || lower.contains(".contacts.") -> R.drawable.ic_ios_contacts
            lower.contains("weather") -> R.drawable.ic_ios_weather
            lower.contains("filemanager") || lower.contains("documentsui") || lower.contains("myfiles") -> R.drawable.ic_ios_files
            lower.contains("music") -> R.drawable.ic_ios_music
            else -> null
        }
    }

    private fun toDark(resId: Int): Int = when (resId) {
        R.drawable.ic_ios_phone -> R.drawable.ic_ios_phone_dark
        R.drawable.ic_ios_messages -> R.drawable.ic_ios_messages_dark
        R.drawable.ic_ios_browser -> R.drawable.ic_ios_browser_dark
        R.drawable.ic_ios_calendar -> R.drawable.ic_ios_calendar_dark
        R.drawable.ic_ios_contacts -> R.drawable.ic_ios_contacts_dark
        R.drawable.ic_ios_camera -> R.drawable.ic_ios_camera_dark
        R.drawable.ic_ios_settings -> R.drawable.ic_ios_settings_dark
        R.drawable.ic_ios_files -> R.drawable.ic_ios_files_dark
        R.drawable.ic_ios_mail -> R.drawable.ic_ios_mail_dark
        R.drawable.ic_ios_notes -> R.drawable.ic_ios_notes_dark
        R.drawable.ic_ios_music -> R.drawable.ic_ios_music_dark
        R.drawable.ic_ios_weather -> R.drawable.ic_ios_weather_dark
        R.drawable.ic_ios_maps -> R.drawable.ic_ios_maps_dark
        R.drawable.ic_ios_photos -> R.drawable.ic_ios_photos_dark
        R.drawable.ic_playstore -> R.drawable.ic_playstore_dark
        else -> resId
    }

    /** Returns a crisp Drawable of the built-in iOS icon, or null if no mapping exists. */
    fun getDrawable(context: Context, packageName: String, isDark: Boolean = false): Drawable? {
        val resId = getCustomIcon(packageName) ?: return null
        val finalRes = if (isDark) toDark(resId) else resId
        return ContextCompat.getDrawable(context, finalRes)
    }
}
