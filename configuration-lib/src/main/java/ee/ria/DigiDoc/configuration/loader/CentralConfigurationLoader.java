package ee.ria.DigiDoc.configuration.loader;

import java.security.cert.X509Certificate;

/**
 * Configuration loader from central configuration service.
 */
public class CentralConfigurationLoader extends ConfigurationLoader {

    private final CentralConfigurationClient configurationClient;

    public CentralConfigurationLoader(String configurationServiceUrl, X509Certificate sslCert) {
        this.configurationClient = new CentralConfigurationClient(configurationServiceUrl, sslCert);
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
