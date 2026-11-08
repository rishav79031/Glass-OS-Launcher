/*
 * Glass Launcher
 * Copyright (c) 2026 Rishav Kumar (rishav79031@gmail.com). All rights reserved.
 */
package com.glasslauncher

import kotlinx.coroutines.flow.receiveAsFlow
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.glasslauncher.ui.LauncherRoot
import com.glasslauncher.ui.LauncherEvents
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val unlocks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val returns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val shadePulls = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    // A queue, so a request that starts the launcher waits until the Home Screen is ready for it.
    private val panels = kotlinx.coroutines.channels.Channel<String>(capacity = 1)
    private var screenWentOff = false
    // Set by USER_PRESENT: Samsung can send it before the launcher resumes, so it's remembered until we can play.
    private var userPresent = false
    private var stopped = false
    private var paused = false

    // Screen off → the next appearance is an unlock (zoom in from the Lock Screen), not a return from an app.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    com.glasslauncher.ui.OverlayControlCentre.hidePanel(this@MainActivity)
                    screenWentOff = true
                    userPresent = false
                    viewModel.onScreenOff()
                }
                Intent.ACTION_USER_PRESENT -> {
                    userPresent = true
                    maybePlayUnlock()
                }
                Intent.ACTION_SCREEN_ON -> maybePlayUnlock()
            }
        }
    }

    private fun isResumed() = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)

    /**
     * Plays the unlock zoom once the Home Screen is actually visible: resumed, screen on, keyguard gone.
     * Without a lock screen Android may resume the launcher while the screen is still off, and on Samsung the
     * unlock broadcast can come before or after resume, so every moment (resume, focus, screen on, user present)
     * checks all conditions and the first one that passes plays it.
     */
    private fun maybePlayUnlock() {
        if (!screenWentOff || !isResumed()) return
        val keyguard = getSystemService(android.app.KeyguardManager::class.java)
        val power = getSystemService(android.os.PowerManager::class.java)
        if (power?.isInteractive != true) return
        if (keyguard?.isKeyguardLocked == true && !userPresent) return
        playUnlock()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            maybePlayUnlock()
        } else if (isResumed() && viewModel.settings.value.glassStatusBar && getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked != true) {
            // On tablets, fold away the system shade and open iPadOS-style Control Centre.
            // On mobile phones, let the native Quick Settings / Control Center open seamlessly.
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600
            if (isTablet && GlassPanelGuard.dismissShade()) shadePulls.tryEmit(Unit)
        }
    }

    private fun playUnlock() {
        screenWentOff = false
        userPresent = false
        unlocks.tryEmit(Unit)
        viewModel.onUnlock()
    }

    override fun onResume() {
        super.onResume()
        // Back on the Home Screen: the launcher handles the swipe itself, and an open overlay panel closes.
        GlassPanelGuard.launcherInFront = true
        com.glasslauncher.ui.OverlayControlCentre.hidePanel(this)
        com.glasslauncher.ui.OverlayControlCentre.setStripVisible(this, false)
        if (paused && !screenWentOff) {
            paused = false
            returns.tryEmit(Unit)
        }
        maybePlayUnlock()
    }

    override fun onPause() {
        super.onPause()
        paused = true
        GlassPanelGuard.launcherInFront = false
        // Avoid keeping an active overlay window over other apps in background to prevent
        // Android system's persistent "Glass OS Launcher is actively displaying over other apps" notification.
        com.glasslauncher.ui.OverlayControlCentre.setStripVisible(this, false)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.glasslauncher.data.LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.glasslauncher.data.LanguageHelper.init(this)
        com.glasslauncher.bridge.ShizukuBridge.init()
        preferHighestRefreshRate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        intent?.getStringExtra(EXTRA_PANEL)?.let { panels.trySend(it) }
        setContent { LauncherRoot(viewModel, LauncherEvents(homePresses, unlocks, returns, shadePulls, panels.receiveAsFlow())) }
    }

    override fun onStart() {
        super.onStart()
        stopped = false
        if (!com.glasslauncher.ui.Permissions.isDefaultHome(this)) {
            viewModel.openDefaultHomePrompt()
        }
    }

    override fun onStop() {
        super.onStop()
        stopped = true
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    // Pressing Home while the launcher is already showing arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val panel = intent.getStringExtra(EXTRA_PANEL)
        if (panel != null) {
            panels.trySend(panel)
        } else if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.tryEmit(Unit)
            if (paused || stopped) {
                paused = false
            }
        } else if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) || intent.action == Intent.ACTION_MAIN) {
            if (!com.glasslauncher.ui.Permissions.isDefaultHome(this)) {
                viewModel.openDefaultHomePrompt()
            }
        }
    }

    companion object {
        /** Asks the launcher to open one of its panels (from the top-edge helper). */
        const val EXTRA_PANEL = "com.glasslauncher.PANEL"
        const val PANEL_CONTROLS = "controls"
        const val PANEL_NOTIFICATIONS = "notifications"
    }

    // The Tab S9 FE+ panel runs at up to 90 Hz; ask for the fastest mode at the current resolution.
    private fun preferHighestRefreshRate() {
        val display = runCatching { display }.getOrNull() ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}
