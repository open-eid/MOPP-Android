package ee.ria.DigiDoc.crypto;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LdapFilterTest {

    private static final String SERIAL_NUMBER_QUERY = "1234567890";
    private static final String CN_QUERY = "ASD QWERTY";

    @Test
    public void shouldBeSerialNumberSearchWhenNumericalQuery() {
        assertThat(new LdapFilter(SERIAL_NUMBER_QUERY).isSerialNumberSearch(), is(true));
    }

    @Test
    public void shouldNotBeSerialNumberSearchWhenNotNumericalQuery() {
        assertThat(new LdapFilter(CN_QUERY).isSerialNumberSearch(), is(false));
    }

    @Test
    public void shouldReturnSerialNumberFilterStringWhenSerialNumberQuery() {
        String filterString = new LdapFilter(SERIAL_NUMBER_QUERY).filterString();
        assertThat(filterString, is("(serialNumber=" + SERIAL_NUMBER_QUERY + ")"));
    }

    @Test
    public void shouldReturnCnFilterStringWhenNotSerialNumberQuery() {
        String filterString = new LdapFilter(CN_QUERY).filterString();
        assertThat(filterString, is("(cn=*" + CN_QUERY + "*)"));
    }
}
