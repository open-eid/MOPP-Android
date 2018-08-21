package ee.ria.DigiDoc.android.auth;

import com.google.auto.value.AutoValue;

import java.io.File;

import ee.ria.DigiDoc.idcard.Token;

@AutoValue
public abstract class AuthRequest {
    abstract Token token();
    abstract String hash ();
    abstract String pin1();

    public static AuthRequest create(Token token, String hash, String pin1) {
        return new AutoValue_AuthRequest(token, hash, pin1);
    }
}
