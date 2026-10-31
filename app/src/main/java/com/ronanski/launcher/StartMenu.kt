package com.ronanski.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Windows 11-style Start menu: search, pinned apps, all apps A–Z, power. */
@Composable
fun StartMenu(
    settings: LauncherSettings,
    repo: AppRepository,
    font: FontFamily,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onRename: ((AppInfo) -> Unit)?,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val accent = Color(settings.accentColor)
    val close: () -> Unit = {
        keyboard?.hide()
        onClose()
    }

    val apps = repo.apps
    val hidden = settings.hidden
    val renames = settings.renames
    val visible = remember(apps, hidden, renames) {
        apps.filter { it.key !in hidden }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { repo.label(it) })
    }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val results = remember(visible, query) {
        val q = query.trim()
        if (q.isEmpty()) emptyList() else visible.filter { repo.label(it).contains(q, ignoreCase = true) }
            .sortedBy { if (repo.label(it).startsWith(q, ignoreCase = true)) 0 else 1 }
    }

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
            }
        }
    }

    // Tapping outside the panel closes Start.
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { close() } }
            .imePadding(),
    ) {
        Column(
            Modifier
                .align(if (settings.taskbarCentered) Alignment.BottomCenter else Alignment.BottomStart)
                .padding(12.dp)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                // While searching (keyboard open) use all the room above the keyboard.
                .fillMaxHeight(if (query.isNotBlank()) 1f else 0.86f)
                .clip(RoundedCornerShape(12.dp))
                .background(panelColor(settings))
                .border(1.dp, Ui.text.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("Search for apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        results.firstOrNull()?.let {
                            AppLaunch.open(context, it)
                            close()
                        }
                    }),
                )
                Spacer(Modifier.height(16.dp))
                when {
                    query.isNotBlank() -> {
                        SearchResults(query, results, repo, settings, font, onRename, close)
                    }
                    showAll -> {
                        SectionHeader("All apps", font, action = "‹ Back") { showAll = false }
                        val grouped = remember(visible) {
                            visible.groupBy { repo.label(it).firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() } ?: '#' }
                        }
                        LazyColumn(Modifier.fillMaxSize()) {
                            grouped.forEach { (letter, group) ->
                                item(key = "header-$letter") {
                                    Text(
                                        letter.toString(),
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = font,
                                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                                    )
                                }
                                items(group, key = { it.key }) { app ->
                                    AppRow(app, repo, settings, font, onRename, close)
                                }
                            }
                        }
                    }
                    else -> {
                        SectionHeader("Pinned", font, action = "All apps ›") { showAll = true }
                        val pinned = settings.startPinned.mapNotNull { byKey[it] }
                        if (pinned.isEmpty()) {
                            Text(
                                "Nothing pinned yet. Open All apps, long-press an app → Pin to Start.",
                                color = Ui.subtle,
                                fontFamily = font,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            // Fewer columns on narrow screens so labels aren't cut off.
                            val fit = (maxWidth / 84.dp).toInt().coerceAtLeast(3)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(minOf(settings.startColumns.coerceIn(3, 10), fit)),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(pinned, key = { it.key }) { app ->
                                    AppTile(app, repo, settings, font, onRename, close)
                                }
                            }
                        }
                    }
                }
            }
            // Bottom bar (hidden while searching so results get the space)
            if (query.isBlank()) {
            HorizontalDivider(color = Ui.text.copy(alpha = 0.08f))
            Row(
                Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            close()
                            LauncherUi.showSettings(context, null)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Ui.text, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Settings", color = Ui.text, fontFamily = font)
                }
                Spacer(Modifier.weight(1f))
                PowerButton(close)
            }
            }
        }
    }
}

