package ee.ria.DigiDoc.android.model;

import android.support.annotation.StringDef;

import static ee.ria.DigiDoc.android.model.EIDType.DIGI_ID;
import static ee.ria.DigiDoc.android.model.EIDType.ID_CARD;
import static ee.ria.DigiDoc.android.model.EIDType.MOBILE_ID;

@StringDef({ID_CARD, DIGI_ID, MOBILE_ID})
public @interface EIDType {

    String ID_CARD = "ID_CARD";
    String DIGI_ID = "DIGI_ID";
    String MOBILE_ID = "MOBILE_ID";
}
