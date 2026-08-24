/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2021 The dot X Project
* Copyright (C) 2018-2021 crDroid Android Project
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import org.neoteric.device.DeviceExtras.FileUtils;
import org.neoteric.device.DeviceExtras.R;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import java.util.Set;

public class DeviceExtras extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    public static final String KEY_OTG_SWITCH = "otg";
    public static final String KEY_THERMAL_LIMITER = "thermal_limiter";
    public static final String KEY_THERMAL_APPS_PICKER = "thermal_limiter_apps_picker";

    private static TwoStatePreference mOTGModeSwitch;
    private static TwoStatePreference mThermalLimiter;

    private Preference mThermalAppsPicker;
    private SeekBarPreference mTempWarm;
    private SeekBarPreference mTempHigh;
    private SeekBarPreference mTempCritical;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        addPreferencesFromResource(R.xml.main);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        // OTG
        mOTGModeSwitch = (TwoStatePreference) findPreference(KEY_OTG_SWITCH);
        if (OTGModeSwitch.isSupported()) {
            mOTGModeSwitch.setChecked(OTGModeSwitch.isCurrentlyEnabled());
            mOTGModeSwitch.setOnPreferenceChangeListener(new OTGModeSwitch());
        } else {
            getPreferenceScreen().removePreference(mOTGModeSwitch);
        }

        // Thermal Limiter
        mThermalLimiter = (TwoStatePreference) findPreference(KEY_THERMAL_LIMITER);
        mThermalAppsPicker = findPreference(KEY_THERMAL_APPS_PICKER);
        mTempWarm = (SeekBarPreference) findPreference(ThermalLimiter.KEY_TEMP_WARM);
        mTempHigh = (SeekBarPreference) findPreference(ThermalLimiter.KEY_TEMP_HIGH);
        mTempCritical = (SeekBarPreference) findPreference(ThermalLimiter.KEY_TEMP_CRITICAL);

        if (ThermalLimiter.isSupported()) {
            mThermalLimiter.setChecked(prefs.getBoolean(KEY_THERMAL_LIMITER, false));
            mThermalLimiter.setOnPreferenceChangeListener(new ThermalLimiter());

            mThermalAppsPicker.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), ThermalAppsActivity.class));
                return true;
            });

            mTempWarm.setOnPreferenceChangeListener(this);
            mTempHigh.setOnPreferenceChangeListener(this);
            mTempCritical.setOnPreferenceChangeListener(this);
            updateThresholdSummaries();
        } else {
            final Preference category = findPreference("thermal");
            if (category != null) {
                getPreferenceScreen().removePreference(category);
            }
            mThermalAppsPicker = null;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int value = (Integer) newValue;
        final String key = preference.getKey();

        if (ThermalLimiter.KEY_TEMP_WARM.equals(key)) {
            if (mTempHigh.getValue() <= value) {
                mTempHigh.setValue(value + 1);
            }
            if (mTempCritical.getValue() <= mTempHigh.getValue()) {
                mTempCritical.setValue(mTempHigh.getValue() + 1);
            }
        } else if (ThermalLimiter.KEY_TEMP_HIGH.equals(key)) {
            if (mTempWarm.getValue() >= value) {
                mTempWarm.setValue(value - 1);
            }
            if (mTempCritical.getValue() <= value) {
                mTempCritical.setValue(value + 1);
            }
        } else if (ThermalLimiter.KEY_TEMP_CRITICAL.equals(key)) {
            if (mTempHigh.getValue() >= value) {
                mTempHigh.setValue(value - 1);
            }
            if (mTempWarm.getValue() >= mTempHigh.getValue()) {
                mTempWarm.setValue(mTempHigh.getValue() - 1);
            }
        }

        updateThresholdSummary(mTempWarm, R.string.thermal_limiter_temp_summary,
                ThermalLimiter.KEY_TEMP_WARM.equals(key) ? value : mTempWarm.getValue());
        updateThresholdSummary(mTempHigh, R.string.thermal_limiter_temp_high_summary,
                ThermalLimiter.KEY_TEMP_HIGH.equals(key) ? value : mTempHigh.getValue());
        updateThresholdSummary(mTempCritical, R.string.thermal_limiter_temp_critical_summary,
                ThermalLimiter.KEY_TEMP_CRITICAL.equals(key) ? value : mTempCritical.getValue());
        return true;
    }

    private void updateThresholdSummaries() {
        updateThresholdSummary(mTempWarm, R.string.thermal_limiter_temp_summary,
                mTempWarm.getValue());
        updateThresholdSummary(mTempHigh, R.string.thermal_limiter_temp_high_summary,
                mTempHigh.getValue());
        updateThresholdSummary(mTempCritical, R.string.thermal_limiter_temp_critical_summary,
                mTempCritical.getValue());
    }

    private void updateThresholdSummary(SeekBarPreference preference, int resId, int value) {
        preference.setSummary(getString(resId, value));
    }

    private void updateAppsPickerSummary() {
        if (mThermalAppsPicker == null) {
            return;
        }
        final Set<String> selected = ThermalLimiter.getSelectedApps(getContext());
        if (selected.isEmpty()) {
            mThermalAppsPicker.setSummary(R.string.thermal_limiter_apps_none);
        } else {
            mThermalAppsPicker.setSummary(getResources().getQuantityString(
                    R.plurals.thermal_limiter_apps_selected, selected.size(), selected.size()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAppsPickerSummary();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
