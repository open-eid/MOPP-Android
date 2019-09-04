
package ee.ria.DigiDoc.configuration;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;

import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;
import ee.ria.DigiDoc.configuration.loader.CachedConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.CentralConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.ConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.DefaultConfigurationLoader;
import ee.ria.DigiDoc.configuration.verify.ConfigurationSignatureVerifier;
import timber.log.Timber;

/**
 * Configuration manager that loads and initialises configuration.
 *
 * For initial application startup cached (if exists and is older than default configuration) or default
 * configuration is loaded in blocking manner. And same time asynchronous loading from central configuration
 * service is initiated. If loading from central configuration service fails then configuration is loaded
 * from cache (if exists). If loading from cache fails or cache does not yet exist then default configuration
 * is loaded.
 *
 * If downloaded central service configuration signature matches with currently cached configuration signature
 * then cached configuration is loaded instead.
 *
 * Default configuration is packaged to the APK assets folder and is updated by gradle task
 * FetchAndPackageDefaultConfigurationTask during APK building process. Along with default configuration
 * files configuration.properties file is packaged to APK which contains:
 *      * central-configuration-service.url: Central configuration service url from where the configuration
 *      is download throughout the application. Defaults to "https://id.eesti.ee".
 *      * configuration.update-interval: Interval in days for how often configuration is updated against central
 *      configuration service. Defaults to 7.
 * Default values are in resources/default-configuration.properties file.
 * These values can be overridden during building APK, for example:
 *      gradle clean fetchAndPackageDefaultConfiguration --args="https://id.eesti.ee 7" app:assemble
 *
 * After each load, configuration's signature is verified against the default configuration signature public key
 * that is packaged with the APK.
 *
 * When configuration is loaded then it is cached to the devices drive. Configuration consists of configuration
 * json, it's signature and public key. Along with named configuration files a configuration-info.properties
 * file is also cached. This properties file contains:
 *      * configuration.last-update-check-date: Date of last central configuration loading. Is updated each
 *      time central configuration is downloaded.
 *      * configuration.update-date: Date of last configuration update. Updated each time downloaded central
 *      configuration is actually loaded for usage (download configuration differs from cached configuration).
 *
 * During each application start-up cached configuration update date (configuration.update-date) is compared
 * to current date and if the difference in days exceeds <configuration.update-interval> then configuration
 * is downloaded from central configuration service. If downloaded central configuration differs from cached
 * configuration then central configuration is loaded for use and cached configuration is updated, else cached
 * version is used.
 *
 * If central configuration download fails for whatever reason then cached configuration is loaded.
 * If cached configuration loading fails for whatever reason then default configuration is loaded.
 * If default configuration loading fails then application startup fails.
 */
public class ConfigurationManager {

    private final String centralConfigurationServiceUrl;
    private final CentralConfigurationLoader centralConfigurationLoader;
    private final DefaultConfigurationLoader defaultConfigurationLoader;
    private final CachedConfigurationLoader cachedConfigurationLoader;
    private final CachedConfigurationHandler cachedConfigurationHandler;
    private final ConfigurationProperties configurationProperties;

    private ConfigurationSignatureVerifier confSignatureVerifier;

    public ConfigurationManager(Context context, ConfigurationProperties configurationProperties, CachedConfigurationHandler cachedConfigurationHandler) {
        this.cachedConfigurationHandler = cachedConfigurationHandler;
        this.centralConfigurationServiceUrl = configurationProperties.getCentralConfigurationServiceUrl();
        this.configurationProperties = configurationProperties;
        this.centralConfigurationLoader = new CentralConfigurationLoader(centralConfigurationServiceUrl, loadCentralConfServiceSSlCertIfPresent(context.getAssets()));
        this.defaultConfigurationLoader = new DefaultConfigurationLoader(context.getAssets());
        this.cachedConfigurationLoader = new CachedConfigurationLoader(cachedConfigurationHandler);
    }

    public ConfigurationProvider getConfiguration() {
        if (shouldUpdateConfiguration()) {
            return loadCentralConfiguration();
        } else {
            return loadCachedConfiguration();
        }
    }

