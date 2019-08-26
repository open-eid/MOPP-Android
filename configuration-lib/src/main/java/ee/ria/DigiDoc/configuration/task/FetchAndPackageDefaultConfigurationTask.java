package ee.ria.DigiDoc.configuration.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import ee.ria.DigiDoc.configuration.ConfigurationProperties;
import ee.ria.DigiDoc.configuration.loader.CentralConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.DefaultConfigurationLoader;

/**
 * Task for loading configuration from central configuration service and storing it to assets folder.
 * These assets are packaged to APK and used as default configuration.
 * Because this task depends on gradle build, these assets are manually copied to built content after
 * task completion.
 *
 * Also creates configuration.properties file to assets folder.
 * configuration.properties hard-coded values will read from resources/default-configuration.properties
 *
 * Two separate configuration file sets will be created. One for 'envtest' build variant and one for all
 * other build variants..
 */
public class FetchAndPackageDefaultConfigurationTask {

    private static final String PROD_CENTRAL_CONF_SERVICE_ULR_NAME = "prod.central-configuration-service.url";
    private static final String TEST_CENTRAL_CONF_SERVICE_ULR_NAME = "test.central-configuration-service.url";
    private static final String DEFAULT_CONFIGURATION_PROPERTIES_FILE_NAME = "default-configuration.properties";
    private static Properties properties = new Properties();
    private static String buildVariant;

    public static void main(String[] args) {
        loadResourcesProperties();
        loadAndStoreDefaultConfiguration(PROD_CENTRAL_CONF_SERVICE_ULR_NAME, "main");
        loadAndStoreDefaultConfiguration(TEST_CENTRAL_CONF_SERVICE_ULR_NAME, "envtest");
    }

    private static void loadAndStoreDefaultConfiguration(String serviceUrlPropertyName, String buildVariant) {
        FetchAndPackageDefaultConfigurationTask.buildVariant = buildVariant;
        String configurationServiceUrl = properties.getProperty(serviceUrlPropertyName);
        CentralConfigurationLoader confLoader = new CentralConfigurationLoader(configurationServiceUrl);
        confLoader.load();
        assertConfigurationLoaded(confLoader);
        storeAsDefaultConfiguration(confLoader);
        if (buildVariant.equals("envtest")) {
            storeApplicationProperties(configurationServiceUrl + "/pop", determineConfigurationUpdateInterval());
        } else {
            storeApplicationProperties(configurationServiceUrl, determineConfigurationUpdateInterval());
        }
    }

    private static void loadResourcesProperties() {
        try {
            properties = new Properties();
            properties.load(new FileInputStream(System.getProperty("user.dir") + "/src/main/resources/" + DEFAULT_CONFIGURATION_PROPERTIES_FILE_NAME));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read '" + DEFAULT_CONFIGURATION_PROPERTIES_FILE_NAME + "' file", e);
        }
    }

    private static int determineConfigurationUpdateInterval() {
        return Integer.parseInt(properties.getProperty(ConfigurationProperties.CONFIGURATION_UPDATE_INTERVAL_PROPERTY));
    }

    private static void assertConfigurationLoaded(CentralConfigurationLoader confLoader) {
        if (confLoader.getConfigurationJson() == null || confLoader.getConfigurationSignature() == null || confLoader.getConfigurationSignaturePublicKey() == null) {
            throw new IllegalStateException("Configuration loading has failed");
        }
    }

    private static void storeAsDefaultConfiguration(CentralConfigurationLoader confLoader) {
        storeFile(DefaultConfigurationLoader.DEFAULT_CONFIG_JSON, confLoader.getConfigurationJson());
        storeFile(DefaultConfigurationLoader.DEFAULT_CONFIG_RSA, confLoader.getConfigurationSignature());
        storeFile(DefaultConfigurationLoader.DEFAULT_CONFIG_PUB, confLoader.getConfigurationSignaturePublicKey());
    }

    private static void storeApplicationProperties(String configurationServiceUrl, int configurationUpdateInterval) {
        StringBuilder propertiesFileBuilder = new StringBuilder()
                .append(ConfigurationProperties.CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY)
                .append("=")
                .append(configurationServiceUrl)
                .append("\n")
                .append(ConfigurationProperties.CONFIGURATION_UPDATE_INTERVAL_PROPERTY)
                .append("=")
                .append(configurationUpdateInterval);
        storeFile(ConfigurationProperties.PROPERTIES_FILE_NAME, propertiesFileBuilder.toString());
    }

    private static void storeFile(String filename, String fileContent) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFileDir(filename)))) {
            writer.write(fileContent);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file '" + filename + "' as default configuration", e);
        }
    }

    private static File configFileDir(String filename) {
        File file = new File(System.getProperty("user.dir") + "/src/" + buildVariant + "/assets/config/" + filename);
        file.getParentFile().mkdirs();
        return file;
    }
}
