package ee.ria.smartcardreader;

public class SmartCardReaderException extends Exception {

    public SmartCardReaderException(String message) {
        super(message);
    }

    public SmartCardReaderException(Throwable cause) {
        super(cause);
    }
}
