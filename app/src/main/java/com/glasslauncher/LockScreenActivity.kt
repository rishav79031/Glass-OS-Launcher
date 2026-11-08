package com.glasslauncher

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.glasslauncher.ui.GlassBackdrop
import com.glasslauncher.ui.LocalGlassBackdrop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasslauncher.data.NotificationHub
import com.glasslauncher.ui.GlassTuning
import com.glasslauncher.ui.LocalGlassTuning
import com.glasslauncher.ui.LocalWidgetEnvironment
import com.glasslauncher.ui.NotificationCentre
import com.glasslauncher.ui.Permissions
import com.glasslauncher.ui.WidgetEnvironment
import kotlinx.coroutines.launch

/**
 * Native Android Lock Screen Activity:
 * Automatically shows when the user presses the lock button and wakes the device (setShowWhenLocked, setTurnScreenOn).
 * Features real-time 3D parallax wallpaper, liquid glass clock, interactive notifications, and swipe-up to unlock.
 */
class LockScreenActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val settings by vm.settings.collectAsStateWithLifecycle()
                val notifications by vm.notifications.collectAsStateWithLifecycle()
                val apps by vm.apps.collectAsStateWithLifecycle()
                val icons by vm.icons.collectAsStateWithLifecycle()
                val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
                val allowed = Permissions.hasNotificationAccess(this)
                val scope = rememberCoroutineScope()
                val unlockOffsetY = remember { Animatable(0f) }

                val unlockSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)

                fun unlockDevice() {
                    scope.launch {
                        unlockOffsetY.animateTo(-1400f, unlockSpring)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this@LockScreenActivity, null)
                        }
                        finish()
                        overridePendingTransition(0, android.R.anim.fade_out)
                    }
                }

                val tuning = GlassTuning(
                    settings.glassLook,
                    settings.widgetFrost,
                    settings.dockFrost,
                    settings.iconStyle,
                    settings.tintColor
                )

                var rootSize by remember { mutableStateOf(IntSize.Zero) }
                val blurLevel = settings.wallpaperBlur
                val backdrop = remember(wallpaper, rootSize, blurLevel > 0.5f, blurLevel > 0.05f) {
                    wallpaper?.let {
                        val sharp = when {
                            blurLevel > 0.5f -> it.heavy
                            blurLevel > 0.05f -> it.light
                            else -> it.sharp
                        }
                        GlassBackdrop(sharp, if (blurLevel > 0.5f) it.heavy else it.light, it.heavy, rootSize, 1f)
                    }
                }

                CompositionLocalProvider(
                    LocalGlassTuning provides tuning,
                    LocalGlassBackdrop provides backdrop,
                    LocalWidgetEnvironment provides WidgetEnvironment(
                        note = settings.noteText,
                        notesList = settings.notesList,
                        reminders = settings.reminders,
                    )
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { rootSize = it }
                            .graphicsLayer {
                                translationY = unlockOffsetY.value
                                alpha = (1f + unlockOffsetY.value / 650f).coerceIn(0f, 1f)
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (unlockOffsetY.value < -200f) {
                                            unlockDevice()
                                        } else {
                                            scope.launch { unlockOffsetY.animateTo(0f, unlockSpring) }
                                        }
                                    }
                                ) { change, dragAmount ->
                                    if (dragAmount < 0 || unlockOffsetY.value < 0) {
                                        change.consume()
                                        scope.launch {
                                            unlockOffsetY.snapTo((unlockOffsetY.value + dragAmount).coerceAtMost(0f))
                                        }
                                    }
                                }
                            }
                    ) {
                        NotificationCentre(
                            allItems = notifications,
                            apps = apps,
                            icons = icons,
                            display = settings.notificationDisplay,
                            clockStyle = settings.lockClockStyle,
                            clockColor = settings.lockClockColor,
                            lockClockScale = settings.lockClockScale,
                            lockClockOffsetX = settings.lockClockOffsetX,
                            lockClockOffsetY = settings.lockClockOffsetY,
                            lockWidgets = settings.lockWidgets,
                            lockWallpaper = settings.lockWallpaper,
                            lock3dEffect = settings.lock3dEffect,
                            lockWallpaperScale = settings.lockWallpaperScale,
                            lockWallpaperOffsetX = settings.lockWallpaperOffsetX,
                            lockWallpaperOffsetY = settings.lockWallpaperOffsetY,
                            homeWallpaper = wallpaper?.sharp,
                            allowed = allowed,
                            glassLook = settings.notificationGlassLook,
                            isLockScreen = true,
                            onUnlock = ::unlockDevice,
                            onOpen = { item ->
                                NotificationHub.open(this@LockScreenActivity, item)
                                unlockDevice()
                            },
                            onDismissItem = NotificationHub::dismiss,
                            onClearAll = NotificationHub::clearAll,
                            onRequestAccess = {
                                Permissions.open(
                                    this@LockScreenActivity,
                                    Permissions.notificationAccessIntent(this@LockScreenActivity),
                                    android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                )
                            },
                            onUpdateClockStyle = vm::setLockClockStyle,
                            onUpdateClockColor = vm::setLockClockColor,
                            onUpdateClockTransform = vm::setLockClockTransform,
                            onToggleLockWidget = vm::toggleLockWidget,
                            onUpdateLockWallpaper = vm::setLockWallpaper,
                            onToggle3dEffect = vm::setLock3dEffect,
                            onUpdateWallpaperTransform = vm::setLockWallpaperTransform,
                            onClose = ::unlockDevice
                        )

                        // iOS Bottom Home Bar Indicator (Swipe up to unlock)
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp)
                                .width(140.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .clickable { unlockDevice() }
                        )
                    }
                }
            }
        }
    }
}
