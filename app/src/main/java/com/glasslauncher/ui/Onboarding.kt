package com.glasslauncher.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import android.os.Build
import android.widget.Toast
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

private enum class SetupStep { Language, DefaultHome, Tour, Permissions, Done }

/**
 * First-run setup & interactive slidable feature tour with live visual snippets.
 * Fully responsive and optimized for phones, tablets, and foldables.
 */
@Composable
fun Onboarding(step: Int, onStep: (Int) -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val locationPermissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val activityPermissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val homeRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }

    val steps = SetupStep.entries
    val currentStep = steps[step.coerceIn(0, steps.lastIndex)]

    fun next() {
        if (step < steps.lastIndex) onStep(step + 1) else onFinish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .consumeTaps(),
        contentAlignment = Alignment.Center
    ) {
        if (currentStep == SetupStep.Tour) {
            FeatureTourPager(
                onFinishTour = ::next,
                onClose = ::next
            )
        } else {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) },
                label = "onboarding_permission",
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
            ) { target ->
                when (target) {
                    SetupStep.Language -> LanguageCard(onContinue = ::next)
                    SetupStep.DefaultHome -> DefaultHomeCard(
                        isHome = Permissions.isDefaultHome(context),
                        onSetDefault = {
                            Toast.makeText(context, "👉 Select 'Glass OS Launcher' as default home app", Toast.LENGTH_LONG).show()
                            homeRequest.launch(Permissions.homeRoleIntent(context))
                        },
                        onContinue = ::next,
                    )
                    SetupStep.Tour -> {}
                    SetupStep.Permissions -> PermissionsCard(
                        onContinue = ::next,
                        onHome = {
                            Toast.makeText(context, "👉 Select 'Glass OS Launcher' as default home app", Toast.LENGTH_LONG).show()
                            homeRequest.launch(Permissions.homeRoleIntent(context))
                        },
                        onNotifications = {
                            Toast.makeText(context, "👉 Find 'Glass OS Launcher' in the list and switch toggle ON", Toast.LENGTH_LONG).show()
                            Permissions.open(context, Permissions.notificationAccessIntent(context), android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onUsage = {
                            Toast.makeText(context, "👉 Select 'Glass OS Launcher' and turn ON 'Permit usage access'", Toast.LENGTH_LONG).show()
                            Permissions.open(context, Permissions.usageAccessIntent(context), android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        onWeather = {
                            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        },
                        onCalendar = {
                            permissionRequest.launch(Manifest.permission.READ_CALENDAR)
                        },
                        onControls = {
                            Toast.makeText(context, "👉 Turn ON 'Allow modifying system settings' for Glass OS Launcher", Toast.LENGTH_LONG).show()
                            Permissions.openWriteSettings(context)
                        },
                        onDnd = {
                            Toast.makeText(context, "👉 Select 'Glass OS Launcher' and tap 'Allow'", Toast.LENGTH_LONG).show()
                            context.startSafely(Permissions.dndIntent())
                        },
                        onActivity = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                activityPermissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                        },
                        isHome = Permissions.isDefaultHome(context),
                        isNotifications = Permissions.hasNotificationAccess(context),
                        isUsage = Permissions.hasUsageAccess(context),
                        isWeather = Permissions.hasLocation(context),
                        isCalendar = Permissions.hasCalendar(context),
                        isControls = Permissions.canWriteSettings(context),
                        isDnd = Permissions.canControlDnd(context),
                        isActivity = Permissions.hasActivityRecognition(context),
                        notificationsSupported = Permissions.notificationsSupported,
                        refresh = refresh
                    )
                    SetupStep.Done -> SetupCard(
                        icon = Icons.Rounded.CheckCircle,
                        title = "You're all set!",
                        body = "Swipe down for Spotlight Search, from top-right for Control Centre, right for Today widgets, and left for the App Library. Long press wallpaper to edit.",
                        primary = "Enter Glass OS",
                        onPrimary = onFinish,
                    )
                }
            }
        }
    }
}

/**
 * Slidable horizontal feature tour showcasing the 5 standout features with visual mockups/snippets.
 * Responsive for phone, tablet, and foldable layouts.
 */
