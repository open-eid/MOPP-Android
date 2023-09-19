package ee.ria.DigiDoc.configuration;

import android.content.res.AssetManager;
import android.util.Log;

import org.bouncycastle.cert.dane.DANEEntryFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import timber.log.Timber;

public class ConfigurationProperties {

    public static final String CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY = "central-configuration-service.url";
    public static final String CONFIGURATION_UPDATE_INTERVAL_PROPERTY = "configuration.update-interval";
    public static final String CONFIGURATION_VERSION_SERIAL_PROPERTY = "configuration.version-serial";
    public static final String CONFIGURATION_DOWNLOAD_DATE_PROPERTY = "configuration.download-date";
    public static final String PROPERTIES_FILE_NAME = "configuration.properties";
    private static final int DEFAULT_UPDATE_INTERVAL = 4;
    private final SimpleDateFormat dateFormat;
    private Properties properties;

    public ConfigurationProperties(AssetManager assetManager) {
        try (InputStream propertiesStream = assetManager.open("config/" + PROPERTIES_FILE_NAME)) {
            loadProperties(propertiesStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open " + PROPERTIES_FILE_NAME + " file from assets", e);
        }
        this.dateFormat = ConfigurationDateUtil.getDateFormat();
    }

    public int getConfigurationVersionSerial() {
        return Integer.parseInt(properties.getProperty(CONFIGURATION_VERSION_SERIAL_PROPERTY));
    }

    String getCentralConfigurationServiceUrl() {
        return properties.getProperty(CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY);
    }

    int getConfigurationUpdateInterval() {
        try {
            return Integer.parseInt(properties.getProperty(CONFIGURATION_UPDATE_INTERVAL_PROPERTY));
        } catch (NumberFormatException nfe) {
            Timber.log(Log.ERROR, nfe, "Unable to get configuration update interval");
            return DEFAULT_UPDATE_INTERVAL;
        }
    }

    Date getPackagedConfigurationInitialDownloadDate() {
        String property = properties.getProperty(CONFIGURATION_DOWNLOAD_DATE_PROPERTY);
        try {
            return dateFormat.parse(property);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse configuration initial download date", e);
        }
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
