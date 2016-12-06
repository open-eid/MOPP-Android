package ee.ria.token.tokenservice.token;

class SignOperationFailedException extends TokenException {
    SignOperationFailedException(Token.PinType type, Exception cause) {
        super(type == Token.PinType.PIN2 ? "Sign failed " + cause.getMessage() : "Auth. failed " + cause.getMessage());
    }
}
