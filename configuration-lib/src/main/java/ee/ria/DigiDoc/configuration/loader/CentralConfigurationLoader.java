package ee.ria.DigiDoc.configuration.loader;

import android.content.Context;

import org.bouncycastle.util.encoders.Base64;

import java.security.cert.X509Certificate;

/**
 * Configuration loader from central configuration service.
 */
public class CentralConfigurationLoader extends ConfigurationLoader {

    private final CentralConfigurationClient configurationClient;

    public CentralConfigurationLoader(String configurationServiceUrl, X509Certificate sslCert, Context context) {
        this.configurationClient = new CentralConfigurationClient(configurationServiceUrl, sslCert, context);
    }

    @Override
    public String loadConfigurationJson() {
        super.configurationJson = configurationClient.getConfiguration().trim();
        assertConfigurationJson();
        return configurationJson;
    }

    @Override
    public byte[] loadConfigurationSignature() {
        String trimmedConfigurationSignature = configurationClient.getConfigurationSignature().trim();
        super.configurationSignature = Base64.decode(trimmedConfigurationSignature);
        assertConfigurationSignature();
        return configurationSignature;
    }

    @Override
    public String loadConfigurationSignaturePublicKey() {
        super.configurationSignaturePublicKey = configurationClient.getConfigurationSignaturePublicKey().trim();
        assertConfigurationSignaturePublicKey();
        return configurationSignaturePublicKey;
    }
}
