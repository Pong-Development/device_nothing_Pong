/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.device.DeviceExtras.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.lunaris.device.DeviceExtras.R
import org.lunaris.device.DeviceExtras.kcal.KcalNodes

private val channelLabels = listOf(
    R.string.kcal_red, R.string.kcal_green, R.string.kcal_blue,
    R.string.kcal_saturation, R.string.kcal_value, R.string.kcal_contrast, R.string.kcal_hue,
)

@Composable
internal fun KcalPage(state: KcalState, model: KcalViewModel, back: () -> Unit) {
    val scroll = rememberLazyListState()
    var editing by rememberSaveable { mutableIntStateOf(-1) }
    val controls = state.supported && !state.loading && !state.loadFailed && !state.saving
    val sliders = controls && state.enabled
    DisposableEffect(model) {
        onDispose { model.save() }
    }
    ExtrasScaffold(stringResource(R.string.kcal_title), back,
        subtitle = stringResource(R.string.kcal_subtitle)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = scroll, modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(ExtrasSegmentedGap),
            ) {
                if (state.loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                } else {
                    item {
                        SwitchCard(
                            R.drawable.ic_color_calibration, stringResource(R.string.kcal_enable),
                            stringResource(when {
                                !state.supported -> R.string.kcal_unavailable
                                state.enabled -> R.string.kcal_active
                                else -> R.string.kcal_inactive
                            }),
                            checked = state.enabled, enabled = controls, onChange = model::setEnabled,
                        )
                    }
                    if (!state.supported || state.loadFailed) {
                        item {
                            FilledTonalButton(
                                onClick = touchAction(model::refresh), shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            ) { Text(stringResource(R.string.device_retry)) }
                        }
                    } else {
                        item {
                            SectionHeading(stringResource(R.string.kcal_preview))
                            KcalPreview()
                            InfoNote(stringResource(R.string.kcal_preview_detail))
                            SectionHeading(stringResource(R.string.kcal_rgb_title))
                        }
                        for (index in 0..2) {
                            item(key = index) {
                                KcalChannelCard(index, state.profile[index], sliders,
                                    first = index == 0, last = index == 2,
                                    onChange = { model.setChannel(index, it) }, onFinish = model::save,
                                    onEdit = { editing = index })
                            }
                        }
                        item { SectionHeading(stringResource(R.string.kcal_tone_title)) }
                        for (index in 3..6) {
                            item(key = index) {
                                KcalChannelCard(index, state.profile[index], sliders,
                                    first = index == 3, last = index == 6,
                                    onChange = { model.setChannel(index, it) }, onFinish = model::save,
                                    onEdit = { editing = index })
                            }
                        }
                        item {
                            InfoNote(stringResource(R.string.kcal_precision_detail))
                            FilledTonalButton(
                                onClick = touchAction(model::reset), shapes = ButtonDefaults.shapes(),
                                enabled = controls && state.profile != KcalNodes.defaults().toList(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.kcal_reset),
                                    style = MaterialTheme.typography.labelLargeEmphasized)
                            }
                        }
                    }
                }
            }
        }
    }
    if (editing in channelLabels.indices && sliders) {
        KcalValueDialog(editing, state.profile[editing], dismiss = { editing = -1 }) { value ->
            model.setChannel(editing, value)
            model.save()
            editing = -1
        }
    }
}

@Composable
private fun KcalPreview() {
    val description = stringResource(R.string.kcal_preview_description)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.fillMaxWidth().height(56.dp).background(Brush.horizontalGradient(
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta),
                )))
            }
            Surface(shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.fillMaxWidth().height(32.dp).background(
                    Brush.horizontalGradient(listOf(Color.Black, Color.White)),
                ))
            }
        }
    }
}

