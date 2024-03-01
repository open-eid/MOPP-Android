package ee.ria.DigiDoc.android.signature.update.idcard;

import ee.ria.DigiDoc.android.signature.update.SignatureAddRequest;
import ee.ria.DigiDoc.idcard.Token;

public abstract class IdCardRequest implements SignatureAddRequest {

    private final Token token;
    private final byte[] pin2;

    protected IdCardRequest(Token token, byte[] pin2) {
        this.token = token;
        this.pin2 = pin2;
    }

    public Token token() {
        return token;
    }

    public byte[] pin2() {
        return pin2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Token token;
        private byte[] pin2;

        public Builder token(Token token) {
            this.token = token;
            return this;
        }

        public Builder pin2(byte[] pin2) {
            this.pin2 = pin2;
            return this;
        }

        public IdCardRequest build() {
            if (token == null) {
                throw new IllegalStateException("Token must be set");
            }
            if (pin2 == null) {
                throw new IllegalStateException("PIN2 must be set");
            }
            return new IdCardRequest(token, pin2) {};
        }
    }
}
