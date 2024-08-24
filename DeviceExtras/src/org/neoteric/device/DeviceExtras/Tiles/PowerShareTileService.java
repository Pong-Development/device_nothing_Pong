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
package org.neoteric.device.DeviceExtras;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.neoteric.device.DeviceExtras.DeviceExtras;

public class PowerShareTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
    }

    private void updateState() {
        Tile mTile = getQsTile();
        boolean enabled = getEnabled();
        mTile.setSubtitle(enabled ?
                getString(R.string.accessibility_quick_settings_on) :
                getString(R.string.accessibility_quick_settings_off));
        mTile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        mTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        setEnabled(!getEnabled());
        updateState();
    }

    private boolean getEnabled() {
        return PowerShareModeSwitch.isCurrentlyEnabled();
    }

    private void setEnabled(boolean enabled) {
        FileUtils.writeValue(PowerShareModeSwitch.FILE, enabled ? "1" : "0");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.edit().putBoolean(DeviceExtras.KEY_POWERSHARE_SWITCH, enabled).apply();
    }
}
