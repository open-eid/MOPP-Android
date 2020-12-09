package ee.ria.DigiDoc.android;

public final class Constants {

    public static final Object VOID = new Object();

    /*
     * Request codes for startActivityForResult
     */

    public static final int RC_SIGNATURE_CREATE_DOCUMENTS_ADD = 0;
    public static final int RC_SIGNATURE_UPDATE_DOCUMENTS_ADD = 1;
    public static final int RC_CRYPTO_CREATE_INITIAL = 2;
    public static final int RC_CRYPTO_CREATE_DATA_FILE_ADD = 3;
    public static final int SAVE_FILE = 4;

    /**
     * Sub-directory name in {@link android.content.Context#getFilesDir() files dir} for signature
     * containers.
     */
    public static final String DIR_SIGNATURE_CONTAINERS = "signed_containers";

    public static final String SIGNATURE_CONTAINER_EXT = "asice";

    private Constants() {}
}
