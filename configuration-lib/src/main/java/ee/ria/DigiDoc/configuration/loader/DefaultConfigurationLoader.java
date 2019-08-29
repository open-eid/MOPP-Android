package ee.ria.DigiDoc.configuration.loader;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Default configuration loader.
 * Default configuration is packaged into APK, located in assets folder.
 * Should be used as initial configuration and if configuration loading from central configuration service fails.
 */
public class DefaultConfigurationLoader extends ConfigurationLoader {

    public static final String DEFAULT_CONFIG_JSON = "default-config.json";
    public static final String DEFAULT_CONFIG_RSA = "default-config.rsa";
    public static final String DEFAULT_CONFIG_PUB = "default-config.pub";
    private final AssetManager assetManager;

    public DefaultConfigurationLoader(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    String loadConfigurationJson() {
        return readFileContent(DEFAULT_CONFIG_JSON);
    }

    @Override
    String loadConfigurationSignature() {
        return readFileContent(DEFAULT_CONFIG_RSA);
    }

    @Override
    public String loadConfigurationSignaturePublicKey() {
        return readFileContent(DEFAULT_CONFIG_PUB);
    }

    private String readFileContent(String filename) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        assetManager.open("config/" + filename)))) {
            StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = reader.readLine();
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file content", e);
        }
    }
}
