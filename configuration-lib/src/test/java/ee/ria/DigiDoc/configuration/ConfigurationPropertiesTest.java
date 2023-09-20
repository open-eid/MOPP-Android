package ee.ria.DigiDoc.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.res.AssetManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationPropertiesTest {

    @Mock
    private AssetManager assetManager;

    @Test
    public void loadProperties() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader != null) {
            try (InputStream inputStream = classLoader.getResourceAsStream(ConfigurationProperties.PROPERTIES_FILE_NAME);
                 InputStream assetStream = assetManager.open(anyString())) {
                if (inputStream != null) {
                    when(assetStream).thenReturn(inputStream);

                    ConfigurationProperties configurationProperties = new ConfigurationProperties(assetManager);

                    assertEquals("https://id.eesti.ee/", configurationProperties.getCentralConfigurationServiceUrl());
                    assertSame(4, configurationProperties.getConfigurationUpdateInterval());
                } else {
                    throw new IllegalStateException("Unable to get properties file name");
                }
            }
        } else {
            throw new IllegalStateException("Unable to get ClassLoader");
        }
    }
}
