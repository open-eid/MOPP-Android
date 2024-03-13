package ee.ria.DigiDoc.configuration.verify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import ee.ria.DigiDoc.configuration.util.FileUtils;

public class SignatureVerifierTest {

    @Test
    public void verifyValidSignature() {
        ClassLoader classLoader = Optional.ofNullable(getClass().getClassLoader())
                .orElseThrow(() -> new IllegalStateException("Unable to get ClassLoader"));
        try (
                InputStream configJsonStream = classLoader.getResourceAsStream("config.json");
                InputStream configSignatureStream = classLoader.getResourceAsStream("config.rsa");
                InputStream configSignaturePublicKeyStream = classLoader.getResourceAsStream("config.pub")
        ) {
            String configJson = FileUtils.readFileContent(configJsonStream);
            byte[] configSignature = FileUtils.readFileContentBytes(configSignatureStream);
            String configSignaturePublicKey = FileUtils.readFileContent(configSignaturePublicKeyStream);
            assertTrue(SignatureVerifier.verify(configSignature, configSignaturePublicKey, configJson));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read resource");
        }
    }

    @Test
    public void verifyInvalidSignature() {
        ClassLoader classLoader = Optional.ofNullable(getClass().getClassLoader())
                .orElseThrow(() -> new IllegalStateException("Unable to get ClassLoader"));
        try (
                InputStream configJsonStream = classLoader.getResourceAsStream("config.json");
                InputStream configSignatureStream = classLoader.getResourceAsStream("config.rsa");
                InputStream configSignaturePublicKeyStream = classLoader.getResourceAsStream("config.pub")
        ) {
            String configJson = FileUtils.readFileContent(configJsonStream);
            byte[] configSignature = FileUtils.readFileContentBytes(configSignatureStream);
            String configSignaturePublicKey = FileUtils.readFileContent(configSignaturePublicKeyStream);
            assertFalse(SignatureVerifier.verify(configSignature, configSignaturePublicKey, configJson + "a"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read resource");
        }
    }
}
