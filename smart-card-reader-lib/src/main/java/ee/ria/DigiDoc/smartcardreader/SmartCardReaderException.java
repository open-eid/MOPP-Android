package ee.ria.DigiDoc.smartcardreader;

public class SmartCardReaderException extends Exception {

    public SmartCardReaderException(String message) {
        super(message);
    }

    public SmartCardReaderException(Throwable cause) {
        super(cause);
    }

    public SmartCardReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
