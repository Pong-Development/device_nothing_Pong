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
package org.lunaris.device.DeviceExtras;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import org.lunaris.device.DeviceExtras.ui.DeviceExtrasUiKt;

public class ThermalAppsActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DeviceExtrasUiKt.attachDeviceExtrasUi(this, true);
    }
}