@Composable
internal fun FeatureTourPager(
    onFinishTour: () -> Unit,
    onClose: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val cardMaxWidth = if (isTablet) 540.dp else 400.dp
    val cardPadding = if (isTablet) 28.dp else 20.dp

    val tourSlides = remember {
        listOf(
            TourSlide(
                id = "dynamic_island",
                tag = "FEATURE 1 OF 5",
                title = "Interactive Dynamic Island",
                description = "Real-time active phone calls, audio spectrum visualizer, and timers right at the top. Expands on tap and auto-collapses when idle.",
                snippet = { DynamicIslandSnippet() }
            ),
            TourSlide(
                id = "control_centre",
                tag = "FEATURE 2 OF 5",
                title = "Fluid Control Centre",
                description = "Swipe down from the top-right corner. Features instant 1-tap toggles for Wi-Fi, Mobile Data, Bluetooth, Brightness, Volume, and Flashlight with glass controls.",
                snippet = { ControlCentreSnippet() }
            ),
            TourSlide(
                id = "smart_stacks",
                tag = "FEATURE 3 OF 5",
                title = "Smart Stacks & Vertical Scroll",
                description = "Stack 2 or more widgets and swipe vertically to flip through them. Includes real-time internal storage monitoring, weather forecast, analog clock, and 3D angle size slider.",
                snippet = { SmartStacksSnippet() }
            ),
            TourSlide(
                id = "lock_screen",
                tag = "FEATURE 4 OF 5",
                title = "Tinted Lock Screen & Wallpapers",
                description = "Swipe down from top-left for Notification Centre. Choose separate Lock Screen & Home Screen wallpapers, glass blur intensity, and clock typography.",
                snippet = { LockScreenSnippet() }
            ),
            TourSlide(
                id = "spotlight_search",
                tag = "FEATURE 5 OF 5",
                title = "Spotlight Search & App Library",
                description = "Swipe down anywhere on Home Screen for Spotlight search with inline math calculations, web shortcuts, and fast contact dialing. Swipe left for auto-categorized App Library.",
                snippet = { SpotlightSearchSnippet() }
            )
        )
    }

    val pagerState = rememberPagerState { tourSlides.size }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = cardMaxWidth)
                .fillMaxWidth()
                .liquidGlass(SmoothShape(36.dp), 36.dp, GlassStyle.Regular)
                .padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar with Close Pill
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Glass OS",
                    style = PanelBodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                )
                Box(
                    Modifier
                        .size(30.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Slidable Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) { page ->
                val slide = tourSlides[page]
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feature Tag
                    Text(
                        slide.tag,
                        style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        slide.title,
                        style = LargeTitleStyle.copy(fontSize = if (isTablet) 24.sp else 21.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(14.dp))

                    // Visual Snippet Card
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (isTablet) 180.dp else 156.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        slide.snippet()
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        slide.description,
                        style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.82f), fontSize = if (isTablet) 15.sp else 13.5.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Pager Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(tourSlides.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(if (isSelected) 22.dp else 7.dp, label = "dot_width")
                    Box(
                        Modifier
                            .height(7.dp)
                            .width(dotWidth)
                            .background(
                                if (isSelected) AccentBlue else Color.White.copy(alpha = 0.28f),
                                Capsule
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Actions: Skip & Next / Continue
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Skip Tour",
                    style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.65f), fontSize = 15.sp),
                    modifier = Modifier
                        .clickable(onClick = onFinishTour)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                val isLast = pagerState.currentPage == tourSlides.lastIndex
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(Capsule)
                        .background(AccentBlue)
                        .clickable {
                            if (isLast) {
                                onFinishTour()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLast) "Get Started" else "Next",
                        style = PanelBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

private data class TourSlide(
    val id: String,
    val tag: String,
    val title: String,
    val description: String,
    val snippet: @Composable () -> Unit,
)

// Visual Snippet 1: Interactive Dynamic Island
@Composable
private fun DynamicIslandSnippet() {
    val infinite = rememberInfiniteTransition(label = "wave")
    val h1 by infinite.animateFloat(initialValue = 6f, targetValue = 22f, animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "h1")
    val h2 by infinite.animateFloat(initialValue = 20f, targetValue = 8f, animationSpec = infiniteRepeatable(tween(540, easing = LinearEasing), RepeatMode.Reverse), label = "h2")
    val h3 by infinite.animateFloat(initialValue = 10f, targetValue = 26f, animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse), label = "h3")
    val h4 by infinite.animateFloat(initialValue = 24f, targetValue = 6f, animationSpec = infiniteRepeatable(tween(490, easing = LinearEasing), RepeatMode.Reverse), label = "h4")
    val h5 by infinite.animateFloat(initialValue = 8f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(390, easing = LinearEasing), RepeatMode.Reverse), label = "h5")

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .clip(Capsule)
                .background(Color(0xFF141416))
                .border(1.dp, Color.White.copy(alpha = 0.22f), Capsule)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(AccentGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.width(3.dp).height(h1.dp).background(AccentGreen, Capsule))
                    Box(Modifier.width(3.dp).height(h2.dp).background(AccentGreen, Capsule))
                    Box(Modifier.width(3.dp).height(h3.dp).background(AccentGreen, Capsule))
                    Box(Modifier.width(3.dp).height(h4.dp).background(AccentGreen, Capsule))
                    Box(Modifier.width(3.dp).height(h5.dp).background(AccentGreen, Capsule))
                }
                Text("03:42", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Box(
                    Modifier
                        .size(26.dp)
                        .background(Color(0xFFFF453A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
        Text("Active Call • Tap to open dialer", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp))
    }
}

// Visual Snippet 2: Fluid Control Centre
@Composable
private fun ControlCentreSnippet() {
    Row(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 2x2 Network block
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                .padding(6.dp)
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniToggleIcon(Icons.Rounded.Wifi, AccentBlue)
                    MiniToggleIcon(Icons.Rounded.Bluetooth, AccentBlue)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniToggleIcon(Icons.Rounded.SignalCellularAlt, AccentGreen)
                    MiniToggleIcon(Icons.Rounded.AirplanemodeActive, AccentOrange)
                }
            }
        }

        // Sliders: Brightness & Volume
        MiniSliderCapsule(Icons.Rounded.LightMode, 0.75f)
        MiniSliderCapsule(Icons.Rounded.VolumeUp, 0.50f)

        // 2 Quick action buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1C40F))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5856D6))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Nightlight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MiniToggleIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun MiniSliderCapsule(icon: ImageVector, fill: Float) {
    Box(
        Modifier
            .width(36.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(fill)
                .background(Color.White.copy(alpha = 0.85f))
        )
        Icon(
            icon,
            contentDescription = null,
            tint = if (fill > 0.4f) Color.DarkGray else Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(18.dp)
        )
    }
}

// Visual Snippet 3: Smart Stacks & 2-Widget Vertical Scroll
@Composable
private fun SmartStacksSnippet() {
    val stackPagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    // Smooth periodic vertical flip animation demo
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            val nextPage = (stackPagerState.currentPage + 1) % 2
            stackPagerState.animateScrollToPage(
                nextPage,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.82f, stiffness = 160f)
            )
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical Scrollable Widget Stack Container (2 squarish widgets on top of each other)
        Box(
            Modifier
                .width(184.dp)
                .height(118.dp)
                .clip(SmoothShape(22.dp))
                .border(1.dp, Color.White.copy(alpha = 0.28f), SmoothShape(22.dp))
        ) {
            VerticalPager(
                state = stackPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // Widget 1: Storage Monitor Widget
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(10.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Storage", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                Text("128 GB Free", style = TextStyle(fontFamily = AppFont, color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
                            }
                            // Storage Bar
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(Capsule)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(Modifier.weight(0.45f).fillMaxHeight().background(AccentBlue))
                                Box(Modifier.weight(0.20f).fillMaxHeight().background(Color(0xFFAF52DE)))
                                Box(Modifier.weight(0.15f).fillMaxHeight().background(AccentOrange))
                                Box(Modifier.weight(0.20f).fillMaxHeight().background(Color.Transparent))
                            }
                            Text("128 GB of 256 GB used", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.75f), fontSize = 10.5.sp))
                        }
                    }
                } else {
                    // Widget 2: Weather Widget
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2C5E8A).copy(alpha = 0.85f))
                            .padding(10.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weather", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                Text("San Francisco", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("24°", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold))
                            }
                            Text("Sunny • H:26° L:16°", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // Right-Side Vertical Pagination Dots
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(2) { idx ->
                val isActive = stackPagerState.currentPage == idx
                val dotHeight by animateDpAsState(if (isActive) 14.dp else 5.dp, label = "vdot_height")
                Box(
                    Modifier
                        .width(5.dp)
                        .height(dotHeight)
                        .background(if (isActive) AccentBlue else Color.White.copy(alpha = 0.35f), Capsule)
                        .clickable {
                            scope.launch { stackPagerState.animateScrollToPage(idx) }
                        }
                )
            }
        }
    }
}

