package ee.ria.DigiDoc.configuration.loader;

/**
 * Configuration loader from central configuration service.
 */
public class CentralConfigurationLoader extends ConfigurationLoader {

    private final CentralConfigurationClient configurationClient;

    public CentralConfigurationLoader(String configurationServiceUrl) {
        this.configurationClient = new CentralConfigurationClient(configurationServiceUrl);
    }

    @Override
    String loadConfigurationJson() {
        return configurationClient.getConfiguration();
    }

    @Override
    String loadConfigurationSignature() {
        return configurationClient.getConfigurationSignature();
    }

    @Override
    String loadConfigurationSignaturePublicKey() {
        return configurationClient.getConfigurationSignaturePublicKey();
    }
}
