@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lunaris.device.DeviceExtras.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    topStart = if (first) 32.dp else 4.dp,
    topEnd = if (first) 32.dp else 4.dp,
    bottomStart = if (last) 32.dp else 4.dp,
    bottomEnd = if (last) 32.dp else 4.dp,
)

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
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    val navigation: @Composable () -> Unit = {
        FilledTonalIconButton(
            onClick = touchAction(onBack),
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.padding(start = 12.dp).size(48.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.device_back))
        }
    }
    val heading: @Composable () -> Unit = {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() })
    }
    val supporting: (@Composable () -> Unit)? = subtitle?.let { text -> { Text(text) } }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (large) {
                LargeFlexibleTopAppBar(
                    title = heading, subtitle = supporting, navigationIcon = navigation,
                    colors = colors, scrollBehavior = scroll,
                )
            } else {
                MediumFlexibleTopAppBar(
                    title = heading, subtitle = supporting, navigationIcon = navigation,
                    colors = colors, scrollBehavior = scroll,
                )
            }
        },
        content = content,
    )
}

@Composable
internal fun SectionHeading(title: String, temperature: Float? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier.weight(1f).semantics { heading() })
        if (temperature != null) {
            val description = stringResource(R.string.device_battery_temperature_description, temperature)
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(painterResource(R.drawable.ic_thermal_temp), null, Modifier.size(16.dp))
                    Text(stringResource(R.string.device_temperature_value, temperature),
                        style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
        }
    }
}

@Composable
internal fun SettingIcon(@DrawableRes icon: Int, enabled: Boolean = true) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.38f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (enabled) 1f else 0.38f),
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun SwitchCard(
    @DrawableRes icon: Int,
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    first: Boolean = true,
    last: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceBright,
        MaterialTheme.motionScheme.fastEffectsSpec(), label = "switchContainer",
    )
    val toggle = touchAction { onChange(!checked) }
    val shape = groupedShape(first, last)
    Surface(
        color = container,
        shape = shape,
        modifier = Modifier.fillMaxWidth().clip(shape).toggleable(
            value = checked, enabled = enabled, role = Role.Switch, onValueChange = { toggle() },
        ),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(detail, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked, enabled = enabled, onCheckedChange = null,
                thumbContent = {
                    Crossfade(checked, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        label = "switchIcon") { active ->
                        Icon(if (active) Icons.Default.Check else Icons.Default.Close,
                            null, Modifier.size(SwitchDefaults.IconSize))
                    }
                },
            )
        }
    }
}

@Composable
internal fun NavigationCard(
    @DrawableRes icon: Int,
    title: String,
    detail: String,
    enabled: Boolean = true,
    first: Boolean = false,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f
    val shape = groupedShape(first, last)
    Surface(
        shape = shape, color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(enabled = enabled, role = Role.Button,
            onClick = touchAction(onClick)),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingIcon(icon, enabled)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                Text(detail, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
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
