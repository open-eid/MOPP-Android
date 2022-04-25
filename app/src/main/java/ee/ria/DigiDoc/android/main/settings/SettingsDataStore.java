package ee.ria.DigiDoc.android.main.settings;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;

public final class SettingsDataStore {

    private static final String KEY_LOCALE = "locale";

    private final SharedPreferences preferences;
    private final Resources resources;

    @Inject SettingsDataStore(Application application) {
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        this.resources = application.getResources();
    }

    public int getSignatureAddMethod() {
        int signatureAddMethod = preferences.getInt(resources.getString(R.string.main_settings_signature_add_method_key), R.id.signatureUpdateSignatureAddMethodMobileId);
        Integer[] signatureAddMethods = { R.id.signatureUpdateSignatureAddMethodMobileId, R.id.signatureUpdateSignatureAddMethodSmartId, R.id.signatureUpdateSignatureAddMethodIdCard };
        if (!Arrays.asList(signatureAddMethods).contains(signatureAddMethod)) {
            return R.id.signatureUpdateSignatureAddMethodMobileId;
        }
        return signatureAddMethod;
    }

    public void setSignatureAddMethod(int method) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(resources.getString(R.string.main_settings_signature_add_method_key), method);
        editor.apply();
    }

    public String getUuid() {
        return preferences.getString(resources.getString(R.string.main_settings_uuid_key), "");
    }

    public void setUuid(String uuid) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_uuid_key), uuid);
        editor.apply();
    }

    public String getPhoneNo() {
        return preferences.getString(resources.getString(R.string.main_settings_phone_no_key), "");
    }

    public void setPhoneNo(String phoneNo) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_phone_no_key), phoneNo);
        editor.apply();
    }

    public String getPersonalCode() {
        return preferences.getString(resources.getString(R.string.main_settings_personal_code_key),
                "");
    }

    public void setPersonalCode(String personalCode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_personal_code_key),
                personalCode);
        editor.apply();
    }

    public String getSidPersonalCode() {
        return preferences.getString(resources.getString(R.string.main_settings_sid_personal_code_key),
                "");
    }

    public void setSidPersonalCode(String personalCode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_sid_personal_code_key),
                personalCode);
        editor.apply();
    }

    public String getCountry() {
        return preferences.getString(resources.getString(R.string.main_settings_smartid_country_key),
                "EE");
    }

    public void setCountry(String country) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_smartid_country_key),
                country);
        editor.apply();
    }

    @Nullable public Locale getLocale() {
        String locale = preferences.getString(KEY_LOCALE, null);
        if (locale != null) {
            return new Locale(locale);
        }
        return null;
    }

    public void setLocale(@Nullable Locale locale) {
        if (locale == null) {
            preferences.edit().remove(KEY_LOCALE).apply();
        } else {
            preferences.edit().putString(KEY_LOCALE, locale.getLanguage()).apply();
        }
    }

    public Boolean getAlwaysSendCrashReport() {
        return preferences.getBoolean(resources.getString(R.string.main_settings_crash_report_setting_key), false);
    }

    public void setAlwaysSendCrashReport(boolean alwaysSend) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_settings_crash_report_setting_key), alwaysSend);
        editor.apply();
    }

    public Boolean getShowSuccessNotification() {
        return preferences.getBoolean(resources.getString(R.string.show_success_notification_key), true);
    }

    public void setShowSuccessNotification(boolean showSuccessNotification) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.show_success_notification_key), showSuccessNotification);
        editor.apply();
    }

    public Boolean getIsDdocParentContainerTimestamped() {
        return preferences.getBoolean(resources.getString(R.string.is_ddoc_parent_container_timestamped_key), true);
    }

    public void setIsDdocParentContainerTimestamped(boolean isDdocParentContainerTimestamped) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.is_ddoc_parent_container_timestamped_key), isDdocParentContainerTimestamped);
        editor.apply();
    }

    public String getTsaUrl() {
        return preferences.getString(resources.getString(R.string.main_settings_tsa_url_key), "");
    }
}
