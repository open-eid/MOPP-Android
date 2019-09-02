package ee.ria.DigiDoc.configuration.verify;

public class ConfigurationSignatureVerifier {

    private final String publicKey;

    public ConfigurationSignatureVerifier(String publicKey) {
        this.publicKey = publicKey;
    }

    public void verifyConfigurationSignature(String config, byte[] signature) {
        boolean signatureValid = SignatureVerifier.verify(signature, publicKey, config);
        if (!signatureValid) {
            throw new IllegalStateException("Configuration signature validation failed!");
        }
    }
}
