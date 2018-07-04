package ee.ria.scardcomlibrary;

public class SmartCardReaderException extends Exception {

    public SmartCardReaderException(String message) {
        super(message);
    }

    public SmartCardReaderException(Throwable cause) {
        super(cause);
    }
}
