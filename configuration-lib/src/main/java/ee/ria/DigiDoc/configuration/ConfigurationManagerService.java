package ee.ria.DigiDoc.configuration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import java.util.Date;

import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;

public class ConfigurationManagerService extends JobIntentService {

    public static final int INIT_LIBDIGIDOC_RESULT_CODE = 1;

    private ConfigurationManager configurationManager;
    private CachedConfigurationHandler cachedConfigurationHandler;

    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, ConfigurationManagerService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cachedConfigurationHandler = new CachedConfigurationHandler(getCacheDir());
        ConfigurationProperties configurationProperties = new ConfigurationProperties(getAssets());
        configurationManager = new ConfigurationManager(this, configurationProperties, cachedConfigurationHandler);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        ResultReceiver confResultReceiver = intent.getParcelableExtra(ConfigurationConstants.CONFIGURATION_RESULT_RECEIVER);
        Bundle bundle = new Bundle();

        ConfigurationProvider configurationProvider = getConfiguration(intent);
        int resultCode = 2;

        /*
            Hackish solution: if returned configuration provider is null, that means central configuration equals
            with currently loaded and cached configuration and no new configuration was loaded.
            But during that process last update check date was changed. Since configuration provider is immutable,
            building new provider from currently loaded one, with last update check date updated.
         */
        long lastConfigurationUpdateEpoch = intent.getLongExtra(ConfigurationConstants.LAST_CONFIGURATION_UPDATE, 0);
        Date confUpdateDate = configurationProvider.getConfigurationUpdateDate();
        if (lastConfigurationUpdateEpoch == 0 || (confUpdateDate != null && confUpdateDate.after(new Date(lastConfigurationUpdateEpoch)))) {
            resultCode = INIT_LIBDIGIDOC_RESULT_CODE;
        }

        bundle.putParcelable(ConfigurationConstants.CONFIGURATION_PROVIDER, configurationProvider);
        confResultReceiver.send(resultCode, bundle);
    }

    private ConfigurationProvider getConfiguration(Intent intent) {
        if (intent.getBooleanExtra(ConfigurationConstants.FORCE_LOAD_CENTRAL_CONFIGURATION, false)) {
            return configurationManager.forceLoadCentralConfiguration();
        } else {
            return configurationManager.getConfiguration();
        }
    }
}

