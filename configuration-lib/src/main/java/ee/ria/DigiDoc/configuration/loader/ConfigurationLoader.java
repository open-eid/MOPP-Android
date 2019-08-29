package ee.ria.DigiDoc.configuration.loader;

public abstract class ConfigurationLoader {

    String configurationJson;
    String configurationSignature;
    String configurationSignaturePublicKey;

    public void load() {
        this.configurationJson = loadConfigurationJson().trim();
        assertValueNotBlank(configurationJson, "configuration json");
        this.configurationSignature = loadConfigurationSignature().trim();
        assertValueNotBlank(configurationJson, "configuration signature");
        this.configurationSignaturePublicKey = loadConfigurationSignaturePublicKey().trim();
        assertValueNotBlank(configurationJson, "configuration signature public key");
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
