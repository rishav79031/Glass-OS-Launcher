package com.glasslauncher.ui
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.ShoppingBag

import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.data.AppEntry
import com.glasslauncher.data.AppGroup
import kotlin.math.max
import kotlin.math.roundToInt

@Immutable
data class IconMenu(val app: AppEntry, val anchor: Rect)

/** One row of the long-press menu. */
@Immutable
data class MenuEntry(val label: String, val icon: ImageVector?, val destructive: Boolean = false, val onClick: () -> Unit)

enum class AlertRole { Default, Destructive, Cancel }

@Immutable
data class AlertButton(val label: String, val role: AlertRole, val onClick: () -> Unit)

@Composable
fun Scrim(onDismiss: () -> Unit, alpha: Float = 0.18f) {
    val dismiss by rememberUpdatedState(onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .pointerInput(Unit) { detectTapGestures { dismiss() } },
    )
}

/** Spotlight: Suggestions when empty, then matching apps and a web search for the same text. */
@Composable
fun SearchOverlay(
    apps: List<AppEntry>,
    suggestions: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onLaunch: (AppEntry, Rect) -> Unit,
    onDismiss: () -> Unit,
    showSuggestions: Boolean = true,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val results = remember(apps, query) {
        val text = query.trim()
        if (text.isEmpty()) {
            emptyList()
        } else {
            apps.filter { it.label.contains(text, ignoreCase = true) }
                .sortedBy { if (it.label.startsWith(text, ignoreCase = true)) 0 else 1 }
                .take(12)
        }
    }
    val calcResult = remember(query) { MathAndConverter.evaluate(query) }
    val contacts = remember(query) { ContactsSearchHelper.searchContacts(context, query) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun searchWeb() {
        val text = query.trim()
        if (text.isEmpty()) return
        val opened = context.startSafely(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text)) ||
            context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(text))))
        if (opened) onDismiss()
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Scrim(onDismiss, alpha = 0.22f)
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 20.dp, start = 14.dp, end = 14.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                // Full height, so the A–Z list can run to the bottom of the screen (or the keyboard).
                .fillMaxHeight()
                .padding(bottom = 12.dp)
                .consumeTaps(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .liquidGlass(Capsule, style = GlassStyle.Regular)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = PanelBodyStyle.copy(fontSize = 18.sp),
                    cursorBrush = SolidColor(AccentBlue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        val first = results.firstOrNull()
                        if (first != null) onLaunch(first, Rect.Zero) else searchWeb()
                    }),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) {
                                Text("Search", style = PanelBodyStyle.copy(fontSize = 18.sp, color = Color.White.copy(alpha = 0.5f)))
                            }
                            field()
                        }
                    },
                )
            }
            if (query.isBlank()) {
                // Tapping Search shows suggestions first, then every app A to Z. Until there's usage to suggest from,
                // the first apps stand in.
                val shown = if (showSuggestions) suggestions.ifEmpty { apps.take(4) } else emptyList()
                if (shown.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .liquidGlass(SmoothShape(26.dp), 26.dp, GlassStyle.Regular)
                            .padding(16.dp),
                    ) {
                        Text("SUGGESTIONS", style = WidgetCaptionStyle)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            shown.take(4).forEach { app ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable { onLaunch(app, Rect.Zero) }) {
                                    icons[app.key]?.let { Image(it, contentDescription = null, modifier = Modifier.size(56.dp)) }
                                    Text(app.label, style = IconLabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
                AlphabeticalApps(apps, icons, onLaunch, Modifier.weight(1f))
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .liquidGlass(SmoothShape(26.dp), 26.dp, GlassStyle.Regular)
                        .padding(vertical = 8.dp),
                ) {
                    LazyColumn(Modifier.heightIn(max = 460.dp)) {
                        if (calcResult != null) {
                            item(key = "calc") {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val toCopy = when (calcResult) {
                                                is SearchCalculation.MathResult -> calcResult.result
                                                is SearchCalculation.ConversionResult -> calcResult.formatted
                                            }
                                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("Calculation", toCopy))
                                            Toast.makeText(context, "Copied $toCopy", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .background(if (calcResult is SearchCalculation.ConversionResult) AccentGreen else AccentOrange, SmoothShape(12.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (calcResult is SearchCalculation.ConversionResult) Icons.Rounded.SwapHoriz else Icons.Rounded.Calculate,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        when (calcResult) {
                                            is SearchCalculation.MathResult -> {
                                                Text(calcResult.expression, style = PanelCaptionStyle)
                                                Text("= ${calcResult.result}", style = LargeTitleStyle.copy(fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                                            }
                                            is SearchCalculation.ConversionResult -> {
                                                Text("${calcResult.fromValue} ${calcResult.fromUnit} =", style = PanelCaptionStyle)
                                                Text(calcResult.formatted, style = LargeTitleStyle.copy(fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                                            }
                                        }
                                    }
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        if (contacts.isNotEmpty()) {
                            item(key = "contacts_header") {
                                Text(
                                    "CONTACTS",
                                    style = WidgetCaptionStyle,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(contacts, key = { "contact_${it.id}" }) { contact ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(42.dp)
                                            .background(Color(0xFF636366), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val initials = contact.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                                        Text(initials.ifEmpty { "?" }, style = androidx.compose.ui.text.TextStyle(fontFamily = AppFont, color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(contact.name, style = PanelBodyStyle, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text(contact.number, style = PanelCaptionStyle, maxLines = 1)
                                    }
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .background(AccentGreen, CircleShape)
                                            .clickable {
                                                ContactsSearchHelper.makeCall(context, contact.number)
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .background(AccentBlue, CircleShape)
                                            .clickable {
                                                ContactsSearchHelper.sendMessage(context, contact.number)
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Chat, contentDescription = "Message", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        items(results, key = { it.key }) { app ->
                            ResultRow(onClick = { onLaunch(app, Rect.Zero) }) {
                                val icon = icons[app.key]
                                if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(40.dp)) else Spacer(Modifier.size(40.dp))
                                Text(app.label, style = PanelBodyStyle, modifier = Modifier.padding(start = 14.dp))
                            }
                        }
                        item(key = "web") {
                            ResultRow(onClick = ::searchWeb) {
                                Image(
                                    painter = painterResource(com.glasslauncher.R.drawable.ic_ios_browser),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(SmoothShape(10.dp))
                                )
                                Text("Search Web for “${query.trim()}”", style = PanelBodyStyle, modifier = Modifier.padding(start = 14.dp))
                            }
                        }
                        item(key = "maps") {
                            ResultRow(onClick = {
                                val text = query.trim()
                                if (text.isNotEmpty()) {
                                    val uri = Uri.parse("geo:0,0?q=" + Uri.encode(text))
                                    context.startSafely(Intent(Intent.ACTION_VIEW, uri))
                                    onDismiss()
                                }
                            }) {
                                Image(
                                    painter = painterResource(com.glasslauncher.R.drawable.ic_ios_maps),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(SmoothShape(10.dp))
                                )
                                Text("Search Maps for “${query.trim()}”", style = PanelBodyStyle, modifier = Modifier.padding(start = 14.dp))
                            }
                        }
                        item(key = "store") {
                            ResultRow(onClick = {
                                val text = query.trim()
                                if (text.isNotEmpty()) {
                                    val marketUri = Uri.parse("market://search?q=" + Uri.encode(text))
                                    val webUri = Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(text) + "&c=apps")
                                    if (!context.startSafely(Intent(Intent.ACTION_VIEW, marketUri))) {
                                        context.startSafely(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                    onDismiss()
                                }
                            }) {
                                Image(
                                    painter = painterResource(com.glasslauncher.R.drawable.ic_playstore),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(SmoothShape(10.dp))
                                )
                                Text("Search Play Store for “${query.trim()}”", style = PanelBodyStyle, modifier = Modifier.padding(start = 14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** An App Library category opened full size. */
@Composable
fun FolderOverlay(
    group: AppGroup,
    icons: Map<String, ImageBitmap>,
    look: IconLook,
    onLaunch: (AppEntry, Rect) -> Unit,
    onMenu: (AppEntry, Rect) -> Unit,
    onDismiss: () -> Unit,
) {
    val animScale = remember { Animatable(0.75f) }
    val animAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { animScale.animateTo(1f, spring(dampingRatio = 0.76f, stiffness = 300f)) }
        launch { animAlpha.animateTo(1f, tween(180)) }
    }
    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss, alpha = 0.30f)
        Column(
            Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = animScale.value
                    scaleY = animScale.value
                    alpha = animAlpha.value
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .consumeTaps(),
        ) {
            Text(group.title, style = LargeTitleStyle, modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(look.size + 36.dp),
                modifier = Modifier
                    .liquidGlass(SmoothShape(38.dp), 38.dp, GlassStyle.Regular)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(group.apps, key = { it.key }) { app ->
                    AppIcon(app, icons[app.key], look, onLaunch, onMenu, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/**
 * iOS context menu: the icon lifts in place over the blurred Home Screen, with Quick Actions and the
 * standard actions in a glass menu beside it.
 */
@Composable
fun IconMenuOverlay(
    menu: IconMenu,
    icon: ImageBitmap?,
    quickActions: List<MenuEntry>,
    actions: List<MenuEntry>,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val popScale = remember { Animatable(0.82f) }
    val popAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { popScale.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 320f)) }
        launch { popAlpha.animateTo(1f, tween(150)) }
    }
    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss, alpha = 0.20f)
        if (icon != null && !menu.anchor.isEmpty) {
            Image(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset(menu.anchor.left.roundToInt(), menu.anchor.top.roundToInt()) }
                    .size(with(density) { menu.anchor.width.toDp() })
                    .shadow(18.dp, RoundedCornerShape(22), clip = false)
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
        }
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                    alpha = popAlpha.value
                },
            content = {
                Column(
                    Modifier
                        .width(254.dp)
                        .liquidGlass(SmoothShape(24.dp), 24.dp, GlassStyle.Regular)
                        .consumeTaps(),
                ) {
                    if (quickActions.isNotEmpty()) {
                        MenuSection(quickActions)
                        Box(Modifier.fillMaxWidth().height(8.dp).background(Color.Black.copy(alpha = 0.18f)))
                    }
                    MenuSection(actions)
                }
            },
        ) { measurables, constraints ->
            val card = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val margin = 16.dp.roundToPx()
                val gap = 12.dp.roundToPx()
                val x = menu.anchor.left.roundToInt().coerceIn(margin, max(margin, constraints.maxWidth - card.width - margin))
                val below = menu.anchor.bottom.roundToInt() + gap
                val y = if (below + card.height + margin <= constraints.maxHeight) {
                    below
                } else {
                    (menu.anchor.top.roundToInt() - card.height - gap).coerceAtLeast(margin)
                }
                card.place(x, y)
            }
        }
    }
}

@Composable
fun FolderMenuOverlay(
    anchor: Rect,
    actions: List<MenuEntry>,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss, alpha = 0.10f)
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Column(
                    Modifier
                        .width(254.dp)
                        .liquidGlass(SmoothShape(24.dp), 24.dp, GlassStyle.Regular)
                        .consumeTaps(),
                ) {
                    MenuSection(actions)
                }
            },
        ) { measurables, constraints ->
            val card = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val margin = 16.dp.roundToPx()
                val gap = 12.dp.roundToPx()
                val x = anchor.left.roundToInt().coerceIn(margin, max(margin, constraints.maxWidth - card.width - margin))
                val below = anchor.bottom.roundToInt() + gap
                val y = if (below + card.height + margin <= constraints.maxHeight) {
                    below
                } else {
                    (anchor.top.roundToInt() - card.height - gap).coerceAtLeast(margin)
                }
                card.place(x, y)
            }
        }
    }
}

@Composable
private fun MenuSection(entries: List<MenuEntry>) {
    entries.forEachIndexed { index, entry ->
        if (index > 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(Separator))
        val color = if (entry.destructive) DestructiveRed else Color.White
        Row(
            Modifier.fillMaxWidth().clickable(onClick = entry.onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(entry.label, style = PanelBodyStyle.copy(color = color), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (entry.icon != null) Icon(entry.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

/** iOS alert: title, message, and full-width stacked buttons. */
@Composable
fun IosAlert(title: String, message: String?, buttons: List<AlertButton>, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss, alpha = 0.3f)
        Column(
            Modifier
                .align(Alignment.Center)
                .width(290.dp)
                .liquidGlass(SmoothShape(30.dp), 30.dp, GlassStyle.Regular)
                .consumeTaps(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = PanelBodyStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = if (message == null) 18.dp else 6.dp),
            )
            if (message != null) {
                Text(
                    message,
                    style = PanelCaptionStyle.copy(color = Color.White.copy(alpha = 0.85f)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                )
            }
            buttons.forEach { button ->
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Separator))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(0.dp))
                        .clickable(onClick = button.onClick)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        button.label,
                        style = PanelBodyStyle.copy(
                            fontSize = 17.sp,
                            color = if (button.role == AlertRole.Destructive) DestructiveRed else AccentBlue,
                            fontWeight = if (button.role == AlertRole.Cancel) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/** Quick note editor window for the Notes widget and launcher, with cursor at end of text and Keep integration. */
@Composable
fun NoteEditor(
    text: String,
    isNew: Boolean = false,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var textFieldValue by remember(text, isNew) {
        val initialText = if (isNew) "" else text
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length) // Cursor automatically at the end of text!
            )
        )
    }
    val focus = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    LaunchedEffect(text, isNew) {
        focus.requestFocus()
        kotlinx.coroutines.delay(60)
        textFieldValue = textFieldValue.copy(
            selection = TextRange(textFieldValue.text.length)
        )
    }

    val handleDone = {
        val clean = textFieldValue.text.trim()
        if (clean.isNotBlank()) {
            onSave(clean)
        }
        onDismiss()
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Scrim(onDismiss = handleDone, alpha = 0.35f)
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .liquidGlass(SmoothShape(26.dp), style = GlassStyle.Regular)
                .consumeTaps()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(AccentYellow.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isNew) "New Note" else "Edit Note", style = PanelTitleStyle.copy(fontSize = 18.sp))
                    Text(
                        "Saved on device • Tap share for Keep",
                        style = PanelCaptionStyle.copy(fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f))
                    )
                }

                // Delete button (when editing existing note)
                if (!isNew && onDelete != null) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30).copy(alpha = 0.22f))
                            .clickable {
                                onDelete()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Share / Send to Keep button
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable {
                            val content = textFieldValue.text.ifBlank { text }
                            openKeepNotes(context, content)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.IosShare,
                        contentDescription = "Send to Keep",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))

                // Done button
                Box(
                    Modifier
                        .clip(Capsule)
                        .background(AccentYellow)
                        .clickable(onClick = handleDone)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Done",
                        style = PanelBodyStyle.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .verticalScroll(scrollState)
                    .padding(12.dp)
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        if (isNew) "Type your note here..." else "Type your note here...",
                        style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.4f), fontSize = 14.5.sp),
                    )
                }
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    textStyle = PanelBodyStyle.copy(fontSize = 14.5.sp),
                    cursorBrush = SolidColor(AccentYellow),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        }
    }
}

/** Adds or edits a reminder entirely on-device with fluid Apple strike-through animation. */
@Composable
fun ReminderEditor(
    reminders: List<String>,
    onAdd: (String) -> Unit,
    onComplete: (Int) -> Unit,
    onDismiss: () -> Unit,
    initialIndex: Int? = null,
    onEdit: (Int, String) -> Unit = { _, _ -> },
) {
    // Default none selected when clicked
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var textFieldValue by remember {
        mutableStateOf(
            if (initialIndex != null && initialIndex in reminders.indices) {
                val t = reminders[initialIndex]
                TextFieldValue(t, selection = TextRange(t.length))
            } else {
                TextFieldValue("")
            }
        )
    }
    val focus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var completingIndices by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(Unit) { focus.requestFocus() }

    fun save() {
        val clean = textFieldValue.text.trim()
        if (clean.isNotBlank()) {
            val idx = editIndex
            if (idx != null && idx in reminders.indices) {
                onEdit(idx, clean)
            } else {
                onAdd(clean)
            }
            textFieldValue = TextFieldValue("")
            editIndex = null
        }
        onDismiss()
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Scrim(onDismiss, alpha = 0.3f)
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .liquidGlass(SmoothShape(30.dp), style = GlassStyle.Regular)
                .consumeTaps()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (editIndex != null) "Edit Reminder" else "Reminders", style = PanelTitleStyle, modifier = Modifier.weight(1f))
                Text(
                    if (editIndex != null) "Save" else "Add",
                    style = PanelBodyStyle.copy(color = AccentOrange, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable { save() }.padding(6.dp),
                )
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it.copy(text = it.text.take(200)) },
                textStyle = PanelBodyStyle,
                singleLine = true,
                cursorBrush = SolidColor(AccentOrange),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp).focusRequester(focus),
                decorationBox = { inner ->
                    if (textFieldValue.text.isEmpty()) Text("Reminder", style = PanelBodyStyle.copy(color = Color.White.copy(alpha = 0.45f)))
                    inner()
                },
            )
            // Every open reminder, newest last; tick the circle to complete one, tap text to edit.
            if (reminders.isNotEmpty()) {
                val listState = rememberLazyListState()
                Box(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().padding(end = if (reminders.size > 4) 8.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(reminders) { index, text ->
                            val isSelected = editIndex == index
                            val isCompleting = index in completingIndices
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable {
                                        if (editIndex == index) {
                                            editIndex = null
                                            textFieldValue = TextFieldValue("")
                                        } else {
                                            editIndex = index
                                            val t = reminders[index]
                                            textFieldValue = TextFieldValue(t, selection = TextRange(t.length))
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, if (isSelected) Color.White else AccentOrange, CircleShape)
                                        .background(if (isCompleting) AccentOrange else Color.Transparent)
                                        .clickable(onClickLabel = "Complete") {
                                            if (!isCompleting) {
                                                completingIndices = completingIndices + index
                                                scope.launch {
                                                    delay(200)
                                                    onComplete(index)
                                                    completingIndices = completingIndices - index
                                                    if (editIndex == index) {
                                                        editIndex = null
                                                        textFieldValue = TextFieldValue("")
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleting) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(
                                    text,
                                    style = PanelBodyStyle.copy(
                                        color = if (isCompleting) Color.White.copy(alpha = 0.45f) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        textDecoration = if (isCompleting) TextDecoration.LineThrough else null
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                )
                            }
                        }
                    }
                    if (reminders.size > 4) {
                        // iOS style vertical scroll indicator
                        val firstVisible = listState.firstVisibleItemIndex
                        val total = reminders.size
                        val progress = if (total > 0) (firstVisible.toFloat() / total).coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(3.dp)
                                .height(40.dp)
                                .offset(y = ((progress - 0.5f) * 60f).dp)
                                .clip(Capsule)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Every app A to Z under letter headings, with the iOS index down the right edge: tap or slide along it to jump.
 */
@Composable
private fun AlphabeticalApps(apps: List<AppEntry>, icons: Map<String, ImageBitmap>, onLaunch: (AppEntry, Rect) -> Unit, modifier: Modifier = Modifier) {
    val sections = remember(apps) {
        apps.sortedBy { it.label.lowercase() }.groupBy { app ->
            app.label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
        }.toSortedMap(compareBy { if (it == '#') '~' else it })
    }
    // Item positions of each letter's heading, for the index.
    val firstIndex = remember(sections) {
        var index = 0
        sections.entries.associate { (letter, list) -> letter to index.also { index += list.size + 1 } }
    }
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val letters = ('A'..'Z').toList() + '#'
    Box(
        modifier
            .fillMaxWidth()
            .liquidGlass(SmoothShape(26.dp), 26.dp, GlassStyle.Regular),
    ) {
        LazyColumn(state = list, modifier = Modifier.fillMaxWidth().padding(end = 22.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            sections.forEach { (letter, group) ->
                item(key = "h$letter") {
                    Text(letter.toString(), style = PanelBodyStyle.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp))
                }
                itemsIndexed(group, key = { _, app -> app.key }) { index, app ->
                    ResultRow(onClick = { onLaunch(app, Rect.Zero) }) {
                        val icon = icons[app.key]
                        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(44.dp)) else Spacer(Modifier.size(44.dp))
                        Text(app.label, style = PanelBodyStyle, modifier = Modifier.padding(start = 14.dp))
                    }
                    // iOS-style hairline between rows, starting under the name.
                    if (index < group.lastIndex) {
                        Box(Modifier.padding(start = 74.dp, end = 12.dp).fillMaxWidth().height(0.6.dp).background(Color.White.copy(alpha = 0.22f)))
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.align(Alignment.CenterEnd).width(22.dp).fillMaxHeight().padding(vertical = 12.dp)) {
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            fun jump(y: Float) {
                val letter = letters[((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)]
                // The nearest letter that has apps, at or after the one touched.
                val target = letters.drop(letters.indexOf(letter)).firstNotNullOfOrNull { firstIndex[it] } ?: firstIndex.values.lastOrNull() ?: 0
                scope.launch { list.scrollToItem(target) }
            }
            Column(
                Modifier.fillMaxSize().pointerInput(heightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        jump(down.position.y)
                        drag(down.id) {
                            it.consume()
                            jump(it.position.y)
                        }
                    }
                },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEach { Text(it.toString(), style = PanelCaptionStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))) }
            }
        }
    }
}
