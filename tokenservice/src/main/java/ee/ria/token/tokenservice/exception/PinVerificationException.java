package ee.ria.token.tokenservice.exception;

import ee.ria.token.tokenservice.Token;

public class PinVerificationException extends Exception {

    private Token.PinType pinType;

    public PinVerificationException(String message, Token.PinType pinType) {
        super(message);
        this.pinType = pinType;
    }

    public Token.PinType getPinType() {
        return pinType;
    }
}
