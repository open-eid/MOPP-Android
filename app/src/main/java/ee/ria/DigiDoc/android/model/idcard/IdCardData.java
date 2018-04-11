package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;

import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.model.EIDData;
import ee.ria.DigiDoc.android.model.EIDType;

@AutoValue
public abstract class IdCardData implements EIDData {

    public abstract int pin1RetryCounter();

    public abstract int pin2RetryCounter();

    public abstract String documentNumber();

    @Nullable public abstract LocalDate expiryDate();

    static IdCardData create(@Nullable @EIDType String type, String givenNames, String surname,
                             String personalCode, String citizenship,
                             CertificateData authCertificate, CertificateData signCertificate,
                             int pin1RetryCounter, int pin2RetryCounter, String documentNumber,
                             @Nullable LocalDate expiryDate) {
        return new AutoValue_IdCardData(type, givenNames, surname, personalCode, citizenship,
                authCertificate, signCertificate, pin1RetryCounter, pin2RetryCounter,
                documentNumber, expiryDate);
    }
}
