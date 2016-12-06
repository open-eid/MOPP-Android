package ee.ria.token.tokenservice.token;


public class SecureOperationOverUnsecureChannelException extends TokenException {
    public SecureOperationOverUnsecureChannelException(String message) {
        super(message);
    }
}
