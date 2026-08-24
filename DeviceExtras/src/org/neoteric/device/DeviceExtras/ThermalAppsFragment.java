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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThermalAppsFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private PreferenceCategory mAppCategory;
    private Set<String> mSelected;

    private static class AppEntry {
        final String packageName;
        final CharSequence label;
        final Drawable icon;

        AppEntry(String packageName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getContext());
        setPreferenceScreen(screen);

        mSelected = new HashSet<>(ThermalLimiter.getSelectedApps(getContext()));

        mAppCategory = new PreferenceCategory(getContext());
        mAppCategory.setTitle(R.string.thermal_limiter_apps_title);
        screen.addPreference(mAppCategory);

        final Preference loading = new Preference(getContext());
        loading.setKey("loading");
        loading.setTitle(R.string.thermal_limiter_apps_loading);
        loading.setSelectable(false);
        mAppCategory.addPreference(loading);

        loadApps();
    }

    private void loadApps() {
        final Context context = getContext().getApplicationContext();
        new Thread(() -> {
            final List<AppEntry> entries = queryLaunchableApps(context);
            mHandler.post(() -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                populate(entries);
            });
        }, "ThermalAppsLoader").start();
    }

    private List<AppEntry> queryLaunchableApps(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> resolved = pm.queryIntentActivities(launcherIntent, 0);
        final List<AppEntry> entries = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) {
                continue;
            }
            final String packageName = info.activityInfo.packageName;
            if (packageName == null || !seen.add(packageName)) {
                continue;
            }
            entries.add(new AppEntry(packageName, info.loadLabel(pm), info.loadIcon(pm)));
        }

        final Collator collator = Collator.getInstance();
        Collections.sort(entries, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry lhs, AppEntry rhs) {
                return collator.compare(lhs.label.toString(), rhs.label.toString());
            }
        });

        return entries;
    }

    private void populate(List<AppEntry> entries) {
        mAppCategory.removeAll();

        for (AppEntry entry : entries) {
            final SwitchPreferenceCompat pref = new SwitchPreferenceCompat(getContext());
            pref.setPersistent(false);
            pref.setKey(entry.packageName);
            pref.setTitle(entry.label);
            pref.setSummary(entry.packageName);
            pref.setIcon(entry.icon);
            pref.setOnPreferenceChangeListener(this);
            mAppCategory.addPreference(pref);
            pref.setChecked(mSelected.contains(entry.packageName));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String packageName = preference.getKey();
        if ((Boolean) newValue) {
            mSelected.add(packageName);
        } else {
            mSelected.remove(packageName);
        }
        ThermalLimiter.setSelectedApps(getContext(), mSelected);
        return true;
    }
}
