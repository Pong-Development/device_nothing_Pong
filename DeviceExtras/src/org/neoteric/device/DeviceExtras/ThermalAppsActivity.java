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

import androidx.fragment.app.Fragment;
import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class ThermalAppsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.thermal_limiter_apps_title);

        Fragment fragment = getSupportFragmentManager().findFragmentById(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                .add(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                        new ThermalAppsFragment())
                .commit();
        }
    }
}
