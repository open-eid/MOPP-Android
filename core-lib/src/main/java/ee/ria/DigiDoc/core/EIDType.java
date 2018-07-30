package ee.ria.DigiDoc.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.PolicyInformation;

public enum EIDType {

    UNKNOWN, ID_CARD, DIGI_ID, MOBILE_ID, E_SEAL;

    @NonNull
    public static EIDType parse(@Nullable CertificatePolicies certificatePolicies) {
        if (certificatePolicies == null) {
            return UNKNOWN;
        }
        for (PolicyInformation policyInformation : certificatePolicies.getPolicyInformation()) {
            String identifier = policyInformation.getPolicyIdentifier().getId();
            if (identifier.startsWith("1.3.6.1.4.1.10015.1.1")
                    || identifier.startsWith("1.3.6.1.4.1.51361.1.1.1")) {
                return ID_CARD;
            } else if (identifier.startsWith("1.3.6.1.4.1.10015.1.2")
                    || identifier.startsWith("1.3.6.1.4.1.51361.1.1")
                    || identifier.startsWith("1.3.6.1.4.1.51455.1.1")) {
                return DIGI_ID;
            } else if (identifier.startsWith("1.3.6.1.4.1.10015.1.3")
                    || identifier.startsWith("1.3.6.1.4.1.10015.11.1")) {
                return MOBILE_ID;
            } else if (identifier.startsWith("1.3.6.1.4.1.10015.7.3")
                    || identifier.startsWith("1.3.6.1.4.1.10015.7.1")
                    || identifier.startsWith("1.3.6.1.4.1.10015.2.1")) {
                return E_SEAL;
            }
        }
        return UNKNOWN;
    }
}
