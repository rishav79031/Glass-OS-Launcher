package com.glasslauncher.ui

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

data class ControlItem(
    val id: String,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val iconColor: Color,
    val isToggle: Boolean = false,
    val isActive: Boolean = false,
    val action: (Context) -> Unit
)

@Composable
fun ControlsGallerySheet(
    controls: SystemControls,
    onOpenIntent: (Intent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val recording by com.glasslauncher.record.ScreenRecording.recording.collectAsStateWithLifecycle()

    val allControls = remember(controls, recording) {
        listOfNotNull(
            // Connectivity
            ControlItem("wifi", "Wi-Fi", "Connectivity", Icons.Rounded.Wifi, AccentBlue, true, controls.wifiOn) {
                scope.launch { controls.toggleWifi() }
            },
            ControlItem("bluetooth", "Bluetooth", "Connectivity", Icons.Rounded.Bluetooth, AccentBlue, true, controls.bluetoothOn) {
                scope.launch { controls.toggleBluetooth() }
            },
            ControlItem("cellular", "Cellular Data", "Connectivity", Icons.Rounded.SignalCellularAlt, AccentGreen, true, controls.mobileData) {
                scope.launch { controls.toggleMobileData() }
            },
            ControlItem("airplane", "Airplane Mode", "Connectivity", Icons.Rounded.AirplanemodeActive, AccentOrange, true, controls.airplaneOn) {
                scope.launch { controls.toggleAirplaneMode() }
            },
            ControlItem("hotspot", "Personal Hotspot", "Connectivity", Icons.Rounded.WifiTethering, AccentGreen, true, controls.hotspot) {
                scope.launch { controls.toggleHotspot() }
            },
            ControlItem("vpn", "VPN", "Connectivity", Icons.Rounded.VpnKey, AccentBlue) {
                context.startSafely(Intent(Settings.ACTION_VPN_SETTINGS))
            },
            ControlItem("nfc", "NFC", "Connectivity", Icons.Rounded.Nfc, AccentTeal) {
                context.startSafely(Intent(Settings.ACTION_NFC_SETTINGS))
            },

            // Utilities
            ControlItem("torch", "Flashlight", "Utilities", Icons.Rounded.FlashlightOn, AccentBlue, true, controls.torchOn) {
                controls.toggleTorch()
            },
            ControlItem("calculator", "Calculator", "Utilities", Icons.Rounded.Calculate, AccentOrange) {
                launchCalculator(context)
            },
            ControlItem("timer", "Timer", "Utilities", Icons.Rounded.Timer, AccentOrange) {
                launchTimer(context)
            },
            ControlItem("alarm", "Alarm", "Utilities", Icons.Rounded.Alarm, AccentOrange) {
                launchAlarm(context)
            },
            ControlItem("stopwatch", "Stopwatch", "Utilities", Icons.Rounded.HourglassTop, AccentOrange) {
                launchTimer(context)
            },
            ControlItem("screen_record", "Screen Recording", "Utilities", Icons.Rounded.RadioButtonChecked, DestructiveRed, true, recording) {
                com.glasslauncher.record.ScreenRecording.toggle(context)
            }.takeIf { com.glasslauncher.BuildConfig.SCREEN_RECORDING },
            ControlItem("qr", "Scan QR Code", "Utilities", Icons.Rounded.QrCodeScanner, AccentBlue) {
                scanQr(context)
            },
            ControlItem("camera", "Camera", "Utilities", Icons.Rounded.PhotoCamera, AccentPurple) {
                context.startSafely(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            },
            ControlItem("notes", "Quick Notes", "Utilities", Icons.Rounded.EditNote, AccentYellow) {
                context.startSafely(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
            },

            // Display & Audio
            ControlItem("dark_mode", "Dark Mode", "Display & Audio", Icons.Rounded.DarkMode, AccentPurple) {
                context.startSafely(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            },
            ControlItem("rotation", "Rotation Lock", "Display & Audio", Icons.Rounded.ScreenLockRotation, DestructiveRed, true, controls.rotationLocked) {
                controls.toggleRotationLock()
            },
            ControlItem("silent", "Silent Mode", "Display & Audio", Icons.Rounded.NotificationsOff, DestructiveRed, true, controls.silent) {
                controls.toggleSilent()
            },
            ControlItem("dnd", "Do Not Disturb", "Display & Audio", Icons.Rounded.DoNotDisturbOn, AccentPurple, true, controls.dndOn) {
                controls.toggleDnd()
            },
            ControlItem("cast", "Screen Mirroring", "Display & Audio", Icons.Rounded.Cast, AccentBlue) {
                context.startSafely(Intent(Settings.ACTION_CAST_SETTINGS))
            },
            ControlItem("battery_saver", "Low Power Mode", "Display & Audio", Icons.Rounded.BatterySaver, AccentYellow) {
                context.startSafely(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            },

            // System Shortcuts
            ControlItem("settings", "System Settings", "System", Icons.Rounded.Settings, Color(0xFF8E8E93)) {
                context.startSafely(Intent(Settings.ACTION_SETTINGS))
            },
            ControlItem("wallet", "Google Wallet", "System", Icons.Rounded.AccountBalanceWallet, AccentBlue) {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.walletnfcrel")
                    ?: Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS)
                context.startSafely(intent)
            },
            ControlItem("accessibility", "Accessibility", "System", Icons.Rounded.Accessibility, AccentGreen) {
                context.startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )
    }

    val filtered = remember(query, allControls) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allControls
        else allControls.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { it.category }
    }

    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss, alpha = 0.35f)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .liquidGlass(SmoothShape(36.dp), 36.dp, GlassStyle.Regular)
                .consumeTaps()
                .padding(20.dp),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Controls Gallery", style = LargeTitleStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    Text("Add and launch Android controls", style = PanelCaptionStyle)
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Search Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color.White.copy(alpha = 0.10f), Capsule)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = PanelBodyStyle.copy(fontSize = 16.sp),
                    cursorBrush = SolidColor(AccentBlue),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text("Search Controls", style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.45f)))
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Controls List
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                grouped.forEach { (category, items) ->
                    item(key = "cat_$category") {
                        Text(category.uppercase(), style = WidgetCaptionStyle, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    }
                    items(items, key = { it.id }) { control ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.07f), SmoothShape(16.dp))
                                .clickable {
                                    AppleTaptic.medium(context)
                                    control.action(context)
                                    if (!control.isToggle) onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .background(
                                        if (control.isActive) control.iconColor else control.iconColor.copy(alpha = 0.25f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    control.icon,
                                    contentDescription = control.title,
                                    tint = if (control.isActive) Color.White else control.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(control.title, style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    if (control.isToggle) (if (control.isActive) "On" else "Off") else "Tap to open",
                                    style = PanelCaptionStyle
                                )
                            }
                            Box(
                                Modifier
                                    .background(
                                        if (control.isActive) control.iconColor else Color.White.copy(alpha = 0.12f),
                                        Capsule
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (control.isToggle) (if (control.isActive) "Active" else "Inactive") else "Open",
                                    style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun launchCalculator(context: Context) {
    val intents = listOf(
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR),
    )
    for (i in intents) {
        if (context.startSafely(i)) return
    }
    val pm = context.packageManager
    val packages = listOf(
        "com.google.android.calculator",
        "com.android.calculator2",
        "com.sec.android.app.popupcalculator",
        "com.miui.calculator",
        "com.oneplus.calculator",
        "com.coloros.calculator",
        "com.huawei.calculator",
        "com.asus.calculator"
    )
    for (pkg in packages) {
        val launch = pm.getLaunchIntentForPackage(pkg)
        if (launch != null && context.startSafely(launch)) return
    }
    try {
        val all = pm.getInstalledApplications(0)
        for (app in all) {
            val label = pm.getApplicationLabel(app).toString()
            if (app.packageName.contains("calc", ignoreCase = true) || label.contains("calc", ignoreCase = true)) {
                val launch = pm.getLaunchIntentForPackage(app.packageName)
                if (launch != null && context.startSafely(launch)) return
            }
        }
    } catch (_: Exception) {}
    Toast.makeText(context, "No Calculator app installed", Toast.LENGTH_SHORT).show()
}

fun launchAlarm(context: Context) {
    if (context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS))) return
    if (context.startSafely(Intent(AlarmClock.ACTION_SET_ALARM))) return
    val pm = context.packageManager
    val packages = listOf(
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.android.deskclock",
        "com.miui.clock",
        "com.oneplus.deskclock",
        "com.coloros.alarmclock"
    )
    for (pkg in packages) {
        val launch = pm.getLaunchIntentForPackage(pkg)
        if (launch != null && context.startSafely(launch)) return
    }
    try {
        val all = pm.getInstalledApplications(0)
        for (app in all) {
            val label = pm.getApplicationLabel(app).toString()
            if (app.packageName.contains("clock", ignoreCase = true) || label.contains("clock", ignoreCase = true) || label.contains("alarm", ignoreCase = true)) {
                val launch = pm.getLaunchIntentForPackage(app.packageName)
                if (launch != null && context.startSafely(launch)) return
            }
        }
    } catch (_: Exception) {}
    Toast.makeText(context, "No Clock / Alarm app found", Toast.LENGTH_SHORT).show()
}

fun launchTimer(context: Context) {
    if (context.startSafely(Intent(AlarmClock.ACTION_SHOW_TIMERS))) return
    if (context.startSafely(Intent(AlarmClock.ACTION_SET_TIMER))) return
    launchAlarm(context)
}
