package ee.ria.DigiDoc.configuration.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
 * that can be overridden by arguments to this task.
 *
 * One can pass 2 arguments to this task: central configuration service url and configuration update interval
 * (example: gradle fetchAndPackageDefaultConfiguration --args="https://id.eesti.ee 7")
 *
 * Two separate configuration file sets will be created. One for 'envtest' build variant and one for all
 * other build variants. 'envtest' build variant is always hard-coded and ignores passed argument value.
 */
public class FetchAndPackageDefaultConfigurationTask {

    private static final String CENTRAL_CONF_SERVICE_ULR_NAME = "central-configuration-service.url";
    private static final String TEST_CENTRAL_CONF_SERVICE_ULR_NAME = "test.central-configuration-service.url";
    private static final String DEFAULT_CONFIGURATION_PROPERTIES_FILE_NAME = "default-configuration.properties";
    private static Properties properties = new Properties();
    private static String buildVariant;

    public static void main(String[] args) {
        loadResourcesProperties();
        loadAndStoreDefaultConfiguration(args);
        loadAndStoreEnvTestDefaultConfiguration(args);
    }

    private static void loadAndStoreDefaultConfiguration(String[] args) {
        FetchAndPackageDefaultConfigurationTask.buildVariant = "main";
        String configurationServiceUrl = determineCentralConfigurationServiceUrl(args);
        CentralConfigurationLoader confLoader = new CentralConfigurationLoader(configurationServiceUrl, null);
        confLoader.load();
        assertConfigurationLoaded(confLoader);
        storeAsDefaultConfiguration(confLoader);
        storeApplicationProperties(configurationServiceUrl, determineConfigurationUpdateInterval(args));
    }

    private static void loadAndStoreEnvTestDefaultConfiguration(String[] args) {
        FetchAndPackageDefaultConfigurationTask.buildVariant = "envtest";
        String configurationServiceUrl = properties.getProperty(TEST_CENTRAL_CONF_SERVICE_ULR_NAME);
        CentralConfigurationLoader confLoader = new CentralConfigurationLoader(configurationServiceUrl, loadCentralConfServiceSSLCert());
        confLoader.load();
        assertConfigurationLoaded(confLoader);
        storeAsDefaultConfiguration(confLoader);
        storeApplicationProperties(configurationServiceUrl, determineConfigurationUpdateInterval(args));
    }

    private static String determineCentralConfigurationServiceUrl(String[] args) {
        if (args.length > 0) {
            return args[0];
        } else {
            return properties.getProperty(CENTRAL_CONF_SERVICE_ULR_NAME);
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

    private static int determineConfigurationUpdateInterval(String[] args) {
        if (args.length > 1) {
            return Integer.parseInt(args[1]);
        } else {
            return Integer.parseInt(properties.getProperty(ConfigurationProperties.CONFIGURATION_UPDATE_INTERVAL_PROPERTY));
        }
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

    private static X509Certificate loadCentralConfServiceSSLCert() {
        try {
            FileInputStream is = new FileInputStream(System.getProperty("user.dir") + "/src/envtest/assets/certs/test-ca.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate)cf.generateCertificate(is);
        } catch (CertificateException | FileNotFoundException e) {
            throw new IllegalStateException("Failed to load SSL certificate", e);
        }
    }
}
