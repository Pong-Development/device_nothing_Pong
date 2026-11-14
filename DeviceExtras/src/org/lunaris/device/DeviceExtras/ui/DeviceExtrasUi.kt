@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.device.DeviceExtras.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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
    activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                model.refresh()
                delay(2000)
            }
        }
    }
    activity.setContent {
        DeviceExtrasTheme {
            DeviceExtrasRoot(model, appsOnly, activity::finish)
        }
    }
}

@Composable
private fun DeviceExtrasRoot(model: DeviceExtrasViewModel, appsOnly: Boolean, finish: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(if (appsOnly) "apps" else "home") }
    val savedPages = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val back: () -> Unit = { if (appsOnly || page == "home") finish() else page = "home" }
    BackHandler(enabled = page != "home", onBack = back)
    LaunchedEffect(model) {
        model.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }
    LaunchedEffect(page) { if (page == "apps") model.loadApps() }
    val distance = with(LocalDensity.current) { 32.dp.roundToPx() } *
        if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val motion = MaterialTheme.motionScheme
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            transitionSpec = {
                val offset = if (targetState == "home") -distance else distance
                (slideInHorizontally(motion.defaultSpatialSpec()) { offset } +
                    fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (slideOutHorizontally(motion.defaultSpatialSpec()) { -offset } +
                        fadeOut(motion.fastEffectsSpec())) using null
            },
            label = "deviceExtrasPage",
        ) { destination ->
            savedPages.SaveableStateProvider(destination) {
                val pageBack: () -> Unit = { if (page == destination) back() }
                when (destination) {
                    "apps" -> ThermalAppsPage(state, model, pageBack)
                    "thresholds" -> ThermalThresholdsPage(state, model, pageBack)
                    else -> DeviceExtrasHome(state, model, pageBack) { next ->
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
    model: DeviceExtrasViewModel,
    back: () -> Unit,
    navigate: (String) -> Unit,
) {
    val scroll = rememberLazyListState()
    ExtrasScaffold(stringResource(R.string.device_title), back,
        subtitle = stringResource(R.string.device_home_subtitle), large = true) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = scroll,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            ) {
                if (state.loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                }
                if (state.loadFailed) {
                    item {
                        InfoNote(stringResource(R.string.device_settings_load_error))
                        FilledTonalButton(
                            onClick = touchAction(model::retryRefresh), shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.device_retry))
                        }
                    }
                }
                if (state.chargingSupported) {
                    item {
                        SectionHeading(stringResource(R.string.charging_title), state.temperature)
                        SwitchCard(
                            R.drawable.ic_charging, stringResource(R.string.charging_limiter_title),
                            stringResource(R.string.charging_switch_detail), state.chargingEnabled,
                            !state.saving && !state.loadFailed, onChange = model::setChargingEnabled,
                        )
                        Spacer(Modifier.height(12.dp))
                        ChargingProfiles(state, model::setChargingProfile)
                        InfoNote(stringResource(R.string.charging_temperature_note))
                    }
                }
                if (state.thermalSupported) {
                    item {
                        SectionHeading(stringResource(R.string.thermal_title))
                        SwitchCard(
                            R.drawable.ic_thermal, stringResource(R.string.thermal_limiter_title),
                            if (state.thermalEnabled) state.thermalStatus
                                else stringResource(R.string.thermal_switch_detail),
                            state.thermalEnabled, !state.saving && !state.loadFailed, last = false,
                            onChange = model::setThermalEnabled,
                        )
                        Spacer(Modifier.height(4.dp))
                        NavigationCard(
                            R.drawable.ic_thermal_apps, stringResource(R.string.thermal_limiter_apps_title),
                            if (state.selectedApps.isEmpty()) stringResource(R.string.thermal_limiter_apps_none)
                                else pluralStringResource(R.plurals.thermal_limiter_apps_selected,
                                    state.selectedApps.size, state.selectedApps.size),
                            enabled = state.thermalEnabled && !state.saving && !state.loadFailed,
                        ) { navigate("apps") }
                        Spacer(Modifier.height(4.dp))
                        NavigationCard(
                            R.drawable.ic_thermal_temp, stringResource(R.string.thermal_thresholds_title),
                            stringResource(R.string.thermal_thresholds_values, state.warm, state.high, state.critical),
                            enabled = state.thermalEnabled && !state.saving && !state.loadFailed, last = true,
                        ) { navigate("thresholds") }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (state.otgSupported) {
                    item {
                        SectionHeading(stringResource(R.string.usb_title))
                        SwitchCard(
                            R.drawable.ic_otg_mode, stringResource(R.string.otg_mode_title),
                            stringResource(R.string.otg_mode_summary), state.otgEnabled,
                            !state.saving && !state.loadFailed,
                            onChange = model::setOtgEnabled,
                        )
                    }
                }
                if (!state.loading && !state.loadFailed && !state.chargingSupported &&
                    !state.thermalSupported && !state.otgSupported) {
                    item { InfoNote(stringResource(R.string.device_controls_unavailable)) }
                }
            }
        }
    }
}

@Composable
private fun ChargingProfiles(state: DeviceExtrasState, select: (String) -> Unit) {
    val values = listOf("slow", "mid", "fast")
    val labels = listOf(R.string.charging_profile_slow, R.string.charging_profile_mid, R.string.charging_profile_fast)
    val icons = listOf(R.drawable.ic_charging_slow, R.drawable.ic_charging_mid, R.drawable.ic_charging_fast)
    val enabled = state.chargingEnabled && !state.saving && !state.loadFailed
    val motion = MaterialTheme.motionScheme
    Text(stringResource(R.string.charging_limiter_profile_title),
        style = MaterialTheme.typography.titleSmallEmphasized,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp).semantics { heading() })
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
        val stacked = maxWidth / fontScale < 320.dp
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically) {
            values.forEachIndexed { index, profile ->
                val checked = state.chargingProfile == profile
                val label = stringResource(labels[index])
                val click = touchAction { select(profile) }
                val container by animateColorAsState(
                    if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    motion.fastEffectsSpec(), label = "profileContainer")
                val foreground by animateColorAsState(
                    if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    motion.fastEffectsSpec(), label = "profileForeground")
                val profileLabel: @Composable () -> Unit = {
                    Text(label,
                        style = if (checked) MaterialTheme.typography.labelLargeEmphasized
                            else MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center)
                }
                ToggleButton(
                    checked = checked, onCheckedChange = { if (!checked) click() }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = if (stacked) 64.dp else 44.dp).clearAndSetSemantics {
                        role = Role.RadioButton
                        selected = checked
                        contentDescription = label
                        if (!enabled) disabled()
                        else onClick { if (!checked) click(); true }
                    },
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
                    if (stacked) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(icons[index]), null, Modifier.size(20.dp))
                            profileLabel()
                        }
                    } else {
                        Icon(painterResource(icons[index]), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        profileLabel()
                    }
                }
            }
        }
    }
    val detail = if (!state.chargingEnabled) R.string.charging_profiles_disabled
        else when (state.chargingProfile) {
            "slow" -> R.string.charging_profile_slow_detail
            "fast" -> R.string.charging_profile_fast_detail
            else -> R.string.charging_profile_mid_detail
        }
    AnimatedContent(
        targetState = detail, label = "chargingProfileDetail",
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        transitionSpec = {
            fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
        },
    ) { description ->
        Text(stringResource(description), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
