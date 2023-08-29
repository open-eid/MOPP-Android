package ee.ria.DigiDoc.configuration.verify;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ee.ria.DigiDoc.configuration.util.FileUtils;

public class SignatureVerifierTest {

    @Test
    public void verifyValidSignature() {
        String configJson = FileUtils.readFileContent(getClass().getClassLoader().getResourceAsStream("config.json"));
        byte[] configSignature = FileUtils.readFileContentBytes(getClass().getClassLoader().getResourceAsStream("config.rsa"));
        String configSignaturePublicKey = FileUtils.readFileContent(getClass().getClassLoader().getResourceAsStream("config.pub"));
        assertTrue(SignatureVerifier.verify(configSignature, configSignaturePublicKey, configJson));
    }

    @Test
    public void verifyInvalidSignature() {
        String configJson = FileUtils.readFileContent(getClass().getClassLoader().getResourceAsStream("config.json"));
        byte[] configSignature = FileUtils.readFileContentBytes(getClass().getClassLoader().getResourceAsStream("config.rsa"));
        String configSignaturePublicKey = FileUtils.readFileContent(getClass().getClassLoader().getResourceAsStream("config.pub"));
        assertFalse(SignatureVerifier.verify(configSignature, configSignaturePublicKey, configJson + "a"));
    }
}
