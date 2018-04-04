package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.StringDef;

import static ee.ria.DigiDoc.android.model.idcard.IdCardStatus.CARD_DETECTED;
import static ee.ria.DigiDoc.android.model.idcard.IdCardStatus.INITIAL;
import static ee.ria.DigiDoc.android.model.idcard.IdCardStatus.READER_DETECTED;

@StringDef({INITIAL, READER_DETECTED, CARD_DETECTED})
public @interface IdCardStatus {

    String INITIAL = "INITIAL";
    String READER_DETECTED = "READER_DETECTED";
    String CARD_DETECTED = "CARD_DETECTED";
}
