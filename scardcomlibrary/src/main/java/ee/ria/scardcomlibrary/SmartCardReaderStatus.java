package ee.ria.scardcomlibrary;

import android.support.annotation.StringDef;

import static ee.ria.scardcomlibrary.SmartCardReaderStatus.CARD_DETECTED;
import static ee.ria.scardcomlibrary.SmartCardReaderStatus.IDLE;
import static ee.ria.scardcomlibrary.SmartCardReaderStatus.READER_DETECTED;

@StringDef({IDLE, READER_DETECTED, CARD_DETECTED})
public @interface SmartCardReaderStatus {

    /**
     * No readers detected.
     */
    String IDLE = "IDLE";

    /**
     * Supported reader detected and connected.
     */
    String READER_DETECTED = "READER_DETECTED";

    /**
     * Card inside reader detected, ready to receive transmissions.
     */
    String CARD_DETECTED = "CARD_DETECTED";
}

