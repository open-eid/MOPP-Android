package ee.ria.DigiDoc.android.utils;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import timber.log.Timber;

public final class LocaleService {

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;

    @Inject public LocaleService(Navigator navigator, SettingsDataStore settingsDataStore) {
        this.navigator = navigator;
        this.settingsDataStore = settingsDataStore;
    }

    /**
     * Override this in {@link android.app.Activity#attachBaseContext(Context)}.
     */
    public Context attachBaseContext(Context context) {
        Locale locale = settingsDataStore.getLocale();
        if (locale == null) {
            return context;
        }
        Locale.setDefault(locale);
        Configuration configuration = applicationConfigurationWithLocale(context, locale);
        return context.createConfigurationContext(configuration);
    }

    /**
     * Return application-wide configuration with overridden locale
     *
     * @return Application-wide configuration with overridden locale
     */
    public Configuration applicationConfigurationWithLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return configuration;
    }

    /**
     * Return application-wide locale or system-wide locale.
     *
     * @return Application-wide or system-wide locale if none exists.
     */
    public Locale applicationLocale() {
        Locale locale = settingsDataStore.getLocale();
        return locale == null ? Locale.getDefault() : locale;
    }

    /**
     * Set application-wide locale that overrides the system-wide locale.
     *
     * @param locale Application-wide locale.
     */
    public void applicationLocale(Locale locale) {
        Timber.e("applicationLocale: %s", locale);
        settingsDataStore.setLocale(locale);
        navigator.activity().recreate();
    }
}
