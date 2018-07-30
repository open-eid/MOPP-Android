package ee.ria.DigiDoc.core;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static ee.ria.DigiDoc.core.EIDType.DIGI_ID;
import static ee.ria.DigiDoc.core.EIDType.E_SEAL;
import static ee.ria.DigiDoc.core.EIDType.ID_CARD;
import static ee.ria.DigiDoc.core.EIDType.MOBILE_ID;
import static ee.ria.DigiDoc.core.EIDType.UNKNOWN;
import static ee.ria.DigiDoc.core.EIDType.parse;

public class EIDTypeTest {

    @Test
    public void parseCertificatePolicies_idCardVNext() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.51361.1.1.1")))
                .isEqualTo(ID_CARD);
    }

    @Test
    public void parseCertificatePolicies_idCardV7_0() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.1.1")))
                .isEqualTo(ID_CARD);
    }

    @Test
    public void parseCertificatePolicies_digiIdVNext() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.51455.1.1")))
                .isEqualTo(DIGI_ID);
    }

    @Test
    public void parseCertificatePolicies_digiIdVNextOther() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.51361.1.1")))
                .isEqualTo(DIGI_ID);
    }

    @Test
    public void parseCertificatePolicies_digiIdV7_0() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.1.2")))
                .isEqualTo(DIGI_ID);
    }

    @Test
    public void parseCertificatePolicies_mobileIdV6_0() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.1.3")))
                .isEqualTo(MOBILE_ID);
    }

    @Test
    public void parseCertificatePolicies_mobileIdV2_0Expired() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.11.1")))
                .isEqualTo(MOBILE_ID);
    }

    @Test
    public void parseCertificatePolicies_eSealV10() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.7.3")))
                .isEqualTo(E_SEAL);
    }

    @Test
    public void parseCertificatePolicies_eSealV5_0() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.7.1")))
                .isEqualTo(E_SEAL);
    }

    @Test
    public void parseCertificatePolicies_eSealV1_1() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.10015.2.1")))
                .isEqualTo(E_SEAL);
    }

    @Test
    public void parseCertificatePolicies_null() {
        assertThat(parse(null))
                .isEqualTo(UNKNOWN);
    }

    @Test
    public void parseCertificatePolicies_unknownIdentifier() {
        assertThat(parse(certificatePolicies("1.3.6.1.4.1.12345.1.1")))
                .isEqualTo(UNKNOWN);
    }

    private static CertificatePolicies certificatePolicies(String identifier) {
        return new CertificatePolicies(new PolicyInformation(new ASN1ObjectIdentifier(identifier)));
    }
}
