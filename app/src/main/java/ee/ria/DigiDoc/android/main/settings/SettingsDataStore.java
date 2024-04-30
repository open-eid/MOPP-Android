package ee.ria.DigiDoc.android.main.settings;

import static ee.ria.DigiDoc.common.ProxySetting.NO_PROXY;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.signing.siva.SivaSetting;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.ViewType;
import ee.ria.DigiDoc.common.EncryptedPreferences;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.ProxySetting;
import timber.log.Timber;

public final class SettingsDataStore {

    private static final String KEY_LOCALE = "locale";

    private final SharedPreferences preferences;
    private final SharedPreferences encryptedPreferences;
    private final Resources resources;

    @Inject SettingsDataStore(Application application) {
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        encryptedPreferences = getEncryptedPreferences(application.getApplicationContext());
        this.resources = application.getResources();
    }

    public int getSignatureAddMethod() {
        int signatureAddMethod = preferences.getInt(resources.getString(R.string.main_settings_signature_add_method_key), R.id.signatureUpdateSignatureAddMethodMobileId);
        Integer[] signatureAddMethods = { R.id.signatureUpdateSignatureAddMethodMobileId, R.id.signatureUpdateSignatureAddMethodSmartId, R.id.signatureUpdateSignatureAddMethodIdCard, R.id.signatureUpdateSignatureAddMethodNFC };
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

    public String getCan() {
        if (encryptedPreferences != null) {
            return encryptedPreferences.getString(resources.getString(R.string.main_settings_can_key), "");
        }
        Timber.log(Log.ERROR, "Unable to read CAN");
        return "";
    }

    public void setCan(String can) {
        if (encryptedPreferences != null) {
            SharedPreferences.Editor editor = encryptedPreferences.edit();
            editor.putString(resources.getString(R.string.main_settings_can_key), can);
            editor.commit();
            return;
        }
        Timber.log(Log.ERROR, "Unable to save CAN");
    }

    public boolean getIsRoleAskingEnabled() {
        return preferences.getBoolean(resources.getString(R.string.main_settings_ask_role_and_address_key),
                false);
    }

    public void setIsRoleAskingEnabled(boolean isRoleAskingEnabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_settings_ask_role_and_address_key),
                isRoleAskingEnabled);
        editor.apply();
    }

    public List<String> getRoles() {
        String rolesList = preferences.getString(resources.getString(R.string.main_settings_role_key),
                    "");
        return Arrays.asList(rolesList.split(","));
    }

    public void setRoles(List<String> roles) {
        SharedPreferences.Editor editor = preferences.edit();
        String rolesList = String.join(",", roles);
        editor.putString(resources.getString(R.string.main_settings_role_key),
                rolesList);
        editor.apply();
    }

    public String getRoleCity() {
        return preferences.getString(resources.getString(R.string.main_settings_city_key),
                "");
    }

    public void setRoleCity(String city) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_city_key),
                city);
        editor.apply();
    }

    public String getRoleState() {
        return preferences.getString(resources.getString(R.string.main_settings_county_key),
                "");
    }

    public void setRoleState(String state) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_county_key),
                state);
        editor.apply();
    }

    public String getRoleCountry() {
        return preferences.getString(resources.getString(R.string.main_settings_country_key),
                "");
    }

    public void setRoleCountry(String country) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_country_key),
                country);
        editor.apply();
    }

    public String getRoleZip() {
        return preferences.getString(resources.getString(R.string.main_settings_postal_code_key),
                "");
    }

    public void setRoleZip(String zip) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_postal_code_key),
                zip);
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

    public Boolean getIsDdocParentContainerTimestamped() {
        return preferences.getBoolean(resources.getString(R.string.is_ddoc_parent_container_timestamped_key), true);
    }

    public void setIsDdocParentContainerTimestamped(boolean isDdocParentContainerTimestamped) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.is_ddoc_parent_container_timestamped_key), isDdocParentContainerTimestamped);
        editor.apply();
    }

    public Boolean getIsOpenAllFileTypesEnabled() {
        return preferences.getBoolean(resources.getString(R.string.main_settings_open_all_filetypes_key), true);
    }

    public void setIsOpenAllFileTypesEnabled(boolean isEnabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_settings_open_all_filetypes_key), isEnabled);
        editor.commit();
    }

    public Boolean getIsScreenshotAllowed() {
        return preferences.getBoolean(resources.getString(R.string.main_settings_allow_screenshots_key), false);
    }

    public void setIsScreenshotAllowed(boolean isEnabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_settings_allow_screenshots_key), isEnabled);
        editor.commit();
    }

    public void setTsaUrl(String tsaUrl) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_tsa_url_key), tsaUrl);
        editor.commit();
    }

    public String getTsaUrl() {
        return preferences.getString(resources.getString(R.string.main_settings_tsa_url_key), "");
    }

    public Boolean getIsLogFileGenerationEnabled() {
        return preferences.getBoolean(resources.getString(R.string.main_diagnostics_logging_key), false);
    }

    public void setIsLogFileGenerationEnabled(boolean isEnabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_diagnostics_logging_key), isEnabled);
        editor.commit();
    }

    public void setIsLogFileGenerationRunning(boolean isRunning) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_diagnostics_logging_running_key), isRunning);
        editor.commit();
    }

    public void setTSACertName(String cert) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_tsa_cert_key), cert);
        editor.commit();
    }

    public String getTSACertName() {
        return preferences.getString(resources.getString(R.string.main_settings_tsa_cert_key), "");
    }

    public void setIsTsaCertificateViewVisible(boolean isVisible) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(resources.getString(R.string.main_settings_tsa_cert_view), isVisible);
        editor.commit();
    }

    public boolean getIsTsaCertificateViewVisible() {
        return preferences.getBoolean(resources.getString(R.string.main_settings_tsa_cert_view), false);
    }

    public void setSivaSetting(SivaSetting sivaSetting) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_siva_setting_key), sivaSetting.name());
        editor.commit();
    }

    public SivaSetting getSivaSetting() {
        String sivaSetting = preferences.getString(resources.getString(R.string.main_settings_siva_setting_key), SivaSetting.DEFAULT.name());
        try {
            return SivaSetting.valueOf(sivaSetting);
        } catch (IllegalArgumentException iae) {
            Timber.log(Log.ERROR, iae, "Unable to get SiVa setting value");
            return SivaSetting.DEFAULT;
        }
    }

    public void setSivaUrl(String sivaUrl) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_siva_url_key), sivaUrl);
        editor.commit();
    }

    public String getSivaUrl() {
        return preferences.getString(resources.getString(R.string.main_settings_siva_url_key), "");
    }

    public void setSivaCertName(String cert) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_siva_cert_key), cert);
        editor.commit();
    }

    public String getSivaCertName() {
        return preferences.getString(resources.getString(R.string.main_settings_siva_cert_key), "");
    }

    public void setProxySetting(ProxySetting proxySetting) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_proxy_setting_key), proxySetting.name());
        editor.commit();
    }

    public ProxySetting getProxySetting() {
        String settingKey = preferences.getString(resources.getString(R.string.main_settings_proxy_setting_key), NO_PROXY.name());
        try {
            return ProxySetting.valueOf(settingKey);
        } catch (IllegalArgumentException iae) {
            Timber.log(Log.ERROR, iae, "Unable to get proxy setting");
            return NO_PROXY;
        }
    }

    public void setProxyHost(String host) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_proxy_host_key), host);
        editor.commit();
    }

    public String getProxyHost() {
        return preferences.getString(resources.getString(R.string.main_settings_proxy_host_key), "");
    }

    public void setProxyPort(int port) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(resources.getString(R.string.main_settings_proxy_port_key), port);
        editor.commit();
    }

    public int getProxyPort() {
        return preferences.getInt(resources.getString(R.string.main_settings_proxy_port_key), 80);
    }

    public void setProxyUsername(String username) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.main_settings_proxy_username_key), username);
        editor.commit();
    }

    public String getProxyUsername() {
        return preferences.getString(resources.getString(R.string.main_settings_proxy_username_key), "");
    }

    public void setProxyPassword(Context context, String password) {
        SharedPreferences encryptedPreferences = getEncryptedPreferences(context);
        if (encryptedPreferences != null) {
            SharedPreferences.Editor editor = encryptedPreferences.edit();
            editor.putString(resources.getString(R.string.main_settings_proxy_password_key), password);
            editor.commit();
        }
        Timber.log(Log.ERROR, "Unable to set proxy password");
    }

    public String getProxyPassword(Context context) {
        SharedPreferences encryptedPreferences = getEncryptedPreferences(context);
        if (encryptedPreferences != null) {
            return encryptedPreferences.getString(resources.getString(R.string.main_settings_proxy_password_key), "");
        }
        Timber.log(Log.ERROR, "Unable to get proxy password");
        return "";
    }

    public ManualProxy getManualProxySettings(Context context) {
        return new ManualProxy(getProxyHost(), getProxyPort(),
                getProxyUsername(), getProxyPassword(context));
    }

    public void setViewType(ViewType type) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.view_type_key), type.name());
        editor.commit();
    }

    public ViewType getViewType() {
        String viewTypeKey = preferences.getString(resources.getString(R.string.view_type_key), ViewType.MAIN.name());
        try {
            return ViewType.valueOf(viewTypeKey);
        } catch (IllegalArgumentException iae) {
            Timber.log(Log.ERROR, iae, "Unable to get view type setting");
            return ViewType.MAIN;
        }
    }

    public void setLibdigidocppVersion(String version) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(resources.getString(R.string.digidoc_version_key), version);
        editor.commit();
    }

    public String getLibdigidocppVersion() {
        return preferences.getString(resources.getString(R.string.digidoc_version_key), "");
    }

    @Nullable
    private static SharedPreferences getEncryptedPreferences(Context context) {
        try {
            return EncryptedPreferences.getEncryptedPreferences(context);
        } catch (GeneralSecurityException | IOException e) {
            Timber.log(Log.ERROR, e, "Unable to get encrypted preferences");
            ToastUtil.showError(context, R.string.signature_update_mobile_id_error_general_client);
            return null;
        }
    }
}
