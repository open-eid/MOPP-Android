package ee.ria.DigiDoc.configuration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.Date;

import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;
import ee.ria.DigiDoc.configuration.util.UserAgentUtil;

public class ConfigurationManagerService extends JobIntentService {

    public static final int NEW_CONFIGURATION_LOADED = 1;
    public static final int CONFIGURATION_UP_TO_DATE = 2;

    private ConfigurationManager configurationManager;

    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, ConfigurationManagerService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CachedConfigurationHandler cachedConfigurationHandler = new CachedConfigurationHandler(getCacheDir());
        ConfigurationProperties configurationProperties = new ConfigurationProperties(getAssets());
        configurationManager = new ConfigurationManager(this, configurationProperties, cachedConfigurationHandler, UserAgentUtil.getUserAgent(getApplicationContext()));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        ResultReceiver confResultReceiver = intent.getParcelableExtra(ConfigurationConstants.CONFIGURATION_RESULT_RECEIVER);
        Bundle bundle = new Bundle();

        ConfigurationProvider configurationProvider = getConfiguration(intent);

        long lastConfigurationUpdateEpoch = intent.getLongExtra(ConfigurationConstants.LAST_CONFIGURATION_UPDATE, 0);
        Date confUpdateDate = configurationProvider.getConfigurationUpdateDate();

        int resultCode = CONFIGURATION_UP_TO_DATE;
        if (lastConfigurationUpdateEpoch == 0 || (confUpdateDate != null && confUpdateDate.after(new Date(lastConfigurationUpdateEpoch)))) {
            resultCode = NEW_CONFIGURATION_LOADED;
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

