package ee.ria.DigiDoc.android.auth;

import com.google.auto.value.AutoValue;

import java.io.File;
import java.io.InputStream;

import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.idcard.Token;

@AutoValue
public abstract class AuthRequest {
    abstract Token token();
    abstract String hash ();
    abstract String pin1();
    abstract String sessionId();
    abstract Certificate certificate();
    abstract InputStream keystore();
    abstract InputStream properties();

    public static AuthRequest create(Token token, String hash, String pin1, String sessionId, Certificate certificate,
                                     InputStream keystore, InputStream properties) {
        return new AutoValue_AuthRequest(token, hash, pin1, sessionId, certificate, keystore, properties);
    }
}
