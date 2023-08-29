package ee.ria.DigiDoc.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.res.AssetManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class ConfigurationPropertiesTest {

    @Mock
    private AssetManager assetManager;

    @Test
    public void loadProperties() throws IOException {
        when(assetManager.open(anyString())).thenReturn(getClass().getClassLoader().getResourceAsStream(ConfigurationProperties.PROPERTIES_FILE_NAME));
        ConfigurationProperties configurationProperties = new ConfigurationProperties(assetManager);
        assertEquals("https://id.eesti.ee/", configurationProperties.getCentralConfigurationServiceUrl());
        assertSame(4, configurationProperties.getConfigurationUpdateInterval());
    }
}
