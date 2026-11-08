package com.glasslauncher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.glasslauncher.ui.OverlayControlCentre
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * Optional helper (full version only) that gives the very top edge to Glass, on the Home Screen and in every app.
 * Accessibility services may place an invisible strip over the status bar itself, so a swipe down there never
 * reaches the phone's own panel: from the right half it opens Glass Control Centre, from the left half Glass
 * Notification Centre. Two quick swipes open the phone's own panel, so it's always reachable.
 *
 * As a backup (some phones let the shade open anyway), the system UI's window changes are watched too, and a shade
 * that opens is folded away. No screen content is read.
 */
class GlassPanelGuard : AccessibilityService() {
    private var strip: View? = null
    private var lastOpened = 0L

    override fun onServiceConnected() {
        active = WeakReference(this)
        addStrip()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addStrip() {
        if (strip != null) return
        val density = resources.displayMetrics.density
        val height = resources.getIdentifier("status_bar_height", "dimen", "android").takeIf { it != 0 }
            ?.let { resources.getDimensionPixelSize(it) } ?: (28 * density).roundToInt()
        val view = View(this).apply {
            // Practically invisible, but a window that never draws counts as hidden and gets no touches.
            setBackgroundColor(0x01000000)
            var startX = 0f
            var startY = 0f
            var fired = false
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        fired = false
                    }
                    // The system claims a top-edge swipe after a few pixels (the strip then gets a cancel), so the
                    // pull is recognised early, and a cancel after any downward movement counts too.
                    MotionEvent.ACTION_MOVE -> if (!fired && event.rawY - startY > 8 * density) {
                        fired = true
                        onPull(fromLeft = startX < v.width / 2f)
                    }
                    MotionEvent.ACTION_CANCEL -> if (!fired && event.rawY - startY > 0f) {
                        fired = true
                        onPull(fromLeft = startX < v.width / 2f)
                    }
                }
                true
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        if (runCatching { getSystemService(WindowManager::class.java).addView(view, params) }.isSuccess) strip = view
    }

    private fun onPull(fromLeft: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        // On the Lock Screen, or a second pull right after the first: the phone's own panel.
        if (locked || now - lastOpened < 1_500) {
            lastOpened = 0L
            letSystemPanelThrough = now
            OverlayControlCentre.hidePanel(this)
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            return
        }
        lastOpened = now
        // The system may already have started its own panel from the same swipe: fold it away as it appears.
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        for (delay in longArrayOf(120, 350, 700)) handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) }, delay)
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        when {
            // The launcher shows its own panels over the Home Screen; Notification Centre lives there too.
            // On mobile phones, top-edge swipe opens Notification Centre (Control Centre is tablet only).
            launcherInFront || fromLeft || !isTablet -> runCatching { startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(MainActivity.EXTRA_PANEL, if (isTablet && !fromLeft) MainActivity.PANEL_CONTROLS else MainActivity.PANEL_NOTIFICATIONS),
            ) }
            else -> OverlayControlCentre.showPanel(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !shadeOpened(event)) return
        val now = SystemClock.elapsedRealtime()
        if (now - letSystemPanelThrough < 3_000) return
        if (getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) return
        // A pull the strip already handled: just make sure the system's panel doesn't stay open.
        if (now - lastOpened < 1_500) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            return
        }
        if (performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)) {
            lastOpened = now
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600
            if (launcherInFront || !isTablet) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(MainActivity.EXTRA_PANEL, if (isTablet) MainActivity.PANEL_CONTROLS else MainActivity.PANEL_NOTIFICATIONS)
                )
            } else {
                OverlayControlCentre.showPanel(this)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        strip?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        strip = null
        if (active?.get() === this) active = null
        super.onDestroy()
    }

    companion object {
        private var active: WeakReference<GlassPanelGuard>? = null
        private var letSystemPanelThrough = 0L

        /** The running helper, for windows that should sit above everything without the overlay permission. */
        fun service(): AccessibilityService? = active?.get()

        /** Set by the launcher while its Home Screen is in front. */
        @Volatile
        var launcherInFront = false

        /** Folds the system shade away; false when the helper isn't turned on. */
        fun dismissShade(): Boolean = active?.get()?.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) == true

        private fun shadeOpened(event: AccessibilityEvent): Boolean {
            if (event.className?.toString() != "android.widget.FrameLayout") return false
            val text = event.text.map { it.toString().lowercase() }
            val named = text.any { "notification shade" in it || "notification panel" in it || "quick settings" in it || "quick panel" in it }
            return (named && text.size > 1) || text.size >= 6
        }
    }
}
