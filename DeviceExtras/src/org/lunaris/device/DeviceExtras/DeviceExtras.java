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
package org.lunaris.device.DeviceExtras;

import android.app.Application;
import androidx.preference.PreferenceManager;

public final class DeviceExtras extends Application {
    public static final String KEY_OTG_SWITCH = "otg";
    public static final String KEY_THERMAL_LIMITER = "thermal_limiter";
    public static final String KEY_THERMAL_APPS_PICKER = "thermal_limiter_apps_picker";

    @Override
    public void onCreate() {
        super.onCreate();

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(KEY_THERMAL_LIMITER, false)) {
            ThermalLimiter.startMonitoring(this);
        }
    }
}
