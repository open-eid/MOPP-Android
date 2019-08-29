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
    public String loadConfigurationJson() {
        super.configurationJson = configurationClient.getConfiguration().trim();
        assertValueNotBlank(configurationJson, "configuration json");
        return configurationJson;
    }

    @Override
    public String loadConfigurationSignature() {
        super.configurationSignature = configurationClient.getConfigurationSignature().trim();
        assertValueNotBlank(configurationSignature, "configuration signature");
        return configurationSignature;
    }

    @Override
    public String loadConfigurationSignaturePublicKey() {
        super.configurationSignaturePublicKey = configurationClient.getConfigurationSignaturePublicKey().trim();
        assertValueNotBlank(configurationSignaturePublicKey, "configuration signature public key");
        return configurationSignaturePublicKey;
    }
}
