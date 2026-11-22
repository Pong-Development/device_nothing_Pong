/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lunaris.device.DeviceExtras.kcal;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KcalNodes {
    public static final int CHANNEL_COUNT = 7;
    private static final int[] DEFAULT = {256, 256, 256, 255, 255, 255, 0};
    private static final int[] MIN = {20, 20, 20, 224, 128, 128, 0};
    private static final int[] MAX = {256, 256, 256, 383, 383, 383, 1536};
    private static final int[] RAW_MIN = {20, 20, 20, 0, 0, 0, 0};
    private static final int[] RAW_MAX = {256, 256, 256, 65535, 255, 255, 4095};
    private static final String[] NAMES = {
            "kcal_red", "kcal_green", "kcal_blue", "kcal_sat", "kcal_val",
            "kcal_cont", "kcal_hue"
    };
    private final Path[] channels = new Path[CHANNEL_COUNT];

    private KcalNodes(Path directory) {
        for (int i = 0; i < channels.length; i++) channels[i] = directory.resolve(NAMES[i]);
    }

    public static int[] defaults() {
        return DEFAULT.clone();
    }

    public static int defaultValue(int channel) {
        return DEFAULT[channel];
    }

    public static int minimum(int channel) {
        return MIN[channel];
    }

    public static int maximum(int channel) {
        return MAX[channel];
    }

    static KcalNodes discover(Path sys) {
        KcalNodes nodes = new KcalNodes(sys.resolve("module/msm_drm/parameters"));
        for (Path path : nodes.channels) {
            if (!Files.isRegularFile(path) || !Files.isReadable(path) || !Files.isWritable(path)) {
                return null;
            }
        }
        try {
            nodes.snapshot();
            return nodes;
        } catch (IOException e) {
            return null;
        }
    }

    private static int read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.US_ASCII)) {
            String line = reader.readLine();
            if (line == null) throw new IOException("Empty KCAL attribute: " + path);
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Invalid KCAL attribute: " + path, e);
        }
    }

    private Map<Path, Integer> snapshot() throws IOException {
        Map<Path, Integer> state = new LinkedHashMap<>();
        for (int i = 0; i < channels.length; i++) {
            int value = read(channels[i]);
            if (value < RAW_MIN[i] || value > RAW_MAX[i]) {
                throw new IOException("KCAL channel out of range: " + channels[i]);
            }
            state.put(channels[i], value);
        }
        return state;
    }

    private static void write(Path path, int value) throws IOException {
        Files.write(path, (value + "\n").getBytes(StandardCharsets.US_ASCII),
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static int nativeValue(int channel, int value) {
        return channel == 4 || channel == 5 ? value & 0xff : value;
    }

    void apply(boolean enabled, int[] profile) throws IOException {
        if (profile == null || profile.length != CHANNEL_COUNT) {
            throw new IOException("Invalid KCAL profile length");
        }
        for (int i = 0; i < profile.length; i++) {
            if (profile[i] < MIN[i] || profile[i] > MAX[i]) {
                throw new IOException("Invalid KCAL channel " + i + ": " + profile[i]);
            }
        }
        Map<Path, Integer> before = snapshot();
        int[] target = enabled ? profile : DEFAULT;
        Map<Path, Integer> undo = new LinkedHashMap<>();
        try {
            for (int i = 0; i < channels.length; i++) {
                Path path = channels[i];
                int desired = nativeValue(i, target[i]);
                if (desired == before.get(path)) continue;
                undo.put(path, before.get(path));
                write(path, desired);
                if (read(path) != desired) throw new IOException("KCAL readback mismatch: " + path);
            }
        } catch (IOException error) {
            ArrayList<Path> changed = new ArrayList<>(undo.keySet());
            for (int i = changed.size() - 1; i >= 0; i--) {
                Path path = changed.get(i);
                try {
                    write(path, undo.get(path));
                } catch (IOException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
            }
            throw error;
        }
    }
}