    public ConfigurationProvider forceLoadCachedConfiguration() {
        return loadCachedConfiguration();
    }

    public ConfigurationProvider forceLoadDefaultConfiguration() {
        return loadDefaultConfiguration();
    }

    ConfigurationProvider forceLoadCentralConfiguration() {
        return loadCentralConfiguration();
    }

    private boolean shouldUpdateConfiguration() {
        return !cachedConfigurationHandler.doesCachedConfigurationExist() || isConfigurationOutdated();
    }

    private boolean isConfigurationOutdated() {
        Date currentDate = new Date();
        Date confLastUpdateDate = cachedConfigurationHandler.getConfLastUpdateCheckDate();
        if (confLastUpdateDate == null) {
            return true;
        }
        long diffTime = currentDate.getTime() - confLastUpdateDate.getTime();
        long differenceInDays = diffTime / (1000 * 60 * 60 * 24);
        return differenceInDays > configurationProperties.getConfigurationUpdateInterval();
    }

    private ConfigurationProvider loadCentralConfiguration() {
        try {
            Timber.i("Attempting to load configuration from central configuration service <%s>", centralConfigurationServiceUrl);
            byte[] centralConfigurationSignature = centralConfigurationLoader.loadConfigurationSignature();
            Date currentDate = new Date();
            cachedConfigurationHandler.updateConfigurationLastCheckDate(currentDate);
            if (cachedConfigurationHandler.doesCachedConfigurationExist() && isCachedConfUpToDate(centralConfigurationSignature)) {
                Timber.i("Cached configuration signature matches with central configuration signature. Not updating and using cached configuration");
                cachedConfigurationHandler.updateProperties();
                return loadCachedConfiguration();
            }

            centralConfigurationLoader.loadConfigurationJson();
            verifyConfigurationSignature(centralConfigurationLoader);

            cachedConfigurationHandler.updateConfigurationUpdatedDate(currentDate);
            Timber.i("Configuration successfully loaded from central configuration service");
            return parseAndCacheConfiguration(centralConfigurationLoader);
        } catch (Exception e) {
            Timber.e(e, "Failed to load configuration from central configuration service");
            return loadCachedConfiguration();
        }
    }

    private ConfigurationProvider loadCachedConfiguration() {
        try {
            Timber.i("Attempting to load cached configuration");
            cachedConfigurationLoader.load();
            verifyConfigurationSignature(cachedConfigurationLoader);
            Timber.i("Cached configuration successfully loaded");
            return parseConfigurationProvider(cachedConfigurationLoader.getConfigurationJson());
        } catch (Exception e) {
            Timber.e(e, "Failed to load cached configuration");
            return loadDefaultConfiguration();
        }
    }

    private ConfigurationProvider loadDefaultConfiguration() {
        try {
            Timber.i("Attempting to load default configuration");
            defaultConfigurationLoader.load();
            verifyConfigurationSignature(defaultConfigurationLoader);
            Timber.i("Default configuration successfully loaded");
            overrideConfUpdateDateWithDefaultConfigurationInitDownloadDate();
            return parseAndCacheConfiguration(defaultConfigurationLoader);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load default configuration", e);
        }
    }

    private void verifyConfigurationSignature(ConfigurationLoader configurationLoader) {
        if (confSignatureVerifier == null) {
            String publicKey = defaultConfigurationLoader.getConfigurationSignaturePublicKey();
            if (publicKey == null) {
                publicKey = defaultConfigurationLoader.loadConfigurationSignaturePublicKey();
            }
            confSignatureVerifier = new ConfigurationSignatureVerifier(publicKey);
        }
        confSignatureVerifier.verifyConfigurationSignature(
                configurationLoader.getConfigurationJson(), configurationLoader.getConfigurationSignature());
    }

    private boolean isCachedConfUpToDate(byte[] centralConfigurationSignature) {
        byte[] cachedConfSignature = cachedConfigurationHandler.readFileContentBytes(CachedConfigurationHandler.CACHED_CONFIG_RSA);
        return Arrays.equals(cachedConfSignature, centralConfigurationSignature);
    }

