package ee.ria.DigiDoc.configuration;

import android.content.Context;

import java.util.Date;

import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;
import ee.ria.DigiDoc.configuration.loader.CachedConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.CentralConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.ConfigurationLoader;
import ee.ria.DigiDoc.configuration.loader.DefaultConfigurationLoader;
import timber.log.Timber;

/**
 * Configuration manager that loads and initialises configuration.
 *
 * For initial application startup configuration is loaded from central configuration service.
 * If loading from central configuration service fails then configuration is loaded from cache (if exists).
 * If loading from cache fails or cache does not yet exist than default configuration is loaded.
 * Default configuration is packaged to the APK assets folder and is updated by gradle task
 * UpdatePackagedDefaultConfigurationTask during APK building process.
 *
 * Loaded configuration is then cached to the devices drive. Configuration consists of configuration json,
 * it's signature and public key. Along with named configuration files a configuration-info.properties file
 * containing is also cached. This properties file contains parameter <configuration.last-update-check-date>
 * which is updated each time central configuration is downloaded and parameter <configuration.update-date>
 * which is updated each time downloaded central configuration is actually loaded for usage.
 *
 * During each application start-up cached configuration update date (configuration.update-date) is compared
 * to current date and if the difference in days exceeds <configuration.update-interval (packaged to the APK
 * assets through configuration.properties, configurable through task UpdatePackagedDefaultConfigurationTask,
 * defaults to 7)>, then configuration is downloaded from central configuration service.
 * If downloaded central configuration differs from cached configuration then central configuration is loaded
 * for use and cached configuration is updated, else cached version is used.
 */
public class ConfigurationManager {

    private final String centralConfigurationServiceUrl;
    private final int configurationUpdateInterval;
    private final ConfigurationLoader centralConfigurationLoader;
    private final ConfigurationLoader defaultConfigurationLoader;
    private final ConfigurationLoader cachedConfigurationLoader;
    private final CachedConfigurationHandler cachedConfigurationHandler;

    public ConfigurationManager(Context context, ConfigurationProperties configurationProperties, CachedConfigurationHandler cachedConfigurationHandler) {
        this.cachedConfigurationHandler = cachedConfigurationHandler;
        this.centralConfigurationServiceUrl = configurationProperties.getCentralConfigurationServiceUrl();
        this.configurationUpdateInterval = configurationProperties.getConfigurationUpdateInterval();
        this.centralConfigurationLoader = new CentralConfigurationLoader(centralConfigurationServiceUrl);
        this.defaultConfigurationLoader = new DefaultConfigurationLoader(context.getAssets());
        this.cachedConfigurationLoader = new CachedConfigurationLoader(cachedConfigurationHandler);
    }

    public ConfigurationProvider getConfiguration() {
        String configurationJson = loadConfiguration();
        ConfigurationParser configurationParser = new ConfigurationParser(configurationJson);
        return parseAndConstructConfigurationProvider(configurationParser);
    }

    public ConfigurationProvider forceLoadCentralConfiguration() {
        String configurationJson = loadCentralConfiguration();
        ConfigurationParser configurationParser = new ConfigurationParser(configurationJson);
        return parseAndConstructConfigurationProvider(configurationParser);
    }

    private String loadConfiguration() {
        if (shouldUpdateConfiguration()) {
            return loadCentralConfiguration();
        } else {
            return loadCachedConfiguration();
        }
    }

    private boolean shouldUpdateConfiguration() {
        return !cachedConfigurationHandler.doesCachedConfigurationInfoExist() || isConfigurationOutdated();
    }

    private boolean isConfigurationOutdated() {
        Date currentDate = new Date();
        Date confLastUpdateDate = cachedConfigurationHandler.getConfLastUpdateCheckDate();
        long diffTime = currentDate.getTime() - confLastUpdateDate.getTime();
        long differenceInDays = diffTime / (1000 * 60 * 60 * 24);
        return differenceInDays > configurationUpdateInterval;
    }

