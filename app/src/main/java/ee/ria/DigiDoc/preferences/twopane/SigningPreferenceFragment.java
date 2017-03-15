/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.preferences.twopane;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.preferences.SettingsActivity;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SigningPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_signing);
        setHasOptionsMenu(true);

        bindPreferencesSummariesToValues(
                "container_file_type",
                "personal_code",
                "mobile_nr"
        );
    }

    private void bindPreferencesSummariesToValues(String... preferenceKey) {
        for (String key : preferenceKey) {
            SettingsActivity.bindPreferenceSummaryToValue(findPreference(key));
        }
    }
}
