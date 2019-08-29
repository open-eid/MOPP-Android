package ee.ria.DigiDoc.configuration.loader;

import ee.ria.DigiDoc.configuration.SignatureVerifier;

public abstract class ConfigurationLoader {

    private String configurationJson;
    private String configurationSignature;
    private String configurationSignaturePublicKey;

    public void load() {
        this.configurationJson = loadConfigurationJson().trim();
        assertValueNotBlank(configurationJson, "configuration json");
        this.configurationSignature = loadConfigurationSignature().trim();
        assertValueNotBlank(configurationJson, "configuration signature");
        this.configurationSignaturePublicKey = loadConfigurationSignaturePublicKey().trim();
        assertValueNotBlank(configurationJson, "configuration signature public key");

        verifyConfigurationSignature();
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public String getConfigurationSignature() {
        return configurationSignature;
    }

    public String getConfigurationSignaturePublicKey() {
        return configurationSignaturePublicKey;
    }

    abstract String loadConfigurationJson();

    abstract String loadConfigurationSignature();

    abstract String loadConfigurationSignaturePublicKey();

    private void assertValueNotBlank(String value, String valueName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Loaded " + valueName + " file is blank");
        }
    }

    private void verifyConfigurationSignature() {
        boolean signatureValid = SignatureVerifier.verify(configurationSignature, configurationSignaturePublicKey, configurationJson);
        if (!signatureValid) {
            throw new IllegalStateException("Configuration signature validation failed!");
        }
    }
}
