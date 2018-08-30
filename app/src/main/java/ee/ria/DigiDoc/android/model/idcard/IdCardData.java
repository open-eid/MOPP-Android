package ee.ria.DigiDoc.android.model.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.EIDType;
import ee.ria.DigiDoc.idcard.PersonalData;

@AutoValue
public abstract class IdCardData {

    public abstract EIDType type();

    public abstract PersonalData personalData();

    public abstract Certificate authCertificate();

    public abstract Certificate signCertificate();

    public abstract int pin1RetryCount();

    public abstract int pin2RetryCount();

    public abstract int pukRetryCount();

    static IdCardData create(EIDType type, PersonalData personalData,
                             Certificate authCertificate, Certificate signCertificate,
                             int pin1RetryCount, int pin2RetryCount, int pukRetryCount) {
        return new AutoValue_IdCardData(type, personalData, authCertificate, signCertificate,
                pin1RetryCount, pin2RetryCount, pukRetryCount);
    }
}
