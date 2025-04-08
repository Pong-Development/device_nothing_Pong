/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.gameoverlay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import org.lineageos.settings.R;

public class GameOverlayFragment extends PreferenceFragmentCompat {

    private GameOverlay mOverlay;

    private SwitchPreferenceCompat mMasterSwitch;

    private SwitchPreferenceCompat mFpsSwitch;
    private SwitchPreferenceCompat mBatteryTempSwitch;
    private SwitchPreferenceCompat mCpuUsageSwitch;
    private SwitchPreferenceCompat mCpuClockSwitch;
    private SwitchPreferenceCompat mCpuTempSwitch;
    private SwitchPreferenceCompat mRamSwitch;

    private SwitchPreferenceCompat mGpuUsageSwitch;
    private SwitchPreferenceCompat mGpuClockSwitch;
    private SwitchPreferenceCompat mGpuTempSwitch;

    private Preference mCaptureStartPref;
    private Preference mCaptureStopPref;
    private Preference mCaptureExportPref;

    private SwitchPreferenceCompat mDoubleTapCapturePref;
    private SwitchPreferenceCompat mSingleTapTogglePref;
    private SwitchPreferenceCompat mLongPressEnablePref;
    private ListPreference  mLongPressTimeoutPref;

    private SeekBarPreference mTextSizePref;
    private SeekBarPreference mBgAlphaPref;
    private SeekBarPreference mCornerRadiusPref;
    private SeekBarPreference mPaddingPref;
    private SeekBarPreference mItemSpacingPref;

