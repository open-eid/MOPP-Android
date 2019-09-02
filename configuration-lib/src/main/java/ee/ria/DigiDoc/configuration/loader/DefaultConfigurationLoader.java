package ee.ria.DigiDoc.configuration.loader;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

import ee.ria.DigiDoc.configuration.util.FileUtils;

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
    byte[] loadConfigurationSignature() {
        return readFileContentBytes(DEFAULT_CONFIG_RSA);
    }

    @Override
    public String loadConfigurationSignaturePublicKey() {
        return readFileContent(DEFAULT_CONFIG_PUB);
    }

    private String readFileContent(String filename) {
        try (InputStream inputStream = assetManager.open("config/" + filename)) {
            return FileUtils.readFileContent(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file content '" + filename + "'", e);
        }
    }

    private byte[] readFileContentBytes(String filename) {
        try (InputStream inputStream = assetManager.open("config/" + filename)) {
            return FileUtils.readFileContentBytes(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file content '" + filename + "'", e);
        }
    }
}
