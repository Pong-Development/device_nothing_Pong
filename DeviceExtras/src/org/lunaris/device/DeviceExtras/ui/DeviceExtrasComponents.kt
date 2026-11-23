@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lunaris.device.DeviceExtras.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.lunaris.device.DeviceExtras.R

@Composable
internal fun touchAction(action: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        action()
    }
}

internal fun groupedShape(first: Boolean, last: Boolean) = RoundedCornerShape(
    topStart = if (first) 28.dp else 8.dp,
    topEnd = if (first) 28.dp else 8.dp,
    bottomStart = if (last) 28.dp else 8.dp,
    bottomEnd = if (last) 28.dp else 8.dp,
)

internal val ExtrasSegmentedGap = 2.dp

// Android 16's Material 3 prebuilt predates the expressive segmented-list APIs.
@Composable
internal fun ExtrasListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    first: Boolean = true,
    last: Boolean = true,
    selected: Boolean = false,
    checked: Boolean? = null,
    role: Role = Role.Button,
    leadingContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val rounded = selected || pressed || focused || hovered
    val motion = MaterialTheme.motionScheme
    val topCorner by animateDpAsState(
        if (first || rounded) 28.dp else 8.dp, motion.fastSpatialSpec(), label = "listTopCorner",
    )
    val bottomCorner by animateDpAsState(
        if (last || rounded) 28.dp else 8.dp, motion.fastSpatialSpec(), label = "listBottomCorner",
    )
    val shape = RoundedCornerShape(
        topStart = topCorner.coerceAtLeast(0.dp), topEnd = topCorner.coerceAtLeast(0.dp),
        bottomStart = bottomCorner.coerceAtLeast(0.dp), bottomEnd = bottomCorner.coerceAtLeast(0.dp),
    )
    val scheme = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.38f
    val container by animateColorAsState(
        if (selected) scheme.secondaryContainer else scheme.surfaceBright,
        motion.fastEffectsSpec(), label = "listContainer",
    )
    val foreground by animateColorAsState(
        (if (selected) scheme.onSecondaryContainer else scheme.onSurface).copy(alpha = alpha),
        motion.fastEffectsSpec(), label = "listForeground",
    )
    val supporting by animateColorAsState(
        (if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant).copy(alpha = alpha),
        motion.fastEffectsSpec(), label = "listSupporting",
    )
    val action = if (checked == null) {
        Modifier.clickable(
            interactionSource = interaction, indication = ripple(), enabled = enabled,
            role = role, onClick = onClick,
        )
    } else {
        Modifier.toggleable(
            value = checked, interactionSource = interaction, indication = ripple(),
            enabled = enabled, role = role, onValueChange = { onClick() },
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(motion.defaultSpatialSpec())
            .clip(shape).then(action),
        shape = shape, color = container, contentColor = foreground,
    ) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            leadingContent()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content()
                if (supportingContent != null) {
                    CompositionLocalProvider(LocalContentColor provides supporting) { supportingContent() }
                }
            }
            CompositionLocalProvider(LocalContentColor provides supporting) { trailingContent() }
        }
    }
}

@Composable
internal fun ExtrasScaffold(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    large: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val navigation: @Composable () -> Unit = {
        FilledTonalIconButton(
            onClick = touchAction(onBack),
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.padding(start = 12.dp).size(48.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.device_back))
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TwoRowsTopAppBar(
                title = { expanded ->
                    Text(title,
                        style = when {
                            !expanded -> MaterialTheme.typography.titleLargeEmphasized
                            large -> MaterialTheme.typography.headlineLargeEmphasized
                            else -> MaterialTheme.typography.headlineSmallEmphasized
                        },
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() })
                },
                subtitle = subtitle?.let { text ->
                    { expanded ->
                        Text(text,
                            style = if (expanded) MaterialTheme.typography.bodySmall
                                else MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = navigation,
                expandedHeight = if (large) {
                    if (subtitle != null) TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
                    else TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
                } else {
                    if (subtitle != null) TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
                    else TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight
                },
                colors = colors, scrollBehavior = scroll,
            )
        },
        content = content,
    )
}

@Composable
internal fun SectionHeading(
    title: String,
    temperature: Float? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized,
            color = scheme.primary,
            modifier = Modifier.weight(1f).semantics { heading() })
        if (temperature != null) {
            val description = stringResource(R.string.device_battery_temperature_description, temperature)
            val badgeContainer by animateColorAsState(
                when {
                    temperature >= 42f -> scheme.errorContainer
                    temperature >= 38f -> scheme.tertiaryContainer
                    else -> scheme.secondaryContainer
                },
                motion.defaultEffectsSpec(), label = "tempContainer",
            )
            val badgeContent by animateColorAsState(
                when {
                    temperature >= 42f -> scheme.onErrorContainer
                    temperature >= 38f -> scheme.onTertiaryContainer
                    else -> scheme.onSecondaryContainer
                },
                motion.defaultEffectsSpec(), label = "tempContent",
            )
            Surface(
                shape = CircleShape,
                color = badgeContainer,
                contentColor = badgeContent,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(painterResource(R.drawable.ic_thermal_temp), null, Modifier.size(16.dp))
                    Text(stringResource(R.string.device_temperature_value, temperature),
                        style = MaterialTheme.typography.labelMediumEmphasized)
                }
            }
        }
        action?.invoke()
    }
}

