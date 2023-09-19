package ee.ria.DigiDoc.configuration.loader;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import ee.ria.DigiDoc.configuration.ConfigurationDateUtil;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import timber.log.Timber;

public class CachedConfigurationHandler {

    public static final String CACHED_CONFIG_JSON = "active-config.json";
    public static final String CACHED_CONFIG_RSA = "active-config.rsa";
    public static final String CACHED_CONFIG_PUB = "active-config.pub";

    private static final String CACHE_CONFIG_FOLDER = "/config/";
    private static final String CONFIGURATION_INFO_FILE_NAME = "configuration-info.properties";
    private static final String CONFIGURATION_LAST_UPDATE_CHECK_DATE_PROPERTY_NAME = "configuration.last-update-check-date";
    private static final String CONFIGURATION_UPDATE_DATE_PROPERTY_NAME = "configuration.update-date";
    private static final String CONFIGURATION_VERSION_SERIAL_PROPERTY_NAME = "configuration.version-serial";

    private final File cacheDir;
    private final SimpleDateFormat dateFormat;
    private Properties properties;

    public CachedConfigurationHandler(File cacheDir) {
        this.cacheDir = cacheDir;
        loadProperties();
        if (properties == null) {
            // Cached properties file missing, generating a empty one
            cacheFile(CONFIGURATION_INFO_FILE_NAME, "");
            loadProperties();
            if (properties == null) {
                throw new IllegalStateException("Failed to load properties file " + CONFIGURATION_INFO_FILE_NAME);
            }
        }
        this.dateFormat = ConfigurationDateUtil.getDateFormat();
    }

    public Integer getConfigurationVersionSerial() {
        try {
            String versionSerial = loadProperty(CONFIGURATION_VERSION_SERIAL_PROPERTY_NAME);
            if (versionSerial == null) {
                return null;
            }
            return Integer.parseInt(versionSerial);
        } catch (NumberFormatException nfe) {
            Timber.log(Log.ERROR, nfe, "Unable to get configuration version serial");
            return null;
        }
    }

    public Date getConfLastUpdateCheckDate() {
        return loadPropertyDate(CONFIGURATION_LAST_UPDATE_CHECK_DATE_PROPERTY_NAME);
    }

    public Date getConfUpdateDate() {
        return loadPropertyDate(CONFIGURATION_UPDATE_DATE_PROPERTY_NAME);
    }

    private Date loadPropertyDate(String propertyName) {
        try {
            String property = loadProperty(propertyName);
            if (property == null) {
                return null;
            }
            return dateFormat.parse(property);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse configuration update date", e);
        }
    }

    private String loadProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public void cacheFile(String fileName, String content) {
        FileUtils.storeFile(cacheDir(fileName), content);
    }

    public void cacheFile(String fileName, byte[] content) {
        FileUtils.storeFile(cacheDir(fileName), content);
    }

    public void updateConfigurationUpdatedDate(Date date) {
        properties.setProperty(CONFIGURATION_UPDATE_DATE_PROPERTY_NAME, dateFormat.format(date));
    }

    public void updateConfigurationLastCheckDate(Date date) {
        properties.setProperty(CONFIGURATION_LAST_UPDATE_CHECK_DATE_PROPERTY_NAME, dateFormat.format(date));
    }

    public void updateConfigurationVersionSerial(int versionSerial) {
        properties.setProperty(CONFIGURATION_VERSION_SERIAL_PROPERTY_NAME, String.valueOf(versionSerial));
    }

    String readFileContent(String filename) {
        return FileUtils.readFileContent(cacheDir(filename));
    }

    public byte[] readFileContentBytes(String filename) {
        return FileUtils.readFileContentBytes(cacheDir(filename));
    }

    public boolean doesCachedConfigurationExist() {
        return doesCachedConfigurationFileExists(CACHED_CONFIG_JSON);
    }

    public boolean doesCachedConfigurationFileExists(String fileName) {
        File file = new File(cacheDir(fileName));
        return file.exists();
    }

    private void loadProperties() {
        try (FileInputStream fileInputStream = new FileInputStream(cacheDir(CONFIGURATION_INFO_FILE_NAME))) {
            properties = new Properties();
            properties.load(fileInputStream);
        } catch (IOException e) {
            Timber.log(Log.DEBUG, e, "Cached properties file '" + CONFIGURATION_INFO_FILE_NAME + "' not found");
            properties = null;
        }
    }

    public void updateProperties() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(cacheDir(CONFIGURATION_INFO_FILE_NAME))) {
            properties.store(fileOutputStream, null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update properties file '" + CONFIGURATION_INFO_FILE_NAME + "'", e);
        }
    }

    private String cacheDir(String filename) {
        return cacheDir + CACHE_CONFIG_FOLDER + filename;
    }
}
