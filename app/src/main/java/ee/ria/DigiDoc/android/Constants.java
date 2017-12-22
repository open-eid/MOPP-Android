package ee.ria.DigiDoc.android;

public final class Constants {

    public static final Object VOID = new Object();

    /*
     * Request codes for startActivityForResult
     */

    public static final int RC_SIGNATURE_CREATE_DOCUMENTS_ADD = 0;
    public static final int RC_SIGNATURE_UPDATE_DOCUMENTS_ADD = 1;

    /**
     * Name of the sub-directory in {@link android.os.Environment#DIRECTORY_DOCUMENTS documents}.
     */
    public static final String DIR_SIGNATURE_CONTAINERS = "DigiDoc";

    private Constants() {}
}