@Composable
internal fun SettingIcon(@DrawableRes icon: Int, enabled: Boolean = true, active: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val alpha = if (enabled) 1f else 0.38f
    val corner by animateDpAsState(
        if (active) 20.dp else 12.dp, motion.fastSpatialSpec(), label = "settingIconCorner",
    )
    val container by animateColorAsState(
        (if (active) scheme.primaryContainer else scheme.secondaryContainer).copy(alpha = alpha),
        motion.fastEffectsSpec(), label = "settingIconContainer",
    )
    val content by animateColorAsState(
        (if (active) scheme.onPrimaryContainer else scheme.onSecondaryContainer).copy(alpha = alpha),
        motion.fastEffectsSpec(), label = "settingIconContent",
    )
    Surface(
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        contentColor = content,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun SwitchCard(
    @DrawableRes icon: Int,
    title: String,
    detail: String?,
    checked: Boolean,
    enabled: Boolean,
    first: Boolean = true,
    last: Boolean = true,
    highlightChecked: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    val toggle = touchAction { onChange(!checked) }
    ExtrasListItem(
        onClick = toggle, enabled = enabled, first = first, last = last,
        selected = highlightChecked && checked,
        checked = checked, role = Role.Switch,
        leadingContent = { SettingIcon(icon, enabled, active = checked) },
        supportingContent = detail?.let { text ->
            { Text(text, style = MaterialTheme.typography.bodySmall) }
        },
        trailingContent = {
            Switch(
                checked = checked, enabled = enabled, onCheckedChange = null,
                colors = SwitchDefaults.colors(),
                thumbContent = {
                    Crossfade(checked, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        label = "switchIcon") { active ->
                        Icon(if (active) Icons.Default.Check else Icons.Default.Close,
                            null, Modifier.size(SwitchDefaults.IconSize))
                    }
                },
            )
        },
    ) {
        Text(title, style = if (highlightChecked && checked) MaterialTheme.typography.titleMediumEmphasized
            else MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun NavigationCard(
    @DrawableRes icon: Int,
    title: String,
    detail: String,
    summary: String? = null,
    enabled: Boolean = true,
    first: Boolean = false,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inlineDetail = maxWidth / fontScale >= 340.dp
        ExtrasListItem(
            onClick = touchAction(onClick), enabled = enabled, first = first, last = last,
            leadingContent = { SettingIcon(icon, enabled) },
            supportingContent = if (summary == null && inlineDetail) null else {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (summary != null) Text(summary, style = MaterialTheme.typography.bodySmall)
                        if (!inlineDetail) Text(detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (inlineDetail) Text(detail, style = MaterialTheme.typography.bodySmall)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            },
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Info, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StatusCard(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isError) scheme.errorContainer else scheme.secondaryContainer,
                contentColor = if (isError) scheme.onErrorContainer else scheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Info, null, Modifier.size(24.dp))
                }
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.invoke()
        }
    }
}

@Composable
internal fun RollingNumberText(
    value: Int,
    format: @Composable (Int) -> String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        label = "rollingNumber",
        transitionSpec = {
            val direction = if (targetState >= initialState) 1 else -1
            (slideInVertically(motion.fastSpatialSpec()) { direction * it / 2 } +
                fadeIn(motion.fastEffectsSpec()))
                .togetherWith(
                    slideOutVertically(motion.fastSpatialSpec()) { -direction * it / 2 } +
                        fadeOut(motion.fastEffectsSpec()),
                )
        },
    ) { current ->
        Text(format(current), style = style, textAlign = TextAlign.Center)
    }
}
