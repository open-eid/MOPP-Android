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

package ee.ria.DigiDoc.preferences.accessor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppPreferences {

    public static final String MOBILE_NUMBER_KEY = "mobile_nr";
    public static final String PERSONAL_CODE_KEY = "personal_code";
    public static final String SIGNATURE_PROFILE_KEY = "container_file_type";
    public static final String BDOC_CONTAINER_TYPE = "bdoc";
    public static final String ASICE_CONTAINER_TYPE = "asice";
    public static final String TIME_MARK_PROFILE = "time-mark";
    public static final String TIME_STAMP_PROFILE = "time-stamp";
    public static final String DEFAULT_SINGATURE_PROFILE = TIME_MARK_PROFILE;

    private SharedPreferences preferences;

    private AppPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static AppPreferences get(Context context) {
        return new AppPreferences(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public String getMobileNumber() {
        return preferences.getString(MOBILE_NUMBER_KEY, "");
    }

    public String getPersonalCode() {
        return preferences.getString(PERSONAL_CODE_KEY, "");
    }

    public String getSignatureProfile() {
        return preferences.getString(SIGNATURE_PROFILE_KEY, DEFAULT_SINGATURE_PROFILE);
    }

    public String getContainerFormat() {
        return isTimeMarkProfile() ? BDOC_CONTAINER_TYPE : ASICE_CONTAINER_TYPE;
    }

    private boolean isTimeMarkProfile() {
        return TIME_MARK_PROFILE.equals(getSignatureProfile());
    }

    public void updateMobileNumber(String newMobileNumber) {
        updateString(MOBILE_NUMBER_KEY, newMobileNumber);
    }

    public void updatePersonalCode(String newPersonalCode) {
        updateString(PERSONAL_CODE_KEY, newPersonalCode);
    }

    private void updateString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

}
