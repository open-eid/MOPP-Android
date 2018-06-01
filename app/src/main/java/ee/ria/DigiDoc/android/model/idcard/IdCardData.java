package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;

import ee.ria.DigiDoc.EIDType;
import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.model.EIDData;

@AutoValue
public abstract class IdCardData implements EIDData {

    public abstract String documentNumber();

    @Nullable public abstract LocalDate expiryDate();

    static IdCardData create(@Nullable EIDType type, String givenNames, String surname,
                             String personalCode, String citizenship,
                             @Nullable LocalDate dateOfBirth, CertificateData authCertificate,
                             CertificateData signCertificate, int pukRetryCount,
                             String documentNumber, @Nullable LocalDate expiryDate) {
        return new AutoValue_IdCardData(type, givenNames, surname, personalCode, citizenship,
                dateOfBirth, authCertificate, signCertificate, pukRetryCount, documentNumber,
                expiryDate);
    }
}