    private String loadCentralConfiguration() {
        try {
            Timber.i("Attempting to load configuration from central configuration service <%s>", centralConfigurationServiceUrl);
            centralConfigurationLoader.load();
            if (cachedConfigurationHandler.doesCachedConfigurationInfoExist() && isCachedConfLatest()) {
                Timber.i("Cached configuration signature matches with central configuration signature. Not updating and using cached configuration");
                cachedConfigurationHandler.updateConfigurationLastCheckDate(new Date());
                return loadCachedConfiguration();
            }
            cacheConfiguration(centralConfigurationLoader);
            Timber.i("Configuration successfully loaded from central configuration service");
            return centralConfigurationLoader.getConfigurationJson();
        } catch (Exception e) {
            Timber.e(e, "Failed to load configuration from central configuration service");
            return loadCachedConfiguration();
        }
    }

    private String loadCachedConfiguration() {
        try {
            Timber.i("Attempting to load cached configuration");
            cachedConfigurationLoader.load();
            Timber.i("Cached configuration successfully loaded");
            return cachedConfigurationLoader.getConfigurationJson();
        } catch (Exception e) {
            Timber.e(e, "Failed to load cached configuration");
            return loadDefaultConfiguration();
        }
    }

    private String loadDefaultConfiguration() {
        try {
            Timber.i("Attempting to load default configuration");
            defaultConfigurationLoader.load();
            Timber.i("Default configuration successfully loaded");
            cacheConfiguration(defaultConfigurationLoader);
            return defaultConfigurationLoader.getConfigurationJson();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load default configuration", e);
        }
    }

    private boolean isCachedConfLatest() {
        String cachedConfSignature = cachedConfigurationHandler.readFileContent(CachedConfigurationHandler.CACHED_CONFIG_RSA);
        return cachedConfSignature.equals(centralConfigurationLoader.getConfigurationSignature());
    }

    private void cacheConfiguration(ConfigurationLoader configurationLoader) {
        cachedConfigurationHandler.cacheFile(CachedConfigurationHandler.CACHED_CONFIG_JSON, configurationLoader.getConfigurationJson());
        cachedConfigurationHandler.cacheFile(CachedConfigurationHandler.CACHED_CONFIG_RSA, configurationLoader.getConfigurationSignature());
        cachedConfigurationHandler.cacheFile(CachedConfigurationHandler.CACHED_CONFIG_PUB, configurationLoader.getConfigurationSignaturePublicKey());
        if (configurationLoader instanceof CentralConfigurationLoader) {
            cachedConfigurationHandler.updateConfigurationUpdatedDate(new Date());
        }
    }

    private ConfigurationProvider parseAndConstructConfigurationProvider(final ConfigurationParser configurationParser) {
        ConfigurationProvider.MetaInf metaInf = ConfigurationProvider.MetaInf.builder()
                .setUrl(configurationParser.parseStringValue("META-INF", "URL"))
                .setDate(configurationParser.parseStringValue("META-INF", "DATE"))
                .setSerial(configurationParser.parseIntValue("META-INF", "SERIAL"))
                .setVersion(configurationParser.parseIntValue("META-INF", "VER"))
                .build();

        String midSignUrl = isTestMode() ? configurationParser.parseStringValue("MID-SIGN-TEST-URL") : configurationParser.parseStringValue("MID-SIGN-URL");
        String ldapPersonUrl = configurationParser.parseStringValue("LDAP-PERSON-URL").split("://")[1];

        configurationParser.parseStringValues("TSL-CERTS");

        return ConfigurationProvider.builder()
                .setMetaInf(metaInf)
                .setConfigUrl(centralConfigurationServiceUrl)
                .setSivaUrl(configurationParser.parseStringValue("SIVA-URL"))
                .setTslUrl(configurationParser.parseStringValue("TSL-URL"))
                .setTslCerts(configurationParser.parseStringValues("TSL-CERTS"))
                .setTsaUrl(configurationParser.parseStringValue("TSA-URL"))
                .setMidSignUrl(midSignUrl)
                .setLdapPersonUrl(ldapPersonUrl)
                .setLdapCorpUrl(configurationParser.parseStringValue("LDAP-CORP-URL"))
                .setOCSPUrls(configurationParser.parseStringValuesToMap("OCSP-URL-ISSUER"))
                .setConfigurationLastUpdateCheckDate(cachedConfigurationHandler.getConfLastUpdateCheckDate())
                .setConfigurationUpdateDate(cachedConfigurationHandler.getConfUpdateDate())
                .build();
    }

    private boolean isTestMode() {
        return centralConfigurationServiceUrl.contains("test");
    }
}