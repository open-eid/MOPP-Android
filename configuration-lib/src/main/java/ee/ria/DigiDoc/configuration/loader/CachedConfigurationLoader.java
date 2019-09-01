package ee.ria.DigiDoc.configuration.loader;

public class CachedConfigurationLoader extends ConfigurationLoader {

    private final CachedConfigurationHandler confCacheHandler;

    public CachedConfigurationLoader(CachedConfigurationHandler confCacheHandler) {
        this.confCacheHandler = confCacheHandler;
    }

    @Override
    String loadConfigurationJson() {
        return confCacheHandler.readFileContent(CachedConfigurationHandler.CACHED_CONFIG_JSON);
    }

    @Override
    String loadConfigurationSignature() {
        return confCacheHandler.readFileContent(CachedConfigurationHandler.CACHED_CONFIG_RSA);
    }

    @Override
    String loadConfigurationSignaturePublicKey() {
        return "";
    }

    @Override
    void assertConfigurationSignaturePublicKey() {
        // Do nothing
    }
}
