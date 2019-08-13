package ee.ria.DigiDoc.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

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
            "    \"TEST of KLASS3-SK 2010\": \"http://demo.sk.ee/ocsp\"," +
            "  }" +
            "}";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parseStringValue() {
        ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
        assertEquals("1.0.0.5", configurationParser.parseStringValue("TERA-SUPPORTED"));
        assertEquals("20190805110015Z", configurationParser.parseStringValue("META-INF", "DATE"));
    }

    @Test
    public void parseMissingStringValue() {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Failed to parse parameter 'MISSING-VALUE' from configuration json");
        ConfigurationParser configurationParser = new ConfigurationParser(TEST_JSON);
        assertNull(configurationParser.parseStringValue("MISSING-VALUE"));
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
        assertSame(93, configurationParser.parseIntValue("META-INF", "SERIAL"));
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
