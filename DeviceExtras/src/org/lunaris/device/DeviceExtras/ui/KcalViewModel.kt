/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lunaris.device.DeviceExtras.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lunaris.device.DeviceExtras.R
import org.lunaris.device.DeviceExtras.kcal.KcalNodes
import org.lunaris.device.DeviceExtras.kcal.KcalSettings
import org.lunaris.device.DeviceExtras.kcal.KcalUpdateQueue

data class KcalState(
    val loading: Boolean = true,
    val supported: Boolean = false,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val enabled: Boolean = false,
    val profile: List<Int> = KcalNodes.defaults().toList(),
)

class KcalViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = KcalSettings(application)
    private val updates = KcalUpdateQueue(KcalSettings.IO, settings::change)
    private val main = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(KcalState())
    val state = mutableState.asStateFlow()
    private val mutableMessages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val messages = mutableMessages.asSharedFlow()
    private var generation = 0L
    private var dirty = false
    private var reading = false
    private var cleared = false
    private var preview: Job? = null
    private var autosave: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (reading || dirty || mutableState.value.saving || cleared) return
        reading = true
        val task = generation
        KcalSettings.IO.execute {
            val snapshot = try {
                KcalState(loading = false, supported = settings.discover(),
                    enabled = settings.enabled(), profile = settings.profile().toList())
            } catch (exception: Exception) {
                Log.e("DeviceExtras", "Could not read KCAL", exception)
                KcalState(loading = false, loadFailed = true)
            }
            main.post {
                reading = false
                if (!cleared && task == generation) mutableState.value = snapshot
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (!canChange()) return
        cancelScheduled()
        mutableState.update { it.copy(enabled = enabled) }
        submit(persist = true, lockControls = true)
    }

    fun reset() {
        if (!canChange()) return
        cancelScheduled()
        mutableState.update { it.copy(profile = KcalNodes.defaults().toList()) }
        submit(persist = true, lockControls = true)
    }

    fun setChannel(index: Int, value: Int) {
        if (!canChange() || !mutableState.value.enabled || index !in 0 until KcalNodes.CHANNEL_COUNT) return
        val channel = value.coerceIn(KcalNodes.minimum(index), KcalNodes.maximum(index))
        if (mutableState.value.profile[index] == channel) return
        generation++
        mutableState.update { current ->
            current.copy(profile = current.profile.toMutableList().also { it[index] = channel })
        }
        dirty = true
        if (preview?.isActive != true) {
            preview = viewModelScope.launch {
                delay(16)
                submit(persist = false)
            }
        }
        autosave?.cancel()
        autosave = viewModelScope.launch {
            delay(300)
            save()
        }
    }

    fun save() {
        if (!dirty) return
        cancelScheduled()
        submit(persist = true)
    }

    private fun canChange() = mutableState.value.let {
        it.supported && !it.loading && !it.loadFailed && !it.saving && !cleared
    }

    private fun cancelScheduled() {
        preview?.cancel()
        autosave?.cancel()
    }

    private fun submit(persist: Boolean, lockControls: Boolean = false) {
        val current = mutableState.value
        val task = ++generation
        if (persist) dirty = false
        if (lockControls) mutableState.update { it.copy(saving = true) }
        updates.submit(current.enabled, current.profile.toIntArray(), persist) { error ->
            val saved = if (error != null) settings.profile().toList() else null
            val enabled = if (error != null) settings.enabled() else current.enabled
            if (error != null) Log.e("DeviceExtras", "Could not apply KCAL", error)
            main.post {
                if (!cleared && task == generation) {
                    if (error == null) {
                        mutableState.update { it.copy(saving = false) }
                    } else {
                        cancelScheduled()
                        dirty = false
                        mutableState.update { it.copy(saving = false, enabled = enabled, profile = saved!!) }
                        mutableMessages.tryEmit(R.string.kcal_error)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        save()
        cleared = true
        cancelScheduled()
        super.onCleared()
    }
}
