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
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import org.neoteric.device.DeviceExtras.FileUtils;
import org.neoteric.device.DeviceExtras.R;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class DeviceExtras extends SettingsBasePreferenceFragment {
    public static final String KEY_POWERSHARE_SWITCH = "powershare";
    public static final String KEY_OTG_SWITCH = "otg";
    public static final String KEY_THERMAL_LIMITER = "thermal_limiter";

    private static TwoStatePreference mPowerShareModeSwitch;
    private static TwoStatePreference mOTGModeSwitch;
    private static TwoStatePreference mThermalLimiter;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        addPreferencesFromResource(R.xml.main);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // PowerShare
        mPowerShareModeSwitch = (TwoStatePreference) findPreference(KEY_POWERSHARE_SWITCH);
        if (PowerShareModeSwitch.isSupported()) {
            mPowerShareModeSwitch.setChecked(PowerShareModeSwitch.isCurrentlyEnabled());
            mPowerShareModeSwitch.setOnPreferenceChangeListener(new PowerShareModeSwitch());
        }

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
        if (ThermalLimiter.isSupported()) {
            mThermalLimiter.setChecked(prefs.getBoolean(KEY_THERMAL_LIMITER, false));
            mThermalLimiter.setOnPreferenceChangeListener(new ThermalLimiter());
        } else {
            getPreferenceScreen().removePreference(mThermalLimiter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        prefs.edit().putBoolean(KEY_THERMAL_LIMITER, mThermalLimiter != null && mThermalLimiter.isChecked()).apply();
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
