/*
* Copyright (C) 2018 The OmniROM Project
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

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class OTGTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
    }

    private void updateState() {
        Tile mTile = getQsTile();
        if (mTile != null) {
            boolean enabled = getEnabled();
            mTile.setSubtitle(enabled ?
                    getString(R.string.accessibility_quick_settings_on) :
                    getString(R.string.accessibility_quick_settings_off));
            mTile.setState(!OTGModeSwitch.isSupported() ? Tile.STATE_UNAVAILABLE
                    : enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            mTile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!OTGModeSwitch.setEnabled(this, !getEnabled())) {
            Toast.makeText(this, R.string.device_setting_error, Toast.LENGTH_SHORT).show();
        }
        updateState();
    }

    private boolean getEnabled() {
        return OTGModeSwitch.isCurrentlyEnabled();
    }
}
