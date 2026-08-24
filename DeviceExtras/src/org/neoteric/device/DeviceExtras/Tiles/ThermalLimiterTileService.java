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

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.neoteric.device.DeviceExtras.DeviceExtras;

public class ThermalLimiterTileService extends TileService {

    private static ThermalLimiterTileService sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
    }

    public static void updateTileState(String state, float temperature) {
        if (sInstance != null) {
            sInstance.updateStateWithInfo(state, temperature);
        }
    }

    public static void refreshTile() {
        if (sInstance != null) {
            sInstance.updateState();
        }
    }

    private void updateStateWithInfo(String state, float temperature) {
        Tile mTile = getQsTile();
        if (mTile != null) {
            if (getEnabled()) {
                mTile.setSubtitle(String.format("%s (%.1f°C)", state, temperature));
                mTile.setState(Tile.STATE_ACTIVE);
            } else {
                mTile.setSubtitle(getString(R.string.accessibility_quick_settings_off));
                mTile.setState(Tile.STATE_INACTIVE);
            }

            mTile.updateTile();
        }
    }

    private void updateState() {
        Tile mTile = getQsTile();
        if (mTile != null) {
            boolean enabled = getEnabled();

            mTile.setSubtitle(ThermalLimiter.getStatusSummary(this));
            mTile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);

            mTile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean newState = !getEnabled();
        setEnabled(newState);

        if (newState) {
            ThermalLimiter.startMonitoring(this);
        } else {
            ThermalLimiter.stopMonitoring();
        }

        updateState();
    }

    private boolean getEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(DeviceExtras.KEY_THERMAL_LIMITER, false);
    }

    private void setEnabled(boolean enabled) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.edit().putBoolean(DeviceExtras.KEY_THERMAL_LIMITER, enabled).apply();
    }
}
