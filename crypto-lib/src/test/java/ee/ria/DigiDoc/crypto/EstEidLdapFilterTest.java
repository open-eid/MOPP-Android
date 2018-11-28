package ee.ria.DigiDoc.crypto;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EstEidLdapFilterTest {

    private static final String SERIAL_NUMBER_QUERY = "1234567890";
    private static final String CN_QUERY = "ASD QWERTY";

    @Test
    public void shouldReturnSerialNumberFilterStringWhenSerialNumberQuery() {
        String filterString = new EstEidLdapFilter(SERIAL_NUMBER_QUERY).filterString();
        assertThat(filterString, is("(serialNumber= PNOEE-" + SERIAL_NUMBER_QUERY + ")"));
    }

    @Test
    public void shouldReturnCnFilterStringWhenNotSerialNumberQuery() {
        String filterString = new EstEidLdapFilter(CN_QUERY).filterString();
        assertThat(filterString, is("(cn= " + CN_QUERY + ")"));
    }
}
