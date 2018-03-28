package ee.ria.DigiDoc.android.signature.update.idcard;

import com.google.auto.value.AutoValue;

import ee.ria.token.tokenservice.TokenService;

@AutoValue
public abstract class TokenServiceConnectData {

    public abstract TokenService tokenService();

    public abstract boolean cardPresent();

    static TokenServiceConnectData create(TokenService tokenService, boolean cardPresent) {
        return new AutoValue_TokenServiceConnectData(tokenService, cardPresent);
    }
}