    private ListPreference mUpdateIntervalPref;
    private ListPreference mTextColorPref;
    private ListPreference mTitleColorPref;
    private ListPreference mValueColorPref;
    private ListPreference mPositionPref;
    private ListPreference mSplitModePref;
    private ListPreference mOverlayFormatPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.game_overlay_preferences, rootKey);

        mOverlay = GameOverlay.getInstance(getContext());

        mMasterSwitch       = findPreference("game_overlay_enable");

        mFpsSwitch          = findPreference("game_overlay_fps_enable");
        mBatteryTempSwitch  = findPreference("game_overlay_temp_enable");
        mCpuUsageSwitch     = findPreference("game_overlay_cpu_usage_enable");
        mCpuClockSwitch     = findPreference("game_overlay_cpu_clock_enable");
        mCpuTempSwitch      = findPreference("game_overlay_cpu_temp_enable");
        mRamSwitch          = findPreference("game_overlay_ram_enable");

        mGpuUsageSwitch     = findPreference("game_overlay_gpu_usage_enable");
        mGpuClockSwitch     = findPreference("game_overlay_gpu_clock_enable");
        mGpuTempSwitch      = findPreference("game_overlay_gpu_temp_enable");

        mCaptureStartPref   = findPreference("game_overlay_capture_start");
        mCaptureStopPref    = findPreference("game_overlay_capture_stop");
        mCaptureExportPref  = findPreference("game_overlay_capture_export");

        mDoubleTapCapturePref = findPreference("game_overlay_doubletap_capture");
        mSingleTapTogglePref  = findPreference("game_overlay_single_tap_toggle");
        mLongPressEnablePref  = findPreference("game_overlay_longpress_enable");
        mLongPressTimeoutPref = findPreference("game_overlay_longpress_timeout");

        mTextSizePref       = findPreference("game_overlay_text_size");
        mBgAlphaPref        = findPreference("game_overlay_background_alpha");
        mCornerRadiusPref   = findPreference("game_overlay_corner_radius");
        mPaddingPref        = findPreference("game_overlay_padding");
        mItemSpacingPref    = findPreference("game_overlay_item_spacing");

        mUpdateIntervalPref = findPreference("game_overlay_update_interval");
        mTextColorPref      = findPreference("game_overlay_text_color");
        mTitleColorPref     = findPreference("game_overlay_title_color");
        mValueColorPref     = findPreference("game_overlay_value_color");
        mPositionPref       = findPreference("game_overlay_position");
        mSplitModePref      = findPreference("game_overlay_split_mode");
        mOverlayFormatPref  = findPreference("game_overlay_format");

        if (mMasterSwitch != null) {
            mMasterSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (boolean) newValue;
                if (enabled) {
                    if (Settings.canDrawOverlays(getContext())) {
                        mOverlay.applyPreferences();
                        mOverlay.show();
                    } else {
                        Toast.makeText(getContext(), R.string.overlay_permission_required, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    mOverlay.hide();
                }
                return true;
            });
        }

        if (mFpsSwitch != null) {
            mFpsSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowFps((boolean) newValue);
                return true;
            });
        }
        if (mBatteryTempSwitch != null) {
            mBatteryTempSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowBatteryTemp((boolean) newValue);
                return true;
            });
        }
        if (mCpuUsageSwitch != null) {
            mCpuUsageSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowCpuUsage((boolean) newValue);
                return true;
            });
        }
        if (mCpuClockSwitch != null) {
            mCpuClockSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowCpuClock((boolean) newValue);
                return true;
            });
        }
        if (mCpuTempSwitch != null) {
            mCpuTempSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowCpuTemp((boolean) newValue);
                return true;
            });
        }
        if (mRamSwitch != null) {
            mRamSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowRam((boolean) newValue);
                return true;
            });
        }

        if (mGpuUsageSwitch != null) {
            mGpuUsageSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowGpuUsage((boolean) newValue);
                return true;
            });
        }
        if (mGpuClockSwitch != null) {
            mGpuClockSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowGpuClock((boolean) newValue);
                return true;
            });
        }
        if (mGpuTempSwitch != null) {
            mGpuTempSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setShowGpuTemp((boolean) newValue);
                return true;
            });
        }

        if (mCaptureStartPref != null) {
            mCaptureStartPref.setOnPreferenceClickListener(pref -> {
                GameDataExport.getInstance().startCapture();
                Toast.makeText(getContext(), "Started logging Data", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        if (mCaptureStopPref != null) {
            mCaptureStopPref.setOnPreferenceClickListener(pref -> {
                GameDataExport.getInstance().stopCapture();
                Toast.makeText(getContext(), "Stopped logging Data", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        if (mCaptureExportPref != null) {
            mCaptureExportPref.setOnPreferenceClickListener(pref -> {
                GameDataExport.getInstance().exportDataToCsv();
                Toast.makeText(getContext(), "Exported log data to file", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        if (mDoubleTapCapturePref != null) {
            mDoubleTapCapturePref.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setDoubleTapCaptureEnabled((boolean) newValue);
                return true;
            });
        }
        if (mSingleTapTogglePref != null) {
            mSingleTapTogglePref.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setSingleTapToggleEnabled((boolean) newValue);
                return true;
            });
        }
        if (mLongPressEnablePref != null) {
            mLongPressEnablePref.setOnPreferenceChangeListener((pref, newValue) -> {
                mOverlay.setLongPressEnabled((boolean) newValue);
                return true;
            });
        }
        if (mLongPressTimeoutPref != null) {
            mLongPressTimeoutPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    long ms = Long.parseLong((String) newValue);
                    mOverlay.setLongPressThresholdMs(ms);
                }
                return true;
            });
        }

        if (mTextSizePref != null) {
            mTextSizePref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof Integer) {
                    mOverlay.updateTextSize((Integer) newValue);
                }
                return true;
            });
        }
        if (mBgAlphaPref != null) {
            mBgAlphaPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof Integer) {
                    mOverlay.updateBackgroundAlpha((Integer) newValue);
                }
                return true;
            });
        }
        if (mCornerRadiusPref != null) {
            mCornerRadiusPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof Integer) {
                    mOverlay.updateCornerRadius((Integer) newValue);
                }
                return true;
            });
        }
        if (mPaddingPref != null) {
            mPaddingPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof Integer) {
                    mOverlay.updatePadding((Integer) newValue);
                }
                return true;
            });
        }

        if (mItemSpacingPref != null) {
            mItemSpacingPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof Integer) {
                    mOverlay.updateItemSpacing((Integer) newValue);
                }
                return true;
            });
        }

        if (mUpdateIntervalPref != null) {
            mUpdateIntervalPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    mOverlay.updateUpdateInterval((String) newValue);
                }
                return true;
            });
        }
        if (mTextColorPref != null) {
            mTextColorPref.setOnPreferenceChangeListener((pref, newValue) -> true);
        }
        if (mTitleColorPref != null) {
            mTitleColorPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    mOverlay.updateTitleColor((String) newValue);
                }
                return true;
            });
        }
        if (mValueColorPref != null) {
            mValueColorPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    mOverlay.updateValueColor((String) newValue);
                }
                return true;
            });
        }
        if (mPositionPref != null) {
            mPositionPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    mOverlay.updatePosition((String) newValue);
                }
                return true;
            });
        }
        if (mSplitModePref != null) {
            mSplitModePref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    mOverlay.updateSplitMode((String) newValue);
                }
                return true;
            });
        }
        if (mOverlayFormatPref != null) {
            mOverlayFormatPref.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue instanceof String) {
                    mOverlay.updateOverlayFormat((String) newValue);
                }
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasUsageStatsPermission(requireContext())) {
            requestUsageStatsPermission();
        }
    }

    private boolean hasUsageStatsPermission(Context context) {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager)
                context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.getPackageName()
        );
        return (mode == android.app.AppOpsManager.MODE_ALLOWED);
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }
}
