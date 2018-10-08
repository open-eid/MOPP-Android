package ee.ria.DigiDoc.smartcardreader;

public enum SmartCardReaderStatus {

    /**
     * No readers detected.
     */
    IDLE,

    /**
     * Supported reader detected and connected.
     */
    READER_DETECTED,

    /**
     * Card inside reader detected, ready to receive transmissions.
     */
    CARD_DETECTED
}
