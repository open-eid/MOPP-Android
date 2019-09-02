package ee.ria.DigiDoc.configuration.verify;


import org.junit.Test;

import ee.ria.DigiDoc.configuration.util.FileUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
