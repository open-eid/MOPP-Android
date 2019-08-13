package ee.ria.DigiDoc.configuration;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SignatureVerifierTest {

    @Test
    public void verifyValidSignature() {
        String configJson = readFileContent("config.json");
        String configSignature = readFileContent("config.rsa");
        String configSignaturePublicKey = readFileContent("config.pub");
        assertTrue(SignatureVerifier.verify(configSignature, configSignaturePublicKey, configJson));
    }

    @Test
    public void verifyInvalidSignature() {
        String configJson = readFileContent("config.json");
        String configSignature = readFileContent("config.rsa");
        String configSignaturePublicKey = readFileContent("config.pub");
        assertFalse(SignatureVerifier.verify(configSignature, configSignaturePublicKey, configJson + "a"));
    }

    private String readFileContent(String filename) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
                if (line != null) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read content of file '" + filename + "'", e);
        }
    }
}
