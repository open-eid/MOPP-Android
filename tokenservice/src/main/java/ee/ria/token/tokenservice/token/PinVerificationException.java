package ee.ria.token.tokenservice.token;

public class PinVerificationException extends TokenException {

    private Token.PinType pinType;

    public PinVerificationException(Token.PinType pinType) {
        super();
        this.pinType = pinType;
    }

    public Token.PinType getPinType() {
        return pinType;
    }

    @Override
    public String getMessage() {
        return createExceptionMessage();
    }

    private String createExceptionMessage() {
        switch (pinType) {
            case PIN1:
                return "PIN1 login failed";
            case PIN2:
                return "PIN2 login failed";
            case PUK:
                return "PUK login failed";
        }
        return "Verification failed";
    }
}
