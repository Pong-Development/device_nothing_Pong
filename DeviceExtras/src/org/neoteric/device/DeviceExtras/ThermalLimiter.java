/*
* Copyright (C) 2024-2026 Lunaris AOSP
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.neoteric.device.DeviceExtras;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThermalLimiter implements OnPreferenceChangeListener {
    private static final String TAG = "ThermalLimiter";

    public static final String KEY_TEMP_WARM = "thermal_limiter_temp_warm";
    public static final String KEY_TEMP_HIGH = "thermal_limiter_temp_high";
    public static final String KEY_TEMP_CRITICAL = "thermal_limiter_temp_critical";
    public static final String KEY_APPS = "thermal_limiter_apps";

    public static final int TEMP_DEFAULT_WARM = 40;
    public static final int TEMP_DEFAULT_HIGH = 43;
    public static final int TEMP_DEFAULT_CRITICAL = 45;
    public static final int TEMP_MIN = 35;
    public static final int TEMP_MAX = 60;

    private static final String BATTERY_TEMP = "/sys/class/power_supply/battery/temp";

    private static final String CPU_LITTLE_MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    private static final String[] CPU_LITTLE_CORES = {"cpu0", "cpu1", "cpu2", "cpu3"};
    private static final String CPU_MID_MAX_FREQ = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq";
    private static final String[] CPU_MID_CORES = {"cpu4", "cpu5", "cpu6"};
    private static final String CPU_PRIME_MAX_FREQ = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq";
    private static final String[] CPU_PRIME_CORES = {"cpu7"};

    private static final String GPU_MAX_FREQ = "/sys/class/kgsl/kgsl-3d0/max_gpuclk";

    private static final String CPU_LITTLE_FREQ_NORMAL = "1804800";
    private static final String CPU_LITTLE_FREQ_WARM = "1555200";
    private static final String CPU_LITTLE_FREQ_HIGH = "1324800";
    private static final String CPU_LITTLE_FREQ_CRITICAL = "1132800";

    private static final String CPU_MID_FREQ_NORMAL = "2496000";
    private static final String CPU_MID_FREQ_WARM = "2092800";
    private static final String CPU_MID_FREQ_HIGH = "1881600";
    private static final String CPU_MID_FREQ_CRITICAL = "1555200";

    private static final String CPU_PRIME_FREQ_NORMAL = "2995200";
    private static final String CPU_PRIME_FREQ_WARM = "2649600";
    private static final String CPU_PRIME_FREQ_HIGH = "2361600";
    private static final String CPU_PRIME_FREQ_CRITICAL = "1996800";

    private static final String GPU_FREQ_NORMAL = "900000000";
    private static final String GPU_FREQ_WARM = "710000000";
    private static final String GPU_FREQ_HIGH = "515000000";
    private static final String GPU_FREQ_CRITICAL = "364000000";

    public static final int STATE_NORMAL = 0;
    public static final int STATE_WARM = 1;
    public static final int STATE_HIGH = 2;
    public static final int STATE_CRITICAL = 3;

    private static final int INTERVAL_IDLE = 4000;
    private static final int INTERVAL_SCREEN_OFF = 30000;

    private static Handler mHandler;
    private static Runnable mThermalMonitor;
    private static boolean mIsMonitoring = false;
    private static int mCurrentThermalState = STATE_NORMAL;
    private static Context mContext;
    private static SharedPreferences mPrefs;
    private static BroadcastReceiver mScreenReceiver;
    private static String mThrottledPackage;
    private static volatile boolean mInteractive = true;

    private static String mLittleCeiling;
    private static String mMidCeiling;
    private static String mPrimeCeiling;

    public static boolean isSupported() {
        if (!FileUtils.isFileReadable(BATTERY_TEMP)) {
            Log.e(TAG, "Battery temperature path not readable");
            return false;
        }

        boolean cpuSupported = FileUtils.fileWritable(CPU_LITTLE_MAX_FREQ) &&
                               FileUtils.fileWritable(CPU_MID_MAX_FREQ) &&
                               FileUtils.fileWritable(CPU_PRIME_MAX_FREQ);

        if (!cpuSupported) {
            Log.w(TAG, "CPU frequency control not available");
            return false;
        }

        boolean gpuSupported = FileUtils.fileWritable(GPU_MAX_FREQ);
        if (gpuSupported) {
            Log.i(TAG, "GPU throttling enabled");
        } else {
            if (FileUtils.isFileReadable(GPU_MAX_FREQ)) {
                Log.w(TAG, "GPU path exists but not writable");
            }
        }

        return true;
    }

    public static void startMonitoring(Context context) {
        if (mIsMonitoring || !isSupported()) {
            return;
        }

        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mIsMonitoring = true;
        mHandler = new Handler(Looper.getMainLooper());
        cacheClusterCeilings();
        seedInteractiveState();

        mThermalMonitor = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(this, tick());
            }
        };

        registerScreenReceiver();
        mHandler.post(mThermalMonitor);
        Log.i(TAG, "Thermal monitoring started");
    }

    public static void stopMonitoring() {
        if (!mIsMonitoring) {
            return;
        }

        mIsMonitoring = false;
        if (mHandler != null && mThermalMonitor != null) {
            mHandler.removeCallbacks(mThermalMonitor);
        }

        unregisterScreenReceiver();
        resetFrequencies();
        ThermalLimiterTileService.refreshTile();
        mContext = null;
        mPrefs = null;
        Log.i(TAG, "Thermal monitoring stopped");
    }

    public static boolean isMonitoring() {
        return mIsMonitoring;
    }

    private static int tick() {
        if (!isInteractive()) {
            releaseThrottle();
            return INTERVAL_SCREEN_OFF;
        }

        final String foreground = getForegroundPackage();

        if (foreground == null || !getSelectedApps(mContext).contains(foreground)) {
            releaseThrottle();
            return INTERVAL_IDLE;
        }

        mThrottledPackage = foreground;
        final int temperature = getBatteryTemperature();
        adjustFrequencies(temperature);
        return getPollingInterval(temperature);
    }

    private static void releaseThrottle() {
        if (mCurrentThermalState == STATE_NORMAL) {
            mThrottledPackage = null;
            return;
        }
        Log.i(TAG, "Left " + mThrottledPackage + ", releasing throttle");
        mThrottledPackage = null;
        resetFrequencies();
        ThermalLimiterTileService.refreshTile();
    }

    private static String getForegroundPackage() {
        if (mContext == null) {
            return null;
        }

        final ActivityManager am = mContext.getSystemService(ActivityManager.class);
        if (am == null) {
            return null;
        }

        try {
            final List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty()) {
                final ActivityManager.RunningTaskInfo task = tasks.get(0);
                ComponentName component = task.topActivity != null
                        ? task.topActivity : task.baseActivity;
                if (component != null) {
                    return component.getPackageName();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getRunningTasks unavailable, falling back to process importance", e);
        }

        try {
            final List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            if (procs != null) {
                for (ActivityManager.RunningAppProcessInfo proc : procs) {
                    if (proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && proc.pkgList != null && proc.pkgList.length > 0) {
                        return proc.pkgList[0];
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve foreground package", e);
        }

        return null;
    }

    public static Set<String> getSelectedApps(Context context) {
        final SharedPreferences prefs = context != null
                ? PreferenceManager.getDefaultSharedPreferences(context) : mPrefs;
        if (prefs == null) {
            return Collections.emptySet();
        }
        return prefs.getStringSet(KEY_APPS, Collections.emptySet());
    }

    public static void setSelectedApps(Context context, Set<String> packages) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putStringSet(KEY_APPS, new HashSet<>(packages)).apply();
    }

    private static int[] getThresholds() {
        int warm = TEMP_DEFAULT_WARM;
        int high = TEMP_DEFAULT_HIGH;
        int critical = TEMP_DEFAULT_CRITICAL;

        if (mPrefs != null) {
            warm = mPrefs.getInt(KEY_TEMP_WARM, TEMP_DEFAULT_WARM);
            high = mPrefs.getInt(KEY_TEMP_HIGH, TEMP_DEFAULT_HIGH);
            critical = mPrefs.getInt(KEY_TEMP_CRITICAL, TEMP_DEFAULT_CRITICAL);
        }

        warm = Math.max(TEMP_MIN, Math.min(TEMP_MAX, warm));
        high = Math.max(warm + 1, Math.min(TEMP_MAX, high));
        critical = Math.max(high + 1, Math.min(TEMP_MAX + 2, critical));

        return new int[] {warm * 10, high * 10, critical * 10};
    }

    private static void cacheClusterCeilings() {
        mLittleCeiling = readClusterCeiling(CPU_LITTLE_CORES[0], CPU_LITTLE_FREQ_NORMAL);
        mMidCeiling = readClusterCeiling(CPU_MID_CORES[0], CPU_MID_FREQ_NORMAL);
        mPrimeCeiling = readClusterCeiling(CPU_PRIME_CORES[0], CPU_PRIME_FREQ_NORMAL);
    }

    private static String readClusterCeiling(String core, String fallback) {
        final String path = "/sys/devices/system/cpu/" + core + "/cpufreq/cpuinfo_max_freq";
        if (!FileUtils.isFileReadable(path)) {
            return fallback;
        }
        final String value = FileUtils.readOneLine(path);
        if (value == null) {
            return fallback;
        }
        try {
            final int freq = Integer.parseInt(value.trim());
            if (freq > 0) {
                return String.valueOf(freq);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Unparseable cpuinfo_max_freq for " + core + ": " + value);
        }
        return fallback;
    }

    private static int getBatteryTemperature() {
        String tempStr = FileUtils.readOneLine(BATTERY_TEMP);
        if (tempStr == null) {
            Log.e(TAG, "Failed to read battery temperature");
            return 0;
        }
        try {
            int temp = Integer.parseInt(tempStr.trim());

            if (temp > 1000) {
                temp = temp / 10;
            }

            if (temp < 0 || temp > 800) {
                Log.w(TAG, "Invalid temperature reading: " + temp);
                return 0;
            }

            return temp;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to read battery temperature", e);
            return 0;
        }
    }

    private static int getPollingInterval(int temperature) {
        final int[] thresholds = getThresholds();
        if (temperature >= thresholds[2]) return 1000;
        if (temperature >= thresholds[1]) return 1500;
        if (temperature >= thresholds[0]) return 2000;
        return 3000;
    }

    private static void setClusterFrequency(String[] cores, String frequency) {
        for (String core : cores) {
            String cpuPath = "/sys/devices/system/cpu/" + core + "/cpufreq/scaling_max_freq";
            if (FileUtils.fileWritable(cpuPath)) {
                FileUtils.writeValue(cpuPath, frequency);
            }
        }
    }

    private static void showToast(String message) {
        if (mContext != null) {
            mHandler.post(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
        }
    }

    private static void adjustFrequencies(int temperature) {
        final int[] thresholds = getThresholds();

        int newState = STATE_NORMAL;
        String littleFreq = mLittleCeiling;
        String midFreq = mMidCeiling;
        String primeFreq = mPrimeCeiling;
        String gpuFreq = GPU_FREQ_NORMAL;
        String stateName = "NORMAL";

        if (temperature >= thresholds[2]) {
            newState = STATE_CRITICAL;
            littleFreq = CPU_LITTLE_FREQ_CRITICAL;
            midFreq = CPU_MID_FREQ_CRITICAL;
            primeFreq = CPU_PRIME_FREQ_CRITICAL;
            gpuFreq = GPU_FREQ_CRITICAL;
            stateName = "CRITICAL";
        } else if (temperature >= thresholds[1]) {
            newState = STATE_HIGH;
            littleFreq = CPU_LITTLE_FREQ_HIGH;
            midFreq = CPU_MID_FREQ_HIGH;
            primeFreq = CPU_PRIME_FREQ_HIGH;
            gpuFreq = GPU_FREQ_HIGH;
            stateName = "HIGH";
        } else if (temperature >= thresholds[0]) {
            newState = STATE_WARM;
            littleFreq = CPU_LITTLE_FREQ_WARM;
            midFreq = CPU_MID_FREQ_WARM;
            primeFreq = CPU_PRIME_FREQ_WARM;
            gpuFreq = GPU_FREQ_WARM;
            stateName = "WARM";
        }

        if (newState != mCurrentThermalState) {
            mCurrentThermalState = newState;

            setClusterFrequency(CPU_LITTLE_CORES, littleFreq);
            setClusterFrequency(CPU_MID_CORES, midFreq);
            setClusterFrequency(CPU_PRIME_CORES, primeFreq);

            if (FileUtils.fileWritable(GPU_MAX_FREQ)) {
                FileUtils.writeValue(GPU_MAX_FREQ, gpuFreq);
            }

            float tempCelsius = temperature / 10.0f;
            Log.i(TAG, String.format("%s | Temp: %.1f°C | State: %s | Little: %s Mid: %s Prime: %s GPU: %s",
                mThrottledPackage, tempCelsius, stateName,
                littleFreq, midFreq, primeFreq, gpuFreq));

            if (newState > STATE_NORMAL) {
                String toastMessage = String.format("Thermal Throttling: %s (%.1f°C)", stateName, tempCelsius);
                showToast(toastMessage);
            }

            ThermalLimiterTileService.updateTileState(stateName, tempCelsius);
        }
    }

    private static void resetFrequencies() {
        setClusterFrequency(CPU_LITTLE_CORES,
                mLittleCeiling != null ? mLittleCeiling : CPU_LITTLE_FREQ_NORMAL);
        setClusterFrequency(CPU_MID_CORES,
                mMidCeiling != null ? mMidCeiling : CPU_MID_FREQ_NORMAL);
        setClusterFrequency(CPU_PRIME_CORES,
                mPrimeCeiling != null ? mPrimeCeiling : CPU_PRIME_FREQ_NORMAL);

        if (FileUtils.fileWritable(GPU_MAX_FREQ)) {
            FileUtils.writeValue(GPU_MAX_FREQ, GPU_FREQ_NORMAL);
        }

        mCurrentThermalState = STATE_NORMAL;
        Log.i(TAG, "Frequencies reset to normal");
    }

    private static boolean isInteractive() {
        return mContext != null && mInteractive;
    }

    private static void seedInteractiveState() {
        final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        final Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        mInteractive = display == null || display.getState() == Display.STATE_ON;
    }

    private static void registerScreenReceiver() {
        mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!mIsMonitoring) {
                    return;
                }
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    mInteractive = false;
                    mHandler.removeCallbacks(mThermalMonitor);
                    releaseThrottle();
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    mInteractive = true;
                    mHandler.removeCallbacks(mThermalMonitor);
                    mHandler.post(mThermalMonitor);
                }
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private static void unregisterScreenReceiver() {
        if (mContext != null && mScreenReceiver != null) {
            try {
                mContext.unregisterReceiver(mScreenReceiver);
            } catch (IllegalArgumentException e) {
                // Never registered, nothing to do
            }
        }
        mScreenReceiver = null;
    }

    public static String getStatusSummary(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(DeviceExtras.KEY_THERMAL_LIMITER, false)) {
            return context.getString(R.string.accessibility_quick_settings_off);
        }
        if (getSelectedApps(context).isEmpty()) {
            return context.getString(R.string.thermal_limiter_status_no_apps);
        }
        if (mCurrentThermalState == STATE_NORMAL) {
            return context.getString(R.string.thermal_limiter_status_idle);
        }
        return context.getString(R.string.thermal_limiter_status_throttling);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean enabled = (Boolean) newValue;

        if (enabled) {
            startMonitoring(preference.getContext());
        } else {
            stopMonitoring();
        }

        return true;
    }
}
