package ee.ria.DigiDoc.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class ConfigurationParseTest {

    private static final String TEST_JSON =
            "{" +
            "  \"TERA-SUPPORTED\": \"1.0.0.5\"," +
            "  \"META-INF\": {" +
            "    \"URL\": \"https://id.eesti.ee/config.json\"," +
            "    \"DATE\": \"20190805110015Z\"," +
            "    \"SERIAL\": 93," +
            "    \"VER\": 1" +
            "  }," +
            "  \"PICTURE-URL\": \"https://sisene.www.eesti.ee/idportaal/portaal.idpilt\"," +
            "  \"TSL-CERTS\": [" +
            "    \"a\","+
            "    \"b\","+
            "    \"c\""+
            "  ]," +
            "  \"OCSP-URL-ISSUER\": {" +
            "    \"KLASS3-SK 2010\": \"http://ocsp.sk.ee\"," +
            "    \"KLASS3-SK 2016\": \"http://ocsp.sk.ee\"," +
            "    \"TEST of KLASS3-SK 2010\": \"http://demo.sk.ee/ocsp\"" +
            "  }" +
            "}";



    @Test
    public void parseStringValue() {
        ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
        assertEquals("1.0.0.5", configurationParser.parseStringValue("TERA-SUPPORTED"));
        assertEquals("20190805110015Z", configurationParser.parseStringValue("META-INF", "DATE"));
    }

    @Test
    public void parseMissingStringValue() {
        assertThrows(RuntimeException.class, () -> {
            ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
            assertNull(configurationParser.parseStringValue("MISSING-VALUE"));
        }, "Failed to parse parameter 'MISSING-VALUE' from configuration json");
    }

    @Test
    public void parseStringValues() {
        ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
        List<String> certs = configurationParser.parseStringValues("TSL-CERTS");
        assertEquals("a", certs.get(0));
        assertEquals("b", certs.get(1));
        assertEquals("c", certs.get(2));
    }

    @Test
    public void parseIntValue() {
        ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
        assertEquals(93, configurationParser.parseIntValue("META-INF", "SERIAL"));
    }

    @Test
    public void parseStringValuesToMap() {
        ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
        Map<String, String> issuers = configurationParser.parseStringValuesToMap("OCSP-URL-ISSUER");
        assertEquals("http://ocsp.sk.ee", issuers.get("KLASS3-SK 2010"));
        assertEquals("http://ocsp.sk.ee", issuers.get("KLASS3-SK 2016"));
        assertEquals("http://demo.sk.ee/ocsp", issuers.get("TEST of KLASS3-SK 2010"));
    }
}
