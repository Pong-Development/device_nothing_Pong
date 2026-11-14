@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lunaris.device.DeviceExtras.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.drop
import org.lunaris.device.DeviceExtras.R

@Composable
internal fun ThermalAppsPage(state: DeviceExtrasState, model: DeviceExtrasViewModel, back: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val searchLabel = stringResource(R.string.device_search_apps)
    val scroll = rememberLazyListState()
    val motion = MaterialTheme.motionScheme
    val visibleApps = remember(state.apps, state.selectedApps, query, selectedOnly) {
        val search = query.trim()
        state.apps.filter { app ->
            (!selectedOnly || app.packageName in state.selectedApps) &&
                (search.isEmpty() || app.label.contains(search, ignoreCase = true) ||
                    app.packageName.contains(search, ignoreCase = true))
        }
    }
    LaunchedEffect(scroll) {
        snapshotFlow { query to selectedOnly }.drop(1).collect { scroll.scrollToItem(0) }
    }
    ExtrasScaffold(
        stringResource(R.string.thermal_limiter_apps_title), back,
        subtitle = pluralStringResource(R.plurals.thermal_limiter_apps_selected,
            state.selectedApps.size, state.selectedApps.size),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().padding(horizontal = 20.dp)) {
                TextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .semantics { contentDescription = searchLabel },
                    shape = RoundedCornerShape(28.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = { Text(searchLabel) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        AnimatedVisibility(query.isNotEmpty(),
                            enter = fadeIn(motion.fastEffectsSpec()),
                            exit = fadeOut(motion.fastEffectsSpec())) {
                            IconButton(onClick = touchAction { query = "" }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Default.Close, stringResource(R.string.device_clear_search))
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                )
                FlowRow(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExtrasFilterChip(
                        selected = !selectedOnly,
                        onClick = touchAction { selectedOnly = false },
                        label = {
                            Text(stringResource(R.string.device_all_apps),
                                style = if (!selectedOnly) MaterialTheme.typography.labelLargeEmphasized
                                    else MaterialTheme.typography.labelLarge)
                        },
                        leadingIcon = if (!selectedOnly) {
                            { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                    ExtrasFilterChip(
                        selected = selectedOnly,
                        onClick = touchAction { selectedOnly = true },
                        label = {
                            Text(stringResource(R.string.device_selected_filter, state.selectedApps.size),
                                style = if (selectedOnly) MaterialTheme.typography.labelLargeEmphasized
                                    else MaterialTheme.typography.labelLarge)
                        },
                        leadingIcon = if (selectedOnly) {
                            { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
                if (state.appsLoading) {
                    Column(Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LoadingIndicator()
                        Text(stringResource(R.string.thermal_limiter_apps_loading),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (state.appsFailed) {
                    InfoNote(stringResource(R.string.device_apps_load_error))
                    FilledTonalButton(
                        onClick = touchAction(model::loadApps), shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.device_retry))
                    }
                } else if (visibleApps.isEmpty()) {
                    InfoNote(stringResource(if (selectedOnly && query.isBlank())
                        R.string.thermal_limiter_apps_none else R.string.device_apps_no_results))
                }
                LazyColumn(
                    state = scroll, modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                        val checked = app.packageName in state.selectedApps
                        val enabled = !state.saving && !state.loadFailed
                        val alpha = if (enabled) 1f else 0.38f
                        val container by animateColorAsState(
                            if (checked) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            motion.fastEffectsSpec(), label = "appContainer",
                        )
                        val toggle = touchAction { model.selectApp(app.packageName, !checked) }
                        val shape = groupedShape(index == 0, index == visibleApps.lastIndex)
                        Surface(
                            shape = shape,
                            color = container,
                            modifier = Modifier.animateItem(
                                fadeInSpec = motion.fastEffectsSpec(),
                                placementSpec = motion.defaultSpatialSpec(),
                                fadeOutSpec = motion.fastEffectsSpec(),
                            ).fillMaxWidth().clip(shape).toggleable(
                                value = checked, enabled = enabled, role = Role.Checkbox,
                                onValueChange = { toggle() },
                            ),
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                val icon = app.icon
                                if (icon != null) {
                                    Image(icon.asImageBitmap(), null,
                                        Modifier.size(40.dp).clip(MaterialTheme.shapes.small), alpha = alpha)
                                } else {
                                    SettingIcon(R.drawable.ic_thermal_apps, enabled)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(app.label,
                                        style = if (checked) MaterialTheme.typography.titleMediumEmphasized
                                            else MaterialTheme.typography.titleMedium,
                                        color = (if (checked) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurface).copy(alpha = alpha),
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                                        color = (if (checked) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Checkbox(checked, onCheckedChange = null, enabled = enabled)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThermalThresholdsPage(state: DeviceExtrasState, model: DeviceExtrasViewModel, back: () -> Unit) {
    val scroll = rememberLazyListState()
    val enabled = state.thermalEnabled && !state.saving && !state.loadFailed
    ExtrasScaffold(stringResource(R.string.thermal_thresholds_title), back,
        subtitle = stringResource(R.string.thermal_thresholds_subtitle)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = scroll, modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    InfoNote(stringResource(R.string.thermal_thresholds_intro))
                }
                item {
                    ThresholdCard(
                        stringResource(R.string.thermal_limiter_temp_warm_title),
                        stringResource(R.string.thermal_warm_detail), state.warm, 35..60,
                        revision = state.settingsRevision,
                        enabled, first = true, onCommit = { model.setThreshold(0, it) },
                    )
                }
                item {
                    ThresholdCard(
                        stringResource(R.string.thermal_limiter_temp_high_title),
                        stringResource(R.string.thermal_high_detail), state.high,
                        36..(if (state.warm == 60) 61 else 60),
                        revision = state.settingsRevision,
                        enabled, onCommit = { model.setThreshold(1, it) },
                    )
                }
                item {
                    ThresholdCard(
                        stringResource(R.string.thermal_limiter_temp_critical_title),
                        stringResource(R.string.thermal_critical_detail), state.critical, 37..62,
                        revision = state.settingsRevision,
                        enabled, last = true, onCommit = { model.setThreshold(2, it) },
                    )
                }
                item {
                    InfoNote(stringResource(R.string.thermal_thresholds_order_note))
                    FilledTonalButton(
                        onClick = touchAction(model::resetThresholds),
                        enabled = enabled && (state.warm != 40 || state.high != 43 || state.critical != 45),
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.thermal_thresholds_reset),
                            style = MaterialTheme.typography.labelLargeEmphasized)
                    }
                    if (!state.thermalEnabled) InfoNote(stringResource(R.string.thermal_enable_first))
                }
            }
        }
    }
}

@Composable
private fun ThresholdCard(
    title: String,
    detail: String,
    value: Int,
    range: IntRange,
    revision: Long,
    enabled: Boolean,
    first: Boolean = false,
    last: Boolean = false,
    onCommit: (Int) -> Unit,
) {
    var draft by remember(value, revision, enabled) { mutableFloatStateOf(value.toFloat()) }
    val motion = MaterialTheme.motionScheme
    val alpha = if (enabled) 1f else 0.38f
    val temperatureDescription = stringResource(R.string.device_integer_temperature, draft.roundToInt())
    Surface(shape = groupedShape(first, last), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                    Text(detail, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                }
                Surface(shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alpha),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha)) {
                    AnimatedContent(
                        targetState = draft.roundToInt(), label = "thresholdValue",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        transitionSpec = {
                            fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                        },
                    ) { temperature ->
                        Text(stringResource(R.string.device_integer_temperature, temperature),
                            style = MaterialTheme.typography.titleMediumEmphasized, textAlign = TextAlign.Center)
                    }
                }
            }
            Slider(
                value = draft.coerceIn(range.first.toFloat(), range.last.toFloat()),
                onValueChange = { draft = it },
                onValueChangeFinished = { onCommit(draft.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0), enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = title
                    stateDescription = temperatureDescription
                },
            )
        }
    }
}
