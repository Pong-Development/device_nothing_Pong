/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.device.DeviceExtras.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lunaris.device.DeviceExtras.R

fun attachDeviceExtrasUi(activity: ComponentActivity, appsOnly: Boolean) {
    activity.enableEdgeToEdge()
    activity.window.isNavigationBarContrastEnforced = false
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DeviceExtrasViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return DeviceExtrasViewModel(activity.application) as T
        }
    }
    val model = ViewModelProvider(activity, factory)[DeviceExtrasViewModel::class.java]
    val kcalFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(KcalViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return KcalViewModel(activity.application) as T
        }
    }
    val kcalModel = ViewModelProvider(activity, kcalFactory)[KcalViewModel::class.java]
    activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            kcalModel.refresh()
            try {
                while (isActive) {
                    model.refresh()
                    delay(2000)
                }
            } finally {
                kcalModel.save()
            }
        }
    }
    activity.setContent {
        DeviceExtrasTheme {
            DeviceExtrasRoot(model, kcalModel, appsOnly, activity::finish)
        }
    }
}

@Composable
private fun DeviceExtrasRoot(
    model: DeviceExtrasViewModel, kcalModel: KcalViewModel, appsOnly: Boolean, finish: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val kcal by kcalModel.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(if (appsOnly) "apps" else "home") }
    val savedPages = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val back: () -> Unit = { if (appsOnly || page == "home") finish() else page = "home" }
    LaunchedEffect(model) {
        model.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }
    LaunchedEffect(kcalModel) {
        kcalModel.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }
    LaunchedEffect(page) { if (page == "apps") model.loadApps() }
    Box(Modifier.fillMaxSize()) {
        ExtrasPageTransition(page, canGoBack = !appsOnly && page != "home", onBack = { page = "home" }) { destination ->
            savedPages.SaveableStateProvider(destination) {
                val pageBack: () -> Unit = { if (page == destination) back() }
                when (destination) {
                    "apps" -> ThermalAppsPage(state, model, pageBack)
                    "thresholds" -> ThermalThresholdsPage(state, model, pageBack)
                    "kcal" -> KcalPage(kcal, kcalModel, pageBack)
                    else -> DeviceExtrasHome(state, kcal, model, pageBack) { next ->
                        if (page == destination) page = next
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding().imePadding().widthIn(max = 720.dp).padding(12.dp))
    }
}

@Composable
private fun DeviceExtrasHome(
    state: DeviceExtrasState,
    kcal: KcalState,
    model: DeviceExtrasViewModel,
    back: () -> Unit,
    navigate: (String) -> Unit,
) {
    val scroll = rememberLazyListState()
    val motion = MaterialTheme.motionScheme
    var showChargingInfo by rememberSaveable { mutableStateOf(false) }
    ExtrasScaffold(stringResource(R.string.device_title), back,
        subtitle = stringResource(R.string.device_home_subtitle), large = true) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter) {
            AnimatedContent(
                targetState = state.loading,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                label = "homeContent",
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) +
                        scaleIn(initialScale = 0.96f, animationSpec = motion.defaultSpatialSpec()))
                        .togetherWith(fadeOut(motion.fastEffectsSpec()))
                },
            ) { loading ->
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                } else {
                    LazyColumn(
                        state = scroll,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    ) {
                        if (state.loadFailed) {
                            item {
                                StatusCard(stringResource(R.string.device_settings_load_error), isError = true) {
                                    FilledTonalButton(
                                        onClick = touchAction(model::retryRefresh), shapes = ButtonDefaults.shapes(),
                                    ) {
                                        Text(stringResource(R.string.device_retry),
                                            style = MaterialTheme.typography.labelLargeEmphasized)
                                    }
                                }
                            }
                        }
                        if (state.chargingSupported) {
                            item {
                                SectionHeading(stringResource(R.string.charging_title), state.temperature) {
                                    IconButton(
                                        onClick = touchAction { showChargingInfo = true },
                                        shapes = IconButtonDefaults.shapes(),
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(Icons.Default.Info, stringResource(R.string.charging_info_title),
                                            Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                SwitchCard(
                                    R.drawable.ic_charging, stringResource(R.string.charging_limiter_title),
                                    stringResource(R.string.charging_switch_detail), state.chargingEnabled,
                                    !state.saving && !state.loadFailed,
                                    last = false, onChange = model::setChargingEnabled,
                                )
                                Spacer(Modifier.height(ExtrasSegmentedGap))
                                Surface(
                                    shape = groupedShape(first = false, last = true),
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                ) {
                                    ChargingProfiles(state, model::setChargingProfile, Modifier.padding(12.dp))
                                }
                            }
                        }
                        if (state.thermalSupported) {
                            item {
                                SectionHeading(stringResource(R.string.thermal_title))
                                SwitchCard(
                                    R.drawable.ic_thermal, stringResource(R.string.thermal_control_title),
                                    if (!state.thermalEnabled) stringResource(R.string.thermal_switch_detail)
                                        else if (state.selectedApps.isEmpty()) state.thermalStatus
                                        else stringResource(R.string.thermal_switch_active_detail, state.thermalStatus),
                                    state.thermalEnabled, !state.saving && !state.loadFailed, last = false,
                                    onChange = model::setThermalEnabled,
                                )
                                Spacer(Modifier.height(ExtrasSegmentedGap))
                                NavigationCard(
                                    R.drawable.ic_thermal_apps, stringResource(R.string.thermal_apps_short_title),
                                    pluralStringResource(R.plurals.thermal_apps_count,
                                        state.selectedApps.size, state.selectedApps.size),
                                    summary = stringResource(R.string.thermal_limiter_apps_summary),
                                    enabled = state.thermalEnabled && !state.saving && !state.loadFailed,
                                ) { navigate("apps") }
                                Spacer(Modifier.height(ExtrasSegmentedGap))
                                NavigationCard(
                                    R.drawable.ic_thermal_temp, stringResource(R.string.thermal_thresholds_short_title),
                                    stringResource(R.string.thermal_thresholds_values, state.warm, state.high, state.critical),
                                    summary = stringResource(R.string.thermal_thresholds_summary),
                                    enabled = state.thermalEnabled && !state.saving && !state.loadFailed, last = true,
                                ) { navigate("thresholds") }
                            }
                        }
                        if (state.otgSupported) {
                            item {
                                SectionHeading(stringResource(R.string.usb_title))
                                SwitchCard(
                                    R.drawable.ic_otg_mode, stringResource(R.string.otg_mode_title),
                                    stringResource(R.string.otg_mode_summary), state.otgEnabled,
                                    !state.saving && !state.loadFailed,
                                    highlightChecked = true,
                                    onChange = model::setOtgEnabled,
                                )
                            }
                        }
                        item {
                            SectionHeading(stringResource(R.string.display_title))
                            NavigationCard(
                                R.drawable.ic_color_calibration, stringResource(R.string.kcal_title),
                                stringResource(when {
                                    kcal.loading -> R.string.kcal_loading
                                    !kcal.supported -> R.string.kcal_unavailable_short
                                    kcal.enabled -> R.string.accessibility_quick_settings_on
                                    else -> R.string.accessibility_quick_settings_off
                                }),
                                summary = stringResource(R.string.kcal_summary), first = true, last = true,
                            ) { navigate("kcal") }
                        }
                        if (!state.loadFailed && !state.chargingSupported &&
                            !state.thermalSupported && !state.otgSupported && !kcal.loading && !kcal.supported) {
                            item { StatusCard(stringResource(R.string.device_controls_unavailable)) }
                        }
                    }
                }
            }
        }
    }
    if (showChargingInfo && state.chargingSupported) {
        ChargingInfoDialog(state.chargingEnabled) { showChargingInfo = false }
    }
}

