package ee.ria.smartcardreader;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static ee.ria.smartcardreader.SmartCardReaderStatus.CARD_DETECTED;
import static ee.ria.smartcardreader.SmartCardReaderStatus.IDLE;
import static ee.ria.smartcardreader.SmartCardReaderStatus.READER_DETECTED;

@StringDef({IDLE, READER_DETECTED, CARD_DETECTED})
@Retention(RetentionPolicy.SOURCE)
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

