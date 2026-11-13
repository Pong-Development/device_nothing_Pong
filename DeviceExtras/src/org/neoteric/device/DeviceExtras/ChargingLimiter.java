package org.neoteric.device.DeviceExtras;

import android.os.SystemProperties;
import android.util.Log;

public final class ChargingLimiter {
    public static final String KEY_ENABLED = "charging_limiter";
    public static final String KEY_PROFILE = "charging_limiter_profile";
    private static final String TAG = "ChargingLimiter";
    private static final String PROPERTY_ENABLED = "persist.sys.nt_chg.enabled";
    private static final String PROPERTY_PROFILE = "persist.sys.nt_chg.profile";

    private ChargingLimiter() {}

    public static boolean isSupported() {
        return FileUtils.isFileReadable("/sys/class/qcom-battery/scenario_fcc");
    }

    public static boolean isEnabled() {
        return SystemProperties.getBoolean(PROPERTY_ENABLED, true);
    }

    public static String getProfile() {
        String profile = SystemProperties.get(PROPERTY_PROFILE, "mid");
        return isValidProfile(profile) ? profile : "mid";
    }

    public static boolean setEnabled(boolean enabled) {
        return setProperty(PROPERTY_ENABLED, Boolean.toString(enabled));
    }

    public static boolean setProfile(String profile) {
        return isValidProfile(profile) && setProperty(PROPERTY_PROFILE, profile);
    }

    private static boolean isValidProfile(String profile) {
        return "slow".equals(profile) || "mid".equals(profile) || "fast".equals(profile);
    }

    private static boolean setProperty(String key, String value) {
        try {
            SystemProperties.set(key, value);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to change charging limiter", e);
            return false;
        }
    }
}
