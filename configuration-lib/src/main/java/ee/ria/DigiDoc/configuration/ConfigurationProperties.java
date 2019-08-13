package ee.ria.DigiDoc.configuration;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationProperties {

    public static final String CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY = "central-configuration-service.url";
    public static final String CONFIGURATION_UPDATE_INTERVAL_PROPERTY = "configuration.update-interval";
    public static final String PROPERTIES_FILE_NAME = "configuration.properties";
    private Properties properties;

    public ConfigurationProperties(AssetManager assetManager) {
        try (InputStream propertiesStream = assetManager.open("config/" + PROPERTIES_FILE_NAME)) {
            loadProperties(propertiesStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open " + PROPERTIES_FILE_NAME + " file from assets", e);
        }
    }

    String getCentralConfigurationServiceUrl() {
        return properties.getProperty(CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY);
    }

    int getConfigurationUpdateInterval() {
        return Integer.parseInt(properties.getProperty(CONFIGURATION_UPDATE_INTERVAL_PROPERTY));
    }

    Properties getProperties() {
        return properties;
    }

    private void loadProperties(InputStream inputStream) {
        try {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties " + PROPERTIES_FILE_NAME + " file from assets", e);
        }
    }
}
