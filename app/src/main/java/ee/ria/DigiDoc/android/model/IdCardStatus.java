package ee.ria.DigiDoc.android.model;

import android.support.annotation.StringDef;

import static ee.ria.DigiDoc.android.model.IdCardStatus.CARD_DETECTED;
import static ee.ria.DigiDoc.android.model.IdCardStatus.DATA;
import static ee.ria.DigiDoc.android.model.IdCardStatus.INITIAL;
import static ee.ria.DigiDoc.android.model.IdCardStatus.READER_DETECTED;

@StringDef({INITIAL, READER_DETECTED, CARD_DETECTED, DATA})
public @interface IdCardStatus {

    String INITIAL = "INITIAL";
    String READER_DETECTED = "READER_DETECTED";
    String CARD_DETECTED = "CARD_DETECTED";
    String DATA = "DATA";
}
