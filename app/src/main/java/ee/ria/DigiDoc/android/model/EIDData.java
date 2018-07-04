package ee.ria.DigiDoc.android.model;

import ee.ria.DigiDoc.EIDType;
import ee.ria.tokenlibrary.PersonalData;

public interface EIDData {

    EIDType type();

    PersonalData personalData();

    CertificateData authCertificate();

    CertificateData signCertificate();

    int pukRetryCount();
}
