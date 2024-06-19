package ee.ria.DigiDoc.android;

public final class Constants {

    public static final Object VOID = new Object();

    /*
     * Relying Party
     */
    public static final String SETTINGS_DEFAULT_RELYING_PARTY_UUID = "00000000-0000-0000-0000-000000000000";

    /*
     * Request codes for startActivityForResult
     */

    public static final int RC_SIGNATURE_CREATE_DOCUMENTS_ADD = 0;
    public static final int RC_SIGNATURE_UPDATE_DOCUMENTS_ADD = 1;
    public static final int RC_CRYPTO_CREATE_INITIAL = 2;
    public static final int RC_CRYPTO_CREATE_DATA_FILE_ADD = 3;
    public static final int SAVE_FILE = 4;
    public static final int RC_TSA_CERT_ADD = 5;
    public static final int RC_SIVA_CERT_ADD = 6;

    /**
     * Sub-directory name in {@link android.content.Context#getFilesDir() files dir} for signature
     * containers.
     */
    public static final String DIR_SIGNATURE_CONTAINERS = "signed_containers";
    public static final String DIR_INTERNAL_FILES = "internal_files";
    public static final String DIR_EXTERNALLY_OPENED_FILES = "external_files";

    public static final String SIGNATURE_CONTAINER_EXT = "asice";

    /**
     * Personal code
     */
    public static final int MAXIMUM_PERSONAL_CODE_LENGTH = 11;

    /**
     * CAN number
     */

    public static final int CAN_LENGTH = 6;

    /**
     * View type to identify screen when restarting app
     */
    public static final String VIEW_TYPE = "VIEW_TYPE";

    private Constants() {}
}
