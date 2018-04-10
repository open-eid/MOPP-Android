package ee.ria.DigiDoc.android.model;

import android.support.annotation.Nullable;

public interface EIDData {

    @Nullable @EIDType String type();

    String givenNames();

    String surname();

    String personalCode();

    String citizenship();

    CertificateData authCertificate();

    CertificateData signCertificate();
}
