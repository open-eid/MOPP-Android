package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddRequest;
import ee.ria.DigiDoc.idcard.Token;

@AutoValue
public abstract class IdCardRequest implements SignatureAddRequest {

    public abstract Token token();

    public abstract String pin2();

    static IdCardRequest create(Token token, String pin2) {
        return new AutoValue_IdCardRequest(token, pin2);
    }
}
