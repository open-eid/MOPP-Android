package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddRequest;

@AutoValue
public abstract class IdCardRequest implements SignatureAddRequest {

    public abstract String pin2();

    static IdCardRequest create(String pin2) {
        return new AutoValue_IdCardRequest(pin2);
    }
}