@Composable
private fun KcalChannelCard(
    index: Int, value: Int, enabled: Boolean,
    first: Boolean, last: Boolean,
    onChange: (Int) -> Unit, onFinish: () -> Unit, onEdit: () -> Unit,
) {
    val title = stringResource(channelLabels[index])
    val number = stringResource(R.string.kcal_number, value)
    val editDescription = stringResource(R.string.kcal_edit_value, title, value)
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val active = enabled && (dragged || pressed || focused || hovered)
    val motion = MaterialTheme.motionScheme
    val scheme = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.38f
    val topCorner by animateDpAsState(
        if (first || active) 28.dp else 8.dp, motion.fastSpatialSpec(), label = "kcalTopCorner",
    )
    val bottomCorner by animateDpAsState(
        if (last || active) 28.dp else 8.dp, motion.fastSpatialSpec(), label = "kcalBottomCorner",
    )
    val container by animateColorAsState(
        if (active) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh,
        motion.fastEffectsSpec(), label = "kcalContainer",
    )
    val foreground by animateColorAsState(
        scheme.onSurface.copy(alpha = alpha), motion.fastEffectsSpec(), label = "kcalForeground",
    )
    val position by animateFloatAsState(
        KcalSliderScale.position(index, value),
        animationSpec = if (dragged || pressed) snap() else motion.fastSpatialSpec(),
        label = "kcalSliderPosition",
    )
    val minimum = KcalNodes.minimum(index)
    val maximum = KcalNodes.maximum(index)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = RoundedCornerShape(
        topStart = topCorner.coerceAtLeast(0.dp), topEnd = topCorner.coerceAtLeast(0.dp),
        bottomStart = bottomCorner.coerceAtLeast(0.dp), bottomEnd = bottomCorner.coerceAtLeast(0.dp),
    )
    Surface(shape = shape, color = container) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized,
                    color = foreground,
                    modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = touchAction(onEdit), enabled = enabled,
                    shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                        .semantics { contentDescription = editDescription }) {
                    Text(number, style = MaterialTheme.typography.titleMediumEmphasized)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KcalStepButton(title, number, increase = false, enabled = enabled && value > minimum) {
                    onChange((value - 1).coerceIn(minimum, maximum))
                    onFinish()
                }
                Slider(
                    value = position.coerceIn(0f, 1f), interactionSource = interaction,
                    onValueChange = { onChange(KcalSliderScale.value(index, it)) },
                    onValueChangeFinished = onFinish, enabled = enabled,
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                        if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val next = when (event.key) {
                            Key.DirectionLeft -> value + if (rtl) 1 else -1
                            Key.DirectionRight -> value + if (rtl) -1 else 1
                            Key.DirectionDown -> value - 1
                            Key.DirectionUp -> value + 1
                            Key.MoveHome -> minimum
                            Key.MoveEnd -> maximum
                            Key.PageDown -> value - (maximum - minimum) / 10
                            Key.PageUp -> value + (maximum - minimum) / 10
                            else -> return@onPreviewKeyEvent false
                        }.coerceIn(minimum, maximum)
                        if (next != value) {
                            onChange(next)
                            onFinish()
                        }
                        true
                    }.semantics {
                        contentDescription = title
                        stateDescription = number
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            value.toFloat(), minimum.toFloat()..maximum.toFloat(), maximum - minimum - 1,
                        )
                        setProgress { requested ->
                            if (!enabled || !requested.isFinite()) false else {
                                val next = requested.roundToInt().coerceIn(minimum, maximum)
                                if (next == value) false else {
                                    onChange(next)
                                    onFinish()
                                    true
                                }
                            }
                        }
                    },
                )
                KcalStepButton(title, number, increase = true, enabled = enabled && value < maximum) {
                    onChange((value + 1).coerceIn(minimum, maximum))
                    onFinish()
                }
            }
        }
    }
}

@Composable
private fun KcalStepButton(
    title: String, number: String, increase: Boolean, enabled: Boolean, onClick: () -> Unit,
) {
    val description = stringResource(
        if (increase) R.string.kcal_increase_value else R.string.kcal_decrease_value, title,
    )
    FilledTonalIconButton(
        onClick = touchAction(onClick), enabled = enabled, shapes = IconButtonDefaults.shapes(),
        modifier = Modifier.size(48.dp).semantics {
            contentDescription = description
            stateDescription = number
        },
    ) {
        Text(if (increase) "+" else "−", style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun KcalValueDialog(index: Int, value: Int, dismiss: () -> Unit, confirm: (Int) -> Unit) {
    var input by rememberSaveable(index, stateSaver = TextFieldValue.Saver) {
        val text = value.toString()
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val focus = remember { FocusRequester() }
    val range = KcalNodes.minimum(index)..KcalNodes.maximum(index)
    val parsed = input.text.toIntOrNull()?.takeIf { it in range }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(channelLabels[index])) },
        text = {
            OutlinedTextField(
                value = input, onValueChange = { edited ->
                    if (edited.text.length <= 5 && edited.text.all { it.isDigit() }) input = edited
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                label = { Text(stringResource(R.string.kcal_value_label)) },
                supportingText = { Text(stringResource(R.string.kcal_range, range.first, range.last)) },
                isError = parsed == null, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { parsed?.let(confirm) }),
            )
            LaunchedEffect(focus) { focus.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = touchAction { parsed?.let(confirm) }, enabled = parsed != null) {
                Text(stringResource(R.string.device_done))
            }
        },
        dismissButton = {
            TextButton(onClick = touchAction(dismiss)) { Text(stringResource(R.string.device_cancel)) }
        },
    )
}
