package ee.ria.DigiDoc.android.model.mobileid;

/**
 * Exception thrown by Mobile-ID service that contains message suitable for showing to the user.
 */
public final class MobileIdMessageException extends Exception {

    public MobileIdMessageException(String message) {
        super(message);
    }
}