/** Windows-style search: calculator, apps, settings, contacts and web. */
@Composable
private fun SearchResults(
    query: String,
    apps: List<AppInfo>,
    repo: AppRepository,
    settings: LauncherSettings,
    font: FontFamily,
    onRename: ((AppInfo) -> Unit)?,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var contactsAllowed by remember { mutableStateOf(Search.hasContactsPermission(context)) }
    val hasRegistry = androidx.activity.compose.LocalActivityResultRegistryOwner.current != null
    val askContacts = if (hasRegistry) {
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        ) { granted -> contactsAllowed = granted }
    } else {
        null
    }
    val calc = remember(query) { Search.calculate(query) }
    val settingsHits = remember(query) { Search.settingsHits(query) }
    val pageHits = remember(query) { Search.launcherPageHits(query) }
    val contacts = remember(query, contactsAllowed) { Search.contacts(context, query) }

    LazyColumn(Modifier.fillMaxSize()) {
        if (calc != null) {
            item { SectionHeader("Calculator", font) }
            item { ResultRow("= $calc", query.trim(), font) {} }
        }
        item { SectionHeader("Apps", font) }
        if (apps.isEmpty()) item { Text("No apps found", color = Ui.subtle, fontFamily = font, modifier = Modifier.padding(8.dp)) }
        items(apps.take(8), key = { it.key }) { app -> AppRow(app, repo, settings, font, onRename, close) }
        if (settingsHits.isNotEmpty() || pageHits.isNotEmpty()) {
            item { SectionHeader("Settings", font) }
            items(pageHits, key = { "page-" + it.name }) { p ->
                ResultRow(p.title, "Pad Launcher settings", font) {
                    close()
                    LauncherUi.showSettings(context, p)
                }
            }
            items(settingsHits, key = { "set-" + it.title }) { hit ->
                ResultRow(hit.title, "Android settings", font) {
                    Search.openSetting(context, hit)
                    close()
                }
            }
        }
        item { SectionHeader("Contacts", font) }
        if (!contactsAllowed) {
            item {
                ResultRow("Search your contacts too", "Tap to allow contacts access", font) {
                    if (askContacts != null) {
                        askContacts.launch(android.Manifest.permission.READ_CONTACTS)
                    } else {
                        close()
                        AppLaunch.openAppDetails(context)
                    }
                }
            }
        } else if (contacts.isEmpty()) {
            item { Text("No contacts found", color = Ui.subtle, fontFamily = font, modifier = Modifier.padding(8.dp)) }
        } else {
            items(contacts, key = { "contact-" + it.id }) { c ->
                ResultRow(c.name, "Contact", font) {
                    Search.openContact(context, c)
                    close()
                }
            }
        }
        item { SectionHeader("Web", font) }
        item {
            ResultRow("Search the web for \"${query.trim()}\"", "Opens your browser", font) {
                Search.webSearch(context, query.trim())
                close()
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, font: FontFamily, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(title, color = Ui.text, fontFamily = font, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = Ui.subtle, fontFamily = font, fontSize = 12.sp)
    }
}

@Composable
private fun PowerButton(close: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { menu = true },
            contentAlignment = Alignment.Center,
        ) { PowerGlyph(Ui.text, 20.dp) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Lock") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                onClick = {
                    menu = false
                    close()
                    Actions.perform(context, GestureAction.LOCK_SCREEN) {}
                },
            )
            DropdownMenuItem(
                text = { Text("Power options…") },
                leadingIcon = { PowerGlyph(Ui.text, 20.dp) },
                onClick = {
                    menu = false
                    close()
                    Actions.perform(context, GestureAction.POWER_MENU) {}
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, font: FontFamily, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ui.text, fontWeight = FontWeight.SemiBold, fontFamily = font, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = Ui.text,
                fontSize = 13.sp,
                fontFamily = font,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Ui.text.copy(alpha = 0.08f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: AppInfo,
    repo: AppRepository,
    settings: LauncherSettings,
    font: FontFamily,
    onRename: ((AppInfo) -> Unit)?,
    close: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .combinedClickable(
                    onClick = {
                        AppLaunch.open(context, app)
                        close()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                )
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIconImage(app, repo, settings, 40.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                repo.label(app),
                color = Ui.text,
                fontSize = 12.sp,
                fontFamily = font,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        AppContextMenu(
            menu, { menu = false }, app,
            windowEntries(context, app, settings, close) + pinEntries(context, app, settings, onRename),
            close,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppInfo,
    repo: AppRepository,
    settings: LauncherSettings,
    font: FontFamily,
    onRename: ((AppInfo) -> Unit)?,
    close: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .combinedClickable(
                    onClick = {
                        AppLaunch.open(context, app)
                        close()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIconImage(app, repo, settings, 32.dp)
            Text(repo.label(app), color = Ui.text, fontFamily = font, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val entries = windowEntries(context, app, settings, close) +
            pinEntries(context, app, settings, onRename) +
            MenuEntry(if (app.key in settings.hidden) "Unhide" else "Hide from All apps") { settings.toggleHidden(app.key) }
        AppContextMenu(menu, { menu = false }, app, entries, close)
    }
}
