package com.glasslauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.glasslauncher.data.SettingsStore
import kotlin.math.roundToInt

/**
 * Glass Control Centre over other apps. Android only lets an app draw on top of others with "Display over other
 * apps", and the system keeps the status bar's own strip, so a thin invisible band just *below* the status bar on
 * the right catches the swipe. It's only there while another app is in front: on the Home Screen the launcher
 * handles the swipe itself.
 */
object OverlayControlCentre {
    private var strip: View? = null
    private var panel: ComposeView? = null
    private var panelOwner: PanelOwner? = null
    private var panelWindows: WindowManager? = null

    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))

    /** Shows the swipe strip (launcher in the background) or removes it (launcher in front, or turned off). */
    fun setStripVisible(context: Context, visible: Boolean) {
        val app = context.applicationContext
        val isTablet = app.resources.configuration.smallestScreenWidthDp >= 600
        // Control Centre overlay is tablet only; not needed on mobile phones
        val enabled = visible && isTablet && canDraw(app) && com.glasslauncher.GlassPanelGuard.service() == null && SettingsStore(app).settings.value.glassStatusBar
        if (!enabled) {
            strip?.let { runCatching { windowManager(app).removeView(it) } }
            strip = null
            return
        }
        if (strip != null) return
        val density = app.resources.displayMetrics.density
        val statusBar = app.resources.getIdentifier("status_bar_height", "dimen", "android").takeIf { it != 0 }
            ?.let { app.resources.getDimensionPixelSize(it) } ?: (24 * density).roundToInt()
        val view = View(app).apply {
            // Practically invisible, but a window that never draws counts as hidden and gets no touches.
            setBackgroundColor(0x01000000)
            setOnTouchListener(StripTouch(app, 40 * density))
        }
        val params = WindowManager.LayoutParams(
            (app.resources.displayMetrics.widthPixels * 0.38f).roundToInt(),
            (14 * density).roundToInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = statusBar
        }
        if (runCatching { windowManager(app).addView(view, params) }.isSuccess) strip = view
    }

    /** Opens Glass Control Centre over whatever is on screen (tablet only). */
    fun showPanel(context: Context) {
        val app = context.applicationContext
        val isTablet = app.resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet) return
        // With the top-edge helper on, its accessibility window is used, which needs no overlay permission.
        val helper = com.glasslauncher.GlassPanelGuard.service()
        if (panel != null || (helper == null && !canDraw(app))) return
        val windows = (helper ?: app).getSystemService(WindowManager::class.java)
        val owner = PanelOwner()
        // Activities started from the panel need a new task, and opening one closes the panel.
        val launching = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) {
                super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                hidePanel(app)
            }
        }
        val settings = SettingsStore(app).settings.value
        val view = ComposeView(launching).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    CompositionLocalProvider(
                        LocalOnBackPressedDispatcherOwner provides owner,
                        LocalGlassTuning provides GlassTuning(settings.glassLook, settings.widgetFrost, settings.dockFrost, settings.iconStyle, settings.tintColor),
                    ) {
                        ControlCentre(
                            onDismiss = { hidePanel(app) },
                            onOpenLauncherSettings = {
                                launching.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(app.packageName))
                            },
                        )
                    }
                }
            }
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (owner.onBackPressedDispatcher.hasEnabledCallbacks()) owner.onBackPressedDispatcher.onBackPressed() else hidePanel(app)
                    true
                } else {
                    keyCode == KeyEvent.KEYCODE_BACK
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (helper != null) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Dims the app behind, so the panel reads clearly even on phones that turn window blur off.
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0),
            PixelFormat.TRANSLUCENT,
        ).apply {
            // The app behind blurs like iOS, on phones that support cross-window blur.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurBehindRadius = (40 * app.resources.displayMetrics.density).roundToInt()
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            dimAmount = 0.45f
        }
        if (runCatching { windows.addView(view, params) }.isSuccess) {
            panel = view
            panelWindows = windows
            panelOwner = owner
            owner.resume()
            view.requestFocus()
        }
    }

    fun hidePanel(context: Context) {
        val view = panel ?: return
        panel = null
        panelOwner?.destroy()
        panelOwner = null
        runCatching { (panelWindows ?: windowManager(context.applicationContext)).removeView(view) }
        panelWindows = null
    }

    private fun windowManager(context: Context) = context.getSystemService(WindowManager::class.java)

    /** A downward swipe on the strip opens the panel; anything else is ignored. */
    private class StripTouch(private val context: Context, private val distance: Float) : View.OnTouchListener {
        private var startY = 0f
        private var fired = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    fired = false
                }
                MotionEvent.ACTION_MOVE -> if (!fired && event.rawY - startY > distance) {
                    fired = true
                    showPanel(context)
                }
            }
            return true
        }
    }

    /** Lifecycle, saved state and Back for a Compose window that isn't inside an activity. */
    private class PanelOwner : LifecycleOwner, SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this).apply { performRestore(null) }
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
        override val onBackPressedDispatcher = OnBackPressedDispatcher()

        fun resume() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
