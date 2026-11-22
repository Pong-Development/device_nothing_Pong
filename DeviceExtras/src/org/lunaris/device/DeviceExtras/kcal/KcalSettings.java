/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lunaris.device.DeviceExtras.kcal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class KcalSettings {
    public static final Executor IO = Executors.newSingleThreadExecutor();
    private static final String ENABLED = "enabled";
    private static final String VERSION = "profile_version";
    private static final int PROFILE_VERSION = 1;
    private final SharedPreferences preferences;
    private KcalNodes nodes;

    public KcalSettings(Context context) {
        preferences = context.createDeviceProtectedStorageContext()
                .getSharedPreferences("kcal", Context.MODE_PRIVATE);
    }

    public boolean discover() {
        nodes = KcalNodes.discover(Paths.get("/sys"));
        return nodes != null;
    }

    public boolean enabled() {
        return preferences.getBoolean(ENABLED, false);
    }

    public int[] profile() {
        int[] values = KcalNodes.defaults();
        boolean legacy = preferences.getInt(VERSION, 0) < PROFILE_VERSION;
        for (int i = 0; i < values.length; i++) {
            if (legacy && i >= 3) continue;
            values[i] = Math.max(KcalNodes.minimum(i), Math.min(KcalNodes.maximum(i),
                    preferences.getInt("channel_" + i, values[i])));
        }
        return values;
    }

    public void change(boolean enabled, int[] profile, boolean persist) throws IOException {
        if (nodes == null) throw new IOException("KCAL unavailable");
        try {
            nodes.apply(enabled, profile);
        } catch (IOException error) {
            try {
                nodes.apply(enabled(), profile());
            } catch (IOException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw error;
        }
        if (!persist) return;
        SharedPreferences.Editor edit = preferences.edit()
                .putBoolean(ENABLED, enabled).putInt(VERSION, PROFILE_VERSION);
        for (int i = 0; i < profile.length; i++) edit.putInt("channel_" + i, profile[i]);
        edit.apply();
    }

    public static void restore(Context context, Runnable finished) {
        KcalSettings settings = new KcalSettings(context);
        IO.execute(() -> {
            try {
                if (settings.preferences.contains(ENABLED) && settings.discover()) {
                    settings.nodes.apply(settings.enabled(), settings.profile());
                }
            } catch (IOException | RuntimeException e) {
                Log.e("DeviceExtras", "Could not restore KCAL", e);
            } finally {
                finished.run();
            }
        });
    }
}
