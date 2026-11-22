/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lunaris.device.DeviceExtras.ui

import kotlin.math.roundToInt
import org.lunaris.device.DeviceExtras.kcal.KcalNodes

internal object KcalSliderScale {
    fun position(channel: Int, value: Int): Float {
        val min = KcalNodes.minimum(channel)
        val max = KcalNodes.maximum(channel)
        val bounded = value.coerceIn(min, max)
        val center = KcalNodes.defaultValue(channel)
        return if (channel in 3..5 && bounded <= center) {
            0.5f * (bounded - min) / (center - min)
        } else if (channel in 3..5) {
            0.5f + 0.5f * (bounded - center) / (max - center)
        } else (bounded - min).toFloat() / (max - min)
    }

    fun value(channel: Int, position: Float): Int {
        val min = KcalNodes.minimum(channel)
        val max = KcalNodes.maximum(channel)
        val bounded = position.coerceIn(0f, 1f)
        val center = KcalNodes.defaultValue(channel)
        return (if (channel in 3..5 && bounded <= 0.5f) {
            min + bounded.toDouble() * 2 * (center - min)
        } else if (channel in 3..5) {
            center + (bounded.toDouble() - 0.5) * 2 * (max - center)
        } else min + bounded.toDouble() * (max - min)).roundToInt().coerceIn(min, max)
    }
}
