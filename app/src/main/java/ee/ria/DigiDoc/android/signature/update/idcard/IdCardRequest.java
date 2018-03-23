package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddRequest;

@AutoValue
public abstract class IdCardRequest implements SignatureAddRequest {

    static IdCardRequest create() {
        return new AutoValue_IdCardRequest();
    }
}
