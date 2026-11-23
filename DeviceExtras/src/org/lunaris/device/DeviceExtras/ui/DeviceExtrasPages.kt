@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lunaris.device.DeviceExtras.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
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
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val filters = listOf(
                        false to stringResource(R.string.device_all_apps),
                        true to stringResource(R.string.device_selected_filter, state.selectedApps.size),
                    )
                    filters.forEachIndexed { index, (isSelectedFilter, label) ->
                        val active = selectedOnly == isSelectedFilter
                        val click = touchAction { selectedOnly = isSelectedFilter }
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val widthWeight by animateFloatAsState(
                            if (pressed) 1.12f else 1f,
                            motion.fastSpatialSpec(), label = "appFilterStretch",
                        )
                        val container by animateColorAsState(
                            if (active) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceBright,
                            motion.fastEffectsSpec(), label = "appFilterContainer",
                        )
                        val foreground by animateColorAsState(
                            if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            motion.fastEffectsSpec(), label = "appFilterForeground",
                        )
                        ToggleButton(
                            checked = active,
                            onCheckedChange = { if (!active) click() },
                            interactionSource = interaction,
                            modifier = Modifier.weight(widthWeight).heightIn(min = 48.dp).clearAndSetSemantics {
                                role = Role.RadioButton
                                selected = active
                                contentDescription = label
                                onClick { if (!active) click(); true }
                            },
                            shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                                else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = container, contentColor = foreground,
                                checkedContainerColor = container, checkedContentColor = foreground,
                            ),
                            elevation = null,
                        ) {
                            AnimatedVisibility(
                                visible = active,
                                enter = fadeIn(motion.fastEffectsSpec()) + expandHorizontally(motion.fastSpatialSpec()),
                                exit = fadeOut(motion.fastEffectsSpec()) + shrinkHorizontally(motion.fastSpatialSpec()),
                            ) {
                                Row {
                                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                            }
                            Text(
                                label,
                                style = if (active) MaterialTheme.typography.labelLargeEmphasized
                                    else MaterialTheme.typography.labelLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
                    StatusCard(stringResource(R.string.device_apps_load_error), isError = true) {
                        FilledTonalButton(
                            onClick = touchAction(model::loadApps), shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.device_retry),
                                style = MaterialTheme.typography.labelLargeEmphasized)
                        }
                    }
                } else if (visibleApps.isEmpty()) {
                    StatusCard(stringResource(if (selectedOnly && query.isBlank())
                        R.string.thermal_limiter_apps_none else R.string.device_apps_no_results))
                }
                LazyColumn(
                    state = scroll, modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ExtrasSegmentedGap),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                        val checked = app.packageName in state.selectedApps
                        val enabled = !state.saving && !state.loadFailed
                        val alpha = if (enabled) 1f else 0.38f
                        val toggle = touchAction { model.selectApp(app.packageName, !checked) }
                        ExtrasListItem(
                            onClick = toggle, checked = checked, selected = checked,
                            role = Role.Checkbox, enabled = enabled,
                            first = index == 0, last = index == visibleApps.lastIndex,
                            modifier = Modifier.animateItem(
                                fadeInSpec = motion.fastEffectsSpec(),
                                placementSpec = motion.defaultSpatialSpec(),
                                fadeOutSpec = motion.fastEffectsSpec(),
                            ).fillMaxWidth(),
                            leadingContent = {
                                val icon = app.icon
                                if (icon != null) {
                                    Image(icon.asImageBitmap(), null,
                                        Modifier.size(40.dp).clip(MaterialTheme.shapes.small), alpha = alpha)
                                } else {
                                    SettingIcon(R.drawable.ic_thermal_apps, enabled, active = checked)
                                }
                            },
                            supportingContent = {
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            trailingContent = {
                                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                            },
                        ) {
                            Text(app.label,
                                style = if (checked) MaterialTheme.typography.titleMediumEmphasized
                                    else MaterialTheme.typography.titleMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                verticalArrangement = Arrangement.spacedBy(ExtrasSegmentedGap),
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
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
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val active = enabled && (dragged || pressed || focused || hovered)
    val motion = MaterialTheme.motionScheme
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f
    val topCorner by animateDpAsState(
        if (first || active) 28.dp else 8.dp, motion.fastSpatialSpec(), label = "thresholdTopCorner",
    )
    val bottomCorner by animateDpAsState(
        if (last || active) 28.dp else 8.dp, motion.fastSpatialSpec(), label = "thresholdBottomCorner",
    )
    val container by animateColorAsState(
        if (active) scheme.surfaceContainerHighest else scheme.surfaceBright,
        motion.fastEffectsSpec(), label = "thresholdContainer",
    )
    val shape = RoundedCornerShape(
        topStart = topCorner.coerceAtLeast(0.dp), topEnd = topCorner.coerceAtLeast(0.dp),
        bottomStart = bottomCorner.coerceAtLeast(0.dp), bottomEnd = bottomCorner.coerceAtLeast(0.dp),
    )
    val rounded = draft.roundToInt()
    val temperatureDescription = stringResource(R.string.device_integer_temperature, rounded)
    Surface(shape = shape, color = container) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMediumEmphasized,
                        color = scheme.onSurface.copy(alpha = alpha))
                    Text(detail, style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant.copy(alpha = alpha))
                }
                Surface(shape = RoundedCornerShape(20.dp),
                    color = scheme.secondaryContainer.copy(alpha = alpha),
                    contentColor = scheme.onSecondaryContainer.copy(alpha = alpha)) {
                    RollingNumberText(
                        value = rounded,
                        format = { stringResource(R.string.device_integer_temperature, it) },
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
            Slider(
                value = draft.coerceIn(range.first.toFloat(), range.last.toFloat()),
                interactionSource = interaction,
                onValueChange = { next ->
                    if (next.roundToInt() != draft.roundToInt()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    draft = next
                },
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
