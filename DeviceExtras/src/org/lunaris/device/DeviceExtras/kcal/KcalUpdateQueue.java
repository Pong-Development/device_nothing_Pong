/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lunaris.device.DeviceExtras.kcal;

import java.io.IOException;
import java.util.concurrent.Executor;

public final class KcalUpdateQueue {
    public interface Applier {
        void apply(boolean enabled, int[] profile, boolean persist) throws IOException;
    }

    public interface Callback {
        void complete(IOException error);
    }

    private static final class Request {
        final boolean enabled;
        final int[] profile;
        final boolean persist;
        final Callback callback;

        Request(boolean enabled, int[] profile, boolean persist, Callback callback) {
            this.enabled = enabled;
            this.profile = profile.clone();
            this.persist = persist;
            this.callback = callback;
        }
    }

    private final Executor executor;
    private final Applier applier;
    private Request pending;
    private boolean running;

    public KcalUpdateQueue(Executor executor, Applier applier) {
        this.executor = executor;
        this.applier = applier;
    }

    public synchronized void submit(boolean enabled, int[] profile, boolean persist, Callback callback) {
        pending = new Request(enabled, profile, persist || (pending != null && pending.persist), callback);
        if (!running) {
            running = true;
            executor.execute(this::drain);
        }
    }

    private void drain() {
        while (true) {
            Request request;
            synchronized (this) {
                request = pending;
                pending = null;
                if (request == null) {
                    running = false;
                    return;
                }
            }
            IOException error = null;
            try {
                applier.apply(request.enabled, request.profile, request.persist);
            } catch (IOException e) {
                error = e;
            } catch (RuntimeException e) {
                error = new IOException("Could not update KCAL", e);
            }
            try {
                request.callback.complete(error);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
