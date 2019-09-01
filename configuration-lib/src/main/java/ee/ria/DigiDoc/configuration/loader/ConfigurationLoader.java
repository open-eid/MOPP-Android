package ee.ria.DigiDoc.configuration.loader;

public abstract class ConfigurationLoader {

    String configurationJson;
    String configurationSignature;
    String configurationSignaturePublicKey;

    public void load() {
        this.configurationJson = loadConfigurationJson().trim();
        assertConfigurationJson();
        this.configurationSignature = loadConfigurationSignature().trim();
        assertConfigurationSignature();
        this.configurationSignaturePublicKey = loadConfigurationSignaturePublicKey().trim();
        assertConfigurationSignaturePublicKey();
    }

    void assertConfigurationJson() {
        assertValueNotBlank(configurationJson, "configuration json");
    }

    void assertConfigurationSignature() {
        assertValueNotBlank(configurationSignature, "configuration signature");
    }

    void assertConfigurationSignaturePublicKey() {
        assertValueNotBlank(configurationSignaturePublicKey, "configuration signature public key");
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

    void assertValueNotBlank(String value, String valueName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Loaded " + valueName + " file is blank");
        }
    }
}