@Composable
private fun ChargingProfiles(
    state: DeviceExtrasState,
    select: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = listOf("slow", "mid", "fast")
    val labels = listOf(R.string.charging_profile_slow, R.string.charging_profile_mid, R.string.charging_profile_fast)
    val enabled = state.chargingEnabled && !state.saving && !state.loadFailed
    val motion = MaterialTheme.motionScheme
    val groupLabel = stringResource(R.string.charging_limiter_profile_title)
    val summary = if (!state.chargingEnabled) R.string.charging_profiles_disabled
        else when (state.chargingProfile) {
            "slow" -> R.string.charging_profile_slow_summary
            "fast" -> R.string.charging_profile_fast_summary
            else -> R.string.charging_profile_mid_summary
        }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup().semantics { contentDescription = groupLabel },
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically) {
            values.forEachIndexed { index, profile ->
                val checked = state.chargingProfile == profile
                val label = stringResource(labels[index])
                val click = touchAction { select(profile) }
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val widthWeight by animateFloatAsState(
                    if (pressed && enabled) 1.16f else 1f,
                    motion.fastSpatialSpec(), label = "profileStretch")
                val container by animateColorAsState(
                    if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    motion.fastEffectsSpec(), label = "profileContainer")
                val foreground by animateColorAsState(
                    if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    motion.fastEffectsSpec(), label = "profileForeground")
                ToggleButton(
                    checked = checked, onCheckedChange = { if (!checked) click() }, enabled = enabled,
                    modifier = Modifier.weight(widthWeight).heightIn(min = 48.dp).clearAndSetSemantics {
                        role = Role.RadioButton
                        selected = checked
                        contentDescription = label
                        if (!enabled) disabled()
                        else onClick { if (!checked) click(); true }
                    },
                    interactionSource = interaction,
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        values.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = container, contentColor = foreground,
                        checkedContainerColor = container, checkedContentColor = foreground,
                    ),
                    elevation = null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(label,
                        style = if (checked) MaterialTheme.typography.labelLargeEmphasized
                            else MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center)
                }
            }
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimatedContent(
                targetState = summary, label = "chargingSummary",
                transitionSpec = {
                    (fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()))
                        .using(SizeTransform { _, _ -> motion.fastSpatialSpec() })
                },
            ) { description ->
                Text(stringResource(description), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.charging_temperature_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChargingInfoDialog(chargingEnabled: Boolean, dismiss: () -> Unit) {
    val profiles = listOf(
        R.string.charging_profile_slow to R.string.charging_profile_slow_detail,
        R.string.charging_profile_mid to R.string.charging_profile_mid_detail,
        R.string.charging_profile_fast to R.string.charging_profile_fast_detail,
    )
    AlertDialog(
        onDismissRequest = dismiss,
        icon = { Icon(Icons.Default.Info, null) },
        title = {
            Text(stringResource(R.string.charging_info_title),
                style = MaterialTheme.typography.headlineSmallEmphasized)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.charging_switch_detail))
                profiles.forEach { (label, detail) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(label), style = MaterialTheme.typography.titleSmallEmphasized)
                        Text(stringResource(detail))
                    }
                }
                if (!chargingEnabled) Text(stringResource(R.string.charging_profiles_disabled))
                Text(stringResource(R.string.charging_temperature_note))
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = touchAction(dismiss), shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.device_done), style = MaterialTheme.typography.labelLargeEmphasized)
            }
        },
    )
}