// Visual Snippet 4: Lock Screen & Dual Wallpapers
@Composable
private fun LockScreenSnippet() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Thursday, October 31", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.7f), fontSize = 10.5.sp))
            Text("09:41", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold))
        }

        // Glass Notification Pill
        Box(
            Modifier
                .widthIn(max = 240.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Messages • 2 new", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Text("Team update ready for review", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.8f), fontSize = 9.5.sp), maxLines = 1)
                }
            }
        }

        // Bottom Torch & Camera buttons
        Row(Modifier.width(180.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Text("Dual Wallpaper Styles", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp))
            Box(Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// Visual Snippet 5: Spotlight Search & App Library
@Composable
private fun SpotlightSearchSnippet() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search Pill
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(Capsule)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), Capsule)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Search apps, math, contacts...", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp))
        }

        // Calculation & Quick Actions
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Calculation Card
            Box(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).background(AccentOrange, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("48 × 25", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp))
                        Text("= 1,200", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }

            // App Library Card
            Box(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.GridView, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("App Library", style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        Text("Auto Categories", style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.6f), fontSize = 9.5.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(onContinue: () -> Unit) {
    val context = LocalContext.current
    val currentLang by com.glasslauncher.data.LanguageHelper.currentLanguage.collectAsState()
    CardFrame {
        IconBubble(Icons.Rounded.Tune)
        Text(com.glasslauncher.data.LanguageHelper.string("welcome"), style = LargeTitleStyle.copy(fontSize = 24.sp), textAlign = TextAlign.Center)
        Text(com.glasslauncher.data.LanguageHelper.string("choose_language"), style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 14.5.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .clip(SmoothShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .verticalScroll(rememberScrollState())
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            com.glasslauncher.data.LanguageHelper.languages.forEach { lang ->
                val selected = currentLang == lang.tag
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(SmoothShape(12.dp))
                        .background(if (selected) AccentBlue.copy(alpha = 0.35f) else Color.Transparent)
                        .clickable {
                            com.glasslauncher.data.LanguageHelper.setLanguage(context, lang.tag)
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        lang.nativeName,
                        style = TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        lang.displayName,
                        style = TextStyle(fontFamily = AppFont, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    )
                    if (selected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton(com.glasslauncher.data.LanguageHelper.string("continue_btn"), onContinue)
    }
}

@Composable
private fun DefaultHomeCard(
    isHome: Boolean,
    onSetDefault: () -> Unit,
    onContinue: () -> Unit,
) {
    var requestedHome by remember { mutableStateOf(false) }

    LaunchedEffect(isHome, requestedHome) {
        if (requestedHome && isHome) {
            delay(700)
            onContinue()
        }
    }

    CardFrame {
        IconBubble(Icons.Rounded.Home)
        Text(
            com.glasslauncher.data.LanguageHelper.string("set_default_home_title"),
            style = LargeTitleStyle.copy(fontSize = 24.sp),
            textAlign = TextAlign.Center
        )
        Text(
            if (isHome) com.glasslauncher.data.LanguageHelper.string("default_home_set_desc")
            else com.glasslauncher.data.LanguageHelper.string("set_default_home_desc"),
            style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 14.5.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        if (isHome) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(Capsule)
                    .background(AccentGreen.copy(alpha = 0.18f))
                    .border(1.dp, AccentGreen.copy(alpha = 0.35f), Capsule)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                Text(
                    com.glasslauncher.data.LanguageHelper.string("default_home_set"),
                    style = PanelBodyStyle.copy(color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(com.glasslauncher.data.LanguageHelper.string("continue_tour_btn"), onContinue)
        } else {
            PrimaryButton(com.glasslauncher.data.LanguageHelper.string("set_default_home_btn")) {
                requestedHome = true
                onSetDefault()
            }
        }
    }
}

@Composable
fun SetDefaultHomeModal(
    onSetDefault: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .consumeTaps(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardFrame {
                IconBubble(Icons.Rounded.Home)
                Text(
                    com.glasslauncher.data.LanguageHelper.string("set_default_home_title"),
                    style = LargeTitleStyle.copy(fontSize = 24.sp),
                    textAlign = TextAlign.Center
                )
                Text(
                    com.glasslauncher.data.LanguageHelper.string("not_default_prompt_desc"),
                    style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 14.5.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                PrimaryButton(com.glasslauncher.data.LanguageHelper.string("set_default_home_btn"), onSetDefault)
            }
        }
    }
}

@Composable
private fun SetupCard(
    icon: ImageVector,
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    granted: Boolean = false,
    onSkip: (() -> Unit)? = null,
) {
    CardFrame {
        IconBubble(icon)
        Text(title, style = LargeTitleStyle.copy(fontSize = 24.sp), textAlign = TextAlign.Center)
        Text(body, style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 14.5.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                Text("Allowed", style = PanelBodyStyle.copy(color = AccentGreen), modifier = Modifier.padding(start = 6.dp))
            }
            PrimaryButton("Continue", onSkip ?: onPrimary)
        } else {
            PrimaryButton(primary, onPrimary)
            if (onSkip != null) SecondaryButton("Not Now", onSkip)
        }
    }
}

@Composable
private fun PermissionsCard(
    onContinue: () -> Unit,
    onHome: () -> Unit,
    onNotifications: () -> Unit,
    onUsage: () -> Unit,
    onWeather: () -> Unit,
    onCalendar: () -> Unit,
    onControls: () -> Unit,
    onDnd: () -> Unit,
    onActivity: () -> Unit,
    isHome: Boolean,
    isNotifications: Boolean,
    isUsage: Boolean,
    isWeather: Boolean,
    isCalendar: Boolean,
    isControls: Boolean,
    isDnd: Boolean,
    isActivity: Boolean,
    notificationsSupported: Boolean,
    refresh: Int,
) {
    CardFrame {
        IconBubble(Icons.Rounded.Security)
        Text("Permissions & Setup", style = LargeTitleStyle.copy(fontSize = 24.sp), textAlign = TextAlign.Center)
        Text(
            "Configure permissions below to enable smart widgets, notification badges, dynamic island, and glass controls.",
            style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 13.5.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionSetupRow(
                icon = Icons.Rounded.Home,
                iconTint = AccentBlue,
                title = "Default Home Launcher",
                desc = "Opens Glass OS on Home tap or swipe up",
                guide = "Select 'Glass OS Launcher' → Set as default",
                granted = isHome,
                onAllow = onHome
            )
            if (notificationsSupported) {
                PermissionSetupRow(
                    icon = Icons.Rounded.Notifications,
                    iconTint = DestructiveRed,
                    title = "Notification Badges & Media",
                    desc = "App badges, lock screen media & Island",
                    guide = "Find 'Glass OS Launcher' → Switch ON",
                    granted = isNotifications,
                    onAllow = onNotifications
                )
            }
            PermissionSetupRow(
                icon = Icons.Rounded.HourglassTop,
                iconTint = AccentPurple,
                title = "Screen Time & Wellbeing",
                desc = "Live app usage & screen time widget",
                guide = "Select 'Glass OS Launcher' → Permit access",
                granted = isUsage,
                onAllow = onUsage
            )
            PermissionSetupRow(
                icon = Icons.Rounded.WbSunny,
                iconTint = AccentOrange,
                title = "Weather & Forecast",
                desc = "Local temperature & hourly rain forecast",
                guide = "Choose 'While using the app'",
                granted = isWeather,
                onAllow = onWeather
            )
            PermissionSetupRow(
                icon = Icons.Rounded.CalendarMonth,
                iconTint = DestructiveRed,
                title = "Calendar Events",
                desc = "Syncs agenda & upcoming schedules",
                guide = "Tap 'Allow' to grant calendar access",
                granted = isCalendar,
                onAllow = onCalendar
            )
            PermissionSetupRow(
                icon = Icons.Rounded.Tune,
                iconTint = AccentBlue,
                title = "Control Centre (Brightness)",
                desc = "Smooth brightness & rotation sliders",
                guide = "Turn ON 'Allow modifying system settings'",
                granted = isControls,
                onAllow = onControls
            )
            PermissionSetupRow(
                icon = Icons.Rounded.DoNotDisturbOn,
                iconTint = AccentPurple,
                title = "Focus & Do Not Disturb",
                desc = "1-tap Focus mode toggle in Control Centre",
                guide = "Select 'Glass OS Launcher' → Allow",
                granted = isDnd,
                onAllow = onDnd
            )
            PermissionSetupRow(
                icon = Icons.Rounded.DirectionsRun,
                iconTint = AccentGreen,
                title = "Fitness & Step Tracker",
                desc = "Counts daily steps and active distance",
                guide = "Tap 'Allow' to track physical activity",
                granted = isActivity,
                onAllow = onActivity
            )
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton("Continue to Glass OS", onContinue)
    }
}

@Composable
private fun PermissionSetupRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String,
    guide: String,
    granted: Boolean,
    onAllow: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(iconTint.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = PanelBodyStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold))
            Text(desc, style = PanelCaptionStyle.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("👉 $guide", style = PanelCaptionStyle.copy(fontSize = 10.sp, color = AccentBlue.copy(alpha = 0.95f), fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (granted) {
            Row(
                Modifier
                    .clip(Capsule)
                    .background(AccentGreen.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Allowed", style = PanelCaptionStyle.copy(fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold))
            }
        } else {
            Box(
                Modifier
                    .clip(Capsule)
                    .background(AccentBlue)
                    .clickable(onClick = onAllow)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Allow", style = PanelBodyStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}

@Composable
private fun ControlsCard(canWrite: Boolean, canDnd: Boolean, onWrite: () -> Unit, onDnd: () -> Unit, onNext: () -> Unit) {
    CardFrame {
        IconBubble(Icons.Rounded.Tune)
        Text("Control Centre Access", style = LargeTitleStyle.copy(fontSize = 24.sp), textAlign = TextAlign.Center)
        Text(
            "Lets the brightness slider, screen rotation, and Focus (Do Not Disturb) update device system settings.",
            style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 14.5.sp),
            textAlign = TextAlign.Center,
        )
        PermissionRow("Modify system settings", canWrite, onWrite)
        PermissionRow("Do Not Disturb access", canDnd, onDnd)
        PrimaryButton("Continue", onNext)
    }
}

@Composable
fun PermissionRow(label: String, granted: Boolean, onAllow: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(enabled = !granted, onClick = onAllow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = PanelBodyStyle.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
        if (granted) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Allowed", tint = AccentGreen, modifier = Modifier.size(22.dp))
        } else {
            Text("Allow", style = PanelBodyStyle.copy(fontSize = 15.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun CardFrame(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    Column(
        Modifier
            .widthIn(max = if (isTablet) 520.dp else 400.dp)
            .fillMaxWidth()
            .liquidGlass(SmoothShape(36.dp), 36.dp, GlassStyle.Regular)
            .padding(if (isTablet) 32.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun IconBubble(icon: ImageVector) {
    Box(Modifier.size(68.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(AccentBlue, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PanelBodyStyle.copy(fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PanelBodyStyle.copy(fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium))
    }
}
