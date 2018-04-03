package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.tokenlibrary.Token;
import okio.ByteString;

@AutoValue
public abstract class IdCardCertData {

    public abstract Token.CertType type();

    public abstract ByteString data();

    static IdCardCertData create(Token.CertType type, ByteString data) {
        return new AutoValue_IdCardCertData(type, data);
    }
}
