package ee.ria.DigiDoc.configuration;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ConfigurationManagerTest {

    @Rule public final TemporaryFolder folder =
            new TemporaryFolder(InstrumentationRegistry.getContext().getCacheDir());

    private ConfigurationProperties configurationProperties;
    private CachedConfigurationHandler cacheConfHandler;
    private ConfigurationManager configurationManager;

    @Before
    public void setup() {
        deleteCachedData(InstrumentationRegistry.getContext().getCacheDir());

        Context context = InstrumentationRegistry.getTargetContext();
        configurationProperties = new ConfigurationProperties(context.getAssets());
        cacheConfHandler = new CachedConfigurationHandler(context.getCacheDir());
        configurationManager = new ConfigurationManager(context, configurationProperties, cacheConfHandler);
    }

    @Test
    public void getConfigurationFromOnline() {
        long processStartDate = inSeconds(new Date());
        ConfigurationProvider configuration = configurationManager.getConfiguration();
        long processEndDate = inSeconds(new Date());
        assertConfigurationValues(configuration);
        assertConfigurationUpdateDates(configuration, processStartDate, processEndDate);
        assertNotNull(configuration.getConfigurationLastUpdateCheckDate());
        assertNotNull(configuration.getConfigurationUpdateDate());
    }

    @Test
    public void forceLoadCentralConfiguration() {
        long processStartDate = inSeconds(new Date());
        ConfigurationProvider configuration = configurationManager.forceLoadCentralConfiguration();
        long processEndDate = inSeconds(new Date());
        assertConfigurationValues(configuration);
        assertConfigurationUpdateDates(configuration, processStartDate, processEndDate);
        assertNotNull(configuration.getConfigurationLastUpdateCheckDate());
        assertNotNull(configuration.getConfigurationUpdateDate());
    }

    @Test
    public void forceLoadCachedConfiguration() {
        ConfigurationProvider configuration = configurationManager.forceLoadCachedConfiguration();
        assertConfigurationValues(configuration);
        assertNull(configuration.getConfigurationLastUpdateCheckDate());
        assertNull(configuration.getConfigurationUpdateDate());
    }

    @Test
    public void forceLoadDefaultConfiguration() {
        ConfigurationProvider configuration = configurationManager.forceLoadDefaultConfiguration();
        assertConfigurationValues(configuration);
        assertNull(configuration.getConfigurationLastUpdateCheckDate());
        assertNull(configuration.getConfigurationUpdateDate());
    }

    @Test
    public void getConfiguration_firstConfigurationGetsFromOnline_allFollowingAttemptsLoadFromCache() throws InterruptedException {
        long processStartDate = inSeconds(new Date());
        ConfigurationProvider configuration = configurationManager.getConfiguration();
        long processEndDate = inSeconds(new Date());
        assertConfigurationValues(configuration);
        assertConfigurationUpdateDates(configuration, processStartDate, processEndDate);
        Date configurationLastUpdateCheckDate = configuration.getConfigurationLastUpdateCheckDate();
        Date configurationUpdateDate = configuration.getConfigurationUpdateDate();

        Thread.sleep(2000);

        configuration = configurationManager.getConfiguration();
        assertConfigurationValues(configuration);
        assertEquals(configurationLastUpdateCheckDate, configuration.getConfigurationLastUpdateCheckDate());
        assertEquals(configurationUpdateDate, configuration.getConfigurationUpdateDate());

        Thread.sleep(2000);

        configuration = configurationManager.getConfiguration();
        assertConfigurationValues(configuration);
        assertEquals(configurationLastUpdateCheckDate, configuration.getConfigurationLastUpdateCheckDate());
        assertEquals(configurationUpdateDate, configuration.getConfigurationUpdateDate());
    }

    @Test
    public void getConfiguration_firstConfigurationFromOnline_secondConfigurationAlsoFromOnlineBecauseConfLastCheckInThePastOverAllowedInterval_lastCheckUpdated() throws InterruptedException {
        long processStartDate = inSeconds(new Date());
        ConfigurationProvider configuration = configurationManager.getConfiguration();
        long processEndDate = inSeconds(new Date());
        assertConfigurationValues(configuration);
        assertConfigurationUpdateDates(configuration, processStartDate, processEndDate);
        Date configurationLastUpdateCheckDate = configuration.getConfigurationLastUpdateCheckDate();
        Date configurationUpdateDate = configuration.getConfigurationUpdateDate();
        Date confUpdateDate = cacheConfHandler.getConfUpdateDate();
        Date confLastUpdateCheckDate = cacheConfHandler.getConfLastUpdateCheckDate();

        Thread.sleep(2000);
        updateCachedPropertyConfLastCheckValueToPast();

        processStartDate = inSeconds(new Date());
        configuration = configurationManager.getConfiguration();
        processEndDate = inSeconds(new Date());
        assertConfigurationValues(configuration);
        assertConfigurationUpdateDates(configuration, processStartDate, processEndDate);

        // Last check updated because another online configuration load was done
        // Update date not updated because conf is equal to last load
        assertNotEquals(configurationLastUpdateCheckDate, configuration.getConfigurationLastUpdateCheckDate());
        assertEquals(configurationUpdateDate, configuration.getConfigurationUpdateDate());

        assertEquals(confUpdateDate, cacheConfHandler.getConfUpdateDate());
        assertNotEquals(confLastUpdateCheckDate, cacheConfHandler.getConfLastUpdateCheckDate());
        assertTrue(cacheConfHandler.getConfLastUpdateCheckDate().after(confLastUpdateCheckDate));
    }

    @Test
    public void getConfiguration_loadingFromOnlineFails_confNotCached_thenDefaultConfigurationLoaded_whenLoadingAgainAndOnlineLoadDoesNotFail_thenOnlineConfReloaded() {
        String confServiceValidUrl = configurationProperties.getCentralConfigurationServiceUrl();
        Properties properties = configurationProperties.getProperties();
        // Set central configuration url invalid, to emulate online configuration loading failure
        properties.setProperty(ConfigurationProperties.CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY, "invalid_url");

        assertFalse(cacheConfHandler.doesCachedConfigurationInfoExist());
        assertFalse(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_JSON));
        assertFalse(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_PUB));
        assertFalse(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_RSA));

        configurationManager = new ConfigurationManager(InstrumentationRegistry.getTargetContext(), configurationProperties, cacheConfHandler);
        // Loading configuration.
        // Online configuration should fail and default configuration loaded, because cached configuration does not exist yet.
        // Default conf cached.
        ConfigurationProvider configuration = configurationManager.getConfiguration();
        assertDefaultConfigurationValues(configuration);
        assertNull(configuration.getConfigurationLastUpdateCheckDate());
        assertNull(configuration.getConfigurationUpdateDate());

        assertFalse(cacheConfHandler.doesCachedConfigurationInfoExist());
        assertTrue(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_JSON));
        assertTrue(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_PUB));
        assertTrue(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_RSA));

        // Set central configuration url back to valid, to emulate online configuration loading successful case
        properties.setProperty(ConfigurationProperties.CENTRAL_CONFIGURATION_SERVICE_URL_PROPERTY, confServiceValidUrl);

        configurationManager = new ConfigurationManager(InstrumentationRegistry.getTargetContext(), configurationProperties, cacheConfHandler);

        // Load configuration again, now online load should succeed, and cached configuration overridden with given conf
        long processStartDate = inSeconds(new Date());
        configuration = configurationManager.getConfiguration();
        long processEndDate = inSeconds(new Date());
        assertConfigurationValues(configuration);
        assertConfigurationUpdateDates(configuration, processStartDate, processEndDate);

        assertTrue(cacheConfHandler.doesCachedConfigurationInfoExist());
        assertEquals(configuration.getConfigurationLastUpdateCheckDate(), cacheConfHandler.getConfLastUpdateCheckDate());
        assertEquals(configuration.getConfigurationUpdateDate(), cacheConfHandler.getConfUpdateDate());
        assertTrue(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_JSON));
        assertTrue(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_PUB));
        assertTrue(cacheConfHandler.doesCachedConfigurationFileExists(CachedConfigurationHandler.CACHED_CONFIG_RSA));
    }

    private void updateCachedPropertyConfLastCheckValueToPast() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, - (configurationProperties.getConfigurationUpdateInterval() + 1));
        cacheConfHandler.updateConfigurationLastCheckDate(calendar.getTime());
    }

    private void assertConfigurationValues(ConfigurationProvider configuration) {
        assertNotNull(configuration);
        assertEquals(configurationProperties.getCentralConfigurationServiceUrl(), configuration.getConfigUrl());
        assertNotNull(configuration.getLdapCorpUrl());
        assertNotNull(configuration.getLdapPersonUrl());
        assertNotNull(configuration.getMidSignUrl());
        assertNotNull(configuration.getSivaUrl());
        assertNotNull(configuration.getTsaUrl());
        assertNotNull(configuration.getTslUrl());
        assertNotNull(configuration.getMetaInf().getDate());
        assertNotNull(configuration.getMetaInf().getUrl());
        assertNotNull(configuration.getMetaInf().getSerial());
        assertNotNull(configuration.getMetaInf().getVersion());
        assertFalse(configuration.getTslCerts().isEmpty());
        assertFalse(configuration.getOCSPUrls().isEmpty());
    }

    private void assertDefaultConfigurationValues(ConfigurationProvider configuration) {
        assertNotNull(configuration);
        assertEquals(configurationProperties.getCentralConfigurationServiceUrl(), configuration.getConfigUrl());
        assertNotNull(configuration.getLdapCorpUrl());
        assertEquals("esteid.ldap.sk.ee", configuration.getLdapPersonUrl());
        assertEquals("https://digidocservice.sk.ee", configuration.getMidSignUrl());
        assertEquals("https://siva.eesti.ee/V2/validate", configuration.getSivaUrl());
        assertEquals("http://dd-at.ria.ee/tsa", configuration.getTsaUrl());
        assertEquals("https://ec.europa.eu/information_society/policy/esignature/trusted-list/tl-mp.xml", configuration.getTslUrl());
        assertEquals("20190805110015Z", configuration.getMetaInf().getDate());
        assertEquals("https://id.eesti.ee/config.json", configuration.getMetaInf().getUrl());
        assertSame(93, configuration.getMetaInf().getSerial());
        assertSame(1, configuration.getMetaInf().getVersion());
        assertSame(8, configuration.getTslCerts().size());
        assertSame(13, configuration.getOCSPUrls().size());
    }


    private void assertConfigurationUpdateDates(ConfigurationProvider configuration, long processStartDate, long processEndDate) {
        long confUpdateDate = inSeconds(configuration.getConfigurationUpdateDate());
        long confLastUpdateCheck = inSeconds(configuration.getConfigurationLastUpdateCheckDate());

        assertTrue(confUpdateDate >= processStartDate);
        assertTrue(confUpdateDate <= processEndDate);
        assertTrue(confLastUpdateCheck >= processStartDate);
        assertTrue(confLastUpdateCheck <= processEndDate);
    }

    private long inSeconds(Date date) {
        return date.getTime() / 1000000;
    }

    private void deleteCachedData(File cacheDir) {
        for (File file : cacheDir.listFiles()) {
            if (file.isDirectory() && file.getName().equals("config")) {
                for (File configFile : file.listFiles()) {
                    if (!configFile.delete()) {
                        throw new IllegalStateException("Failed to delete android cached directory file/folder named " + configFile.getName());
                    }
                }
            }
        }
    }
}
