package ee.ria.DigiDoc.configuration;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;
import timber.log.Timber;

public class ConfigurationManagerService extends IntentService {

    public static final String TAG = ConfigurationManagerService.class.getName();
    private ConfigurationManager configurationManager;
    private CachedConfigurationHandler cachedConfigurationHandler;

    public ConfigurationManagerService() {
        super(TAG);
        Timber.tag(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cachedConfigurationHandler = new CachedConfigurationHandler(getCacheDir());
        ConfigurationProperties configurationProperties = new ConfigurationProperties(getAssets());
        configurationManager = new ConfigurationManager(this, configurationProperties, cachedConfigurationHandler);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver confResultReceiver = intent.getParcelableExtra(ConfigurationConstants.CONFIGURATION_RESULT_RECEIVER);
        Bundle bundle = new Bundle();

        ConfigurationProvider configurationProvider = getConfiguration(intent);

        /*
            Hackish solution: if returned configuration provider is null, that means central configuration equals
            with currently loaded and cached configuration and no new configuration was loaded.
            But during that process last update check date was changed. Since configuration provider is immutable,
            building new provider from currently loaded one, with last update check date updated.
         */
        if (configurationProvider == null) {
            configurationProvider = intent.getParcelableExtra(ConfigurationConstants.CONFIGURATION_PROVIDER);
            configurationProvider = updateConfLastUpdateCheckDate(configurationProvider);
        }

        bundle.putParcelable(ConfigurationConstants.CONFIGURATION_PROVIDER, configurationProvider);
        confResultReceiver.send(1, bundle);
    }

    private ConfigurationProvider getConfiguration(Intent intent) {
        if (intent.getBooleanExtra(ConfigurationConstants.FORCE_LOAD_CENTRAL_CONFIGURATION, false)) {
            return configurationManager.forceLoadCentralConfiguration();
        } else {
            return configurationManager.getConfiguration();
        }
    }

    private ConfigurationProvider updateConfLastUpdateCheckDate(ConfigurationProvider configurationProvider) {
        return ConfigurationProvider.builder()
                .setMetaInf(configurationProvider.getMetaInf())
                .setConfigUrl(configurationProvider.getConfigUrl())
                .setSivaUrl(configurationProvider.getSivaUrl())
                .setTslUrl(configurationProvider.getTslUrl())
                .setTslCerts(configurationProvider.getTslCerts())
                .setTsaUrl(configurationProvider.getTsaUrl())
                .setMidSignUrl(configurationProvider.getMidSignUrl())
                .setLdapPersonUrl(configurationProvider.getLdapPersonUrl())
                .setLdapCorpUrl(configurationProvider.getLdapCorpUrl())
                .setOCSPUrls(configurationProvider.getOCSPUrls())
                .setConfigurationLastUpdateCheckDate(cachedConfigurationHandler.getConfLastUpdateCheckDate())
                .setConfigurationUpdateDate(configurationProvider.getConfigurationUpdateDate())
                .build();
    }
}

