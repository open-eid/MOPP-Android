package ee.ria.DigiDoc.configuration.loader;

public abstract class ConfigurationLoader {

    String configurationJson;
    byte[] configurationSignature;
    String configurationSignaturePublicKey;

    public void load() {
        this.configurationJson = loadConfigurationJson().trim();
        assertConfigurationJson();
        this.configurationSignature = loadConfigurationSignature();
        assertConfigurationSignature();
        this.configurationSignaturePublicKey = loadConfigurationSignaturePublicKey().trim();
        assertConfigurationSignaturePublicKey();
    }

    void assertConfigurationJson() {
        assertValueNotBlank(configurationJson, "configuration json");
    }

    void assertConfigurationSignature() {
        if (configurationSignature == null || configurationSignature.length <= 0) {
            throw new IllegalStateException("Loaded configuration signature file is blank");
        }
    }

    void assertConfigurationSignaturePublicKey() {
        assertValueNotBlank(configurationSignaturePublicKey, "configuration signature public key");
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public byte[] getConfigurationSignature() {
        return configurationSignature;
    }

    public String getConfigurationSignaturePublicKey() {
        return configurationSignaturePublicKey;
    }

    abstract String loadConfigurationJson();

    abstract byte[] loadConfigurationSignature();

    abstract String loadConfigurationSignaturePublicKey();

    private void assertValueNotBlank(String value, String valueName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Loaded " + valueName + " file is blank");
        }
    }
}
