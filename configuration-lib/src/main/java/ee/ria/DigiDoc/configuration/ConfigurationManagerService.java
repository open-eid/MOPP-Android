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

    public ConfigurationManagerService() {
        super(TAG);
        Timber.tag(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigurationProperties configurationProperties = new ConfigurationProperties(getAssets());
        CachedConfigurationHandler cachedConfigurationHandler = new CachedConfigurationHandler(getCacheDir());
        configurationManager = new ConfigurationManager(this, configurationProperties, cachedConfigurationHandler);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver confResultReceiver = intent.getParcelableExtra(ConfigurationConstants.CONFIGURATION_RESULT_RECEIVER);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ConfigurationConstants.CONFIGURATION_PROVIDER, getConfiguration(intent));
        confResultReceiver.send(1, bundle);
    }

    private ConfigurationProvider getConfiguration(Intent intent) {
        if (intent.getBooleanExtra(ConfigurationConstants.FORCE_LOAD_CENTRAL_CONFIGURATION, false)) {
            return configurationManager.forceLoadCentralConfiguration();
        } else {
            return configurationManager.getConfiguration();
        }
    }
}

