package ee.ria.DigiDoc.configuration;

import android.content.res.AssetManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationPropertiesTest {

    @Mock
    private AssetManager assetManager;

    @Test
    public void loadProperties() throws IOException {
        when(assetManager.open(anyString())).thenReturn(getClass().getClassLoader().getResourceAsStream(ConfigurationProperties.PROPERTIES_FILE_NAME));
        ConfigurationProperties configurationProperties = new ConfigurationProperties(assetManager);
        assertEquals("https://id.eesti.ee/", configurationProperties.getCentralConfigurationServiceUrl());
        assertSame(7, configurationProperties.getConfigurationUpdateInterval());
    }
}
