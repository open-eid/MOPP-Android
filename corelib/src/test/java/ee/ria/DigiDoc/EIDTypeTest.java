package ee.ria.DigiDoc;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static ee.ria.DigiDoc.EIDType.DIGI_ID;
import static ee.ria.DigiDoc.EIDType.ID_CARD;
import static ee.ria.DigiDoc.EIDType.MOBILE_ID;
import static ee.ria.DigiDoc.EIDType.UNKNOWN;
import static ee.ria.DigiDoc.EIDType.parseOrganization;

public class EIDTypeTest {

    @Test
    public void parseOrganization_IdCard() {
        assertThat(parseOrganization("ESTEID")).isEqualTo(ID_CARD);
    }

    @Test
    public void parseOrganization_digiId() {
        assertThat(parseOrganization("ESTEID (DIGI-ID)")).isEqualTo(DIGI_ID);
    }

    @Test
    public void parseOrganization_mobileId() {
        assertThat(parseOrganization("ESTEID (MOBIIL-ID)")).isEqualTo(MOBILE_ID);
    }

    @Test
    public void parseOrganization_emptyString() {
        assertThat(parseOrganization("")).isEqualTo(UNKNOWN);
    }

    @Test
    public void parseOrganization_null() {
        assertThat(parseOrganization(null)).isEqualTo(UNKNOWN);
    }
}
