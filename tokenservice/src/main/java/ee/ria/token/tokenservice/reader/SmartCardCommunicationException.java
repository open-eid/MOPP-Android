package ee.ria.token.tokenservice.reader;


public class SmartCardCommunicationException extends RuntimeException {
    public SmartCardCommunicationException(String message) {
        super(message);
    }

    public SmartCardCommunicationException(Exception cause) {
        super(cause);
    }
}
