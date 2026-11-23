/*
 * Copyright (C) 2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package org.lunaris.device.DeviceExtras.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CancellationException

@Composable
internal fun ExtrasPageTransition(
    page: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    content: @Composable (String) -> Unit,
) {
    val navigation = remember { SeekableTransitionState(page) }
    val transition = rememberTransition(navigation, label = "deviceExtrasNavigation")
    var predictingBack by remember { mutableStateOf(false) }
    var backSequence by remember { mutableIntStateOf(0) }
    val committedPage by rememberUpdatedState(page)
    val motion = MaterialTheme.motionScheme
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1

    LaunchedEffect(page, predictingBack, backSequence) {
        if (!predictingBack) navigation.animateTo(page)
    }
    PredictiveBackHandler(enabled = canGoBack) { progress ->
        val sequence = ++backSequence
        val gesturePage = committedPage
        var started = false
        predictingBack = true
        try {
            progress.collect { event ->
                if (!started) {
                    navigation.snapTo(gesturePage)
                    started = true
                }
                navigation.seekTo(event.progress, targetState = "home")
            }
            if (backSequence == sequence && committedPage == gesturePage) onBack()
        } catch (_: CancellationException) {
        } finally {
            if (backSequence == sequence) predictingBack = false
        }
    }

    Box(Modifier.fillMaxSize().clipToBounds().background(MaterialTheme.colorScheme.background)) {
        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState == "home") {
                    (slideInHorizontally(motion.defaultSpatialSpec()) { -direction * it / 4 } +
                        fadeIn(motion.defaultEffectsSpec()))
                        .togetherWith(slideOutHorizontally(motion.defaultSpatialSpec()) { direction * it })
                        .apply { targetContentZIndex = 0f }
                        .using(null)
                } else {
                    slideInHorizontally(motion.defaultSpatialSpec()) { direction * it }
                        .togetherWith(slideOutHorizontally(motion.defaultSpatialSpec()) { -direction * it / 4 } +
                            fadeOut(motion.defaultEffectsSpec()))
                        .apply { targetContentZIndex = 1f }
                        .using(null)
                }
            },
        ) { destination ->
            val semantics = if (destination == page) Modifier else Modifier.clearAndSetSemantics {}
            Box(Modifier.fillMaxSize().then(semantics)) { content(destination) }
        }
        if (predictingBack || transition.currentState != page || transition.targetState != page) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            })
        }
    }
}
