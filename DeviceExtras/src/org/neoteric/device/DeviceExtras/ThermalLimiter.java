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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class ThermalLimiter implements OnPreferenceChangeListener {
    private static final String TAG = "ThermalLimiter";
    
    private static final String BATTERY_TEMP = "/sys/class/power_supply/battery/temp";
    
    private static final String CPU_LITTLE_MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    private static final String[] CPU_LITTLE_CORES = {"cpu0", "cpu1", "cpu2", "cpu3"};
    private static final String CPU_MID_MAX_FREQ = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq";
    private static final String[] CPU_MID_CORES = {"cpu4", "cpu5", "cpu6"};
    private static final String CPU_PRIME_MAX_FREQ = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq";
    private static final String[] CPU_PRIME_CORES = {"cpu7"};
    
    private static final String GPU_MAX_FREQ = "/sys/class/kgsl/kgsl-3d0/max_gpuclk";
    
    private static final int TEMP_THRESHOLD_CRITICAL = 450;
    private static final int TEMP_THRESHOLD_HIGH = 430;
    private static final int TEMP_THRESHOLD_WARM = 400;
    
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
    
    private static Handler mHandler;
    private static Runnable mThermalMonitor;
    private static boolean mIsMonitoring = false;
    private static int mCurrentThermalState = 0;
    private static Context mContext;

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
        mIsMonitoring = true;
        mHandler = new Handler(Looper.getMainLooper());
        
        mThermalMonitor = new Runnable() {
            @Override
            public void run() {
                int temperature = getBatteryTemperature();
                adjustFrequencies(temperature);
                
                if (mIsMonitoring) {
                    int delay = getPollingInterval(temperature);
                    mHandler.postDelayed(this, delay);
                }
            }
        };
        
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
        
        resetFrequencies();
        mContext = null;
        Log.i(TAG, "Thermal monitoring stopped");
    }

    private static int getBatteryTemperature() {
        String tempStr = FileUtils.readOneLine(BATTERY_TEMP);
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
        if (temperature >= TEMP_THRESHOLD_CRITICAL) return 1000;
        if (temperature >= TEMP_THRESHOLD_HIGH) return 1500;
        if (temperature >= TEMP_THRESHOLD_WARM) return 2000;
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
        int newState = 0;
        String littleFreq = CPU_LITTLE_FREQ_NORMAL;
        String midFreq = CPU_MID_FREQ_NORMAL;
        String primeFreq = CPU_PRIME_FREQ_NORMAL;
        String gpuFreq = GPU_FREQ_NORMAL;
        String stateName = "NORMAL";

        if (temperature >= TEMP_THRESHOLD_CRITICAL) {
            newState = 3;
            littleFreq = CPU_LITTLE_FREQ_CRITICAL;
            midFreq = CPU_MID_FREQ_CRITICAL;
            primeFreq = CPU_PRIME_FREQ_CRITICAL;
            gpuFreq = GPU_FREQ_CRITICAL;
            stateName = "CRITICAL";
        } else if (temperature >= TEMP_THRESHOLD_HIGH) {
            newState = 2;
            littleFreq = CPU_LITTLE_FREQ_HIGH;
            midFreq = CPU_MID_FREQ_HIGH;
            primeFreq = CPU_PRIME_FREQ_HIGH;
            gpuFreq = GPU_FREQ_HIGH;
            stateName = "HIGH";
        } else if (temperature >= TEMP_THRESHOLD_WARM) {
            newState = 1;
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
            String logMessage = String.format("Temp: %.1f°C | State: %s | Little: %s Mid: %s Prime: %s GPU: %s", 
                tempCelsius, stateName,
                littleFreq.substring(0, Math.min(4, littleFreq.length())), 
                midFreq.substring(0, Math.min(4, midFreq.length())), 
                primeFreq.substring(0, Math.min(4, primeFreq.length())),
                gpuFreq.substring(0, 3));
            Log.i(TAG, logMessage);
            
            if (newState > 0) {
                String toastMessage = String.format("Thermal Throttling: %s (%.1f°C)", stateName, tempCelsius);
                showToast(toastMessage);
            }
            
            ThermalLimiterTileService.updateTileState(stateName, tempCelsius);
        }
    }

    private static void resetFrequencies() {
        setClusterFrequency(CPU_LITTLE_CORES, CPU_LITTLE_FREQ_NORMAL);
        setClusterFrequency(CPU_MID_CORES, CPU_MID_FREQ_NORMAL);
        setClusterFrequency(CPU_PRIME_CORES, CPU_PRIME_FREQ_NORMAL);
        
        if (FileUtils.fileWritable(GPU_MAX_FREQ)) {
            FileUtils.writeValue(GPU_MAX_FREQ, GPU_FREQ_NORMAL);
        }
        
        mCurrentThermalState = 0;
        Log.i(TAG, "Frequencies reset to normal");
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