    private void overrideConfUpdateDateWithDefaultConfigurationInitDownloadDate() {
        Date defaultConfInitDownloadDate = configurationProperties.getPackagedConfigurationInitialDownloadDate();
        Date cachedConfUpdateDate = cachedConfigurationHandler.getConfUpdateDate();
        if (cachedConfUpdateDate == null || defaultConfInitDownloadDate.after(cachedConfUpdateDate)) {
            cachedConfigurationHandler.updateConfigurationUpdatedDate(defaultConfInitDownloadDate);
        }
    }

    private ConfigurationProvider parseAndCacheConfiguration(ConfigurationLoader configurationLoader) {
        ConfigurationProvider configurationProvider = parseConfigurationProvider(configurationLoader.getConfigurationJson());
        cacheConfiguration(configurationLoader, configurationProvider);
        Timber.i("Configuration successfully cached");

        cachedConfigurationHandler.updateProperties();
        return configurationProvider;
    }

    private ConfigurationProvider parseConfigurationProvider(String configurationJson) {
        ConfigurationParser configurationParser = new ConfigurationParser(configurationJson);
        return parseAndConstructConfigurationProvider(configurationParser);
    }

    private void cacheConfiguration(ConfigurationLoader configurationLoader, ConfigurationProvider configurationProvider) {
        String configurationJson = configurationLoader.getConfigurationJson();
        cachedConfigurationHandler.cacheFile(CachedConfigurationHandler.CACHED_CONFIG_JSON, configurationJson);
        cachedConfigurationHandler.cacheFile(CachedConfigurationHandler.CACHED_CONFIG_RSA, configurationLoader.getConfigurationSignature());
        cachedConfigurationHandler.updateConfigurationVersionSerial(configurationProvider.getMetaInf().getSerial());
    }

    private ConfigurationProvider parseAndConstructConfigurationProvider(final ConfigurationParser configurationParser) {
        ConfigurationProvider.MetaInf metaInf = ConfigurationProvider.MetaInf.builder()
                .setUrl(configurationParser.parseStringValue("META-INF", "URL"))
                .setDate(configurationParser.parseStringValue("META-INF", "DATE"))
                .setSerial(configurationParser.parseIntValue("META-INF", "SERIAL"))
                .setVersion(configurationParser.parseIntValue("META-INF", "VER"))
                .build();

        return ConfigurationProvider.builder()
                .setMetaInf(metaInf)
                .setConfigUrl(centralConfigurationServiceUrl)
                .setSivaUrl(configurationParser.parseStringValue("SIVA-URL"))
                .setTslUrl(configurationParser.parseStringValue("TSL-URL"))
                .setTslCerts(configurationParser.parseStringValues("TSL-CERTS"))
                .setTsaUrl(configurationParser.parseStringValue("TSA-URL"))
                .setMidSignUrl(configurationParser.parseStringValue("MID-SIGN-URL"))
                .setLdapPersonUrl(configurationParser.parseStringValue("LDAP-PERSON-URL"))
                .setLdapCorpUrl(configurationParser.parseStringValue("LDAP-CORP-URL"))
                .setOCSPUrls(configurationParser.parseStringValuesToMap("OCSP-URL-ISSUER"))
                .setConfigurationLastUpdateCheckDate(cachedConfigurationHandler.getConfLastUpdateCheckDate())
                .setConfigurationUpdateDate(cachedConfigurationHandler.getConfUpdateDate())
                .build();
    }

    private X509Certificate loadCentralConfServiceSSlCertIfPresent(AssetManager assetManager) {
        try {
            InputStream certStream = assetManager.open("certs/test-ca.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate)cf.generateCertificate(certStream);
        } catch (FileNotFoundException e) {
            // No explicit SSL certificate found in assets, using java default cacerts
            return null;
        } catch (IOException | CertificateException e) {
            throw new IllegalStateException("Failed to load SSL certificate", e);
        }
    }
}