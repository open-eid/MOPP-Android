package ee.ria.DigiDoc.android.model.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.EIDType;
import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.model.EIDData;
import ee.ria.tokenlibrary.PersonalData;

@AutoValue
public abstract class IdCardData implements EIDData {

    static IdCardData create(EIDType type, PersonalData personalData,
                             CertificateData authCertificate, CertificateData signCertificate,
                             int pukRetryCount) {
        return new AutoValue_IdCardData(type, personalData, authCertificate, signCertificate,
                pukRetryCount);
    }
}
