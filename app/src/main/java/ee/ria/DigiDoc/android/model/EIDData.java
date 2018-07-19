package ee.ria.DigiDoc.android.model;

import ee.ria.DigiDoc.core.EIDType;
import ee.ria.DigiDoc.idcard.PersonalData;

public interface EIDData {

    EIDType type();

    PersonalData personalData();

    CertificateData authCertificate();

    CertificateData signCertificate();

    int pukRetryCount();
}
