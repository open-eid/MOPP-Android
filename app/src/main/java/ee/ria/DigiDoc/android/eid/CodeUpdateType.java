package ee.ria.DigiDoc.android.eid;

import android.support.annotation.StringDef;

import static ee.ria.DigiDoc.android.eid.CodeUpdateType.EDIT;
import static ee.ria.DigiDoc.android.eid.CodeUpdateType.UNBLOCK;

@StringDef({EDIT, UNBLOCK})
@interface CodeUpdateType {

    String EDIT = "EDIT";
    String UNBLOCK = "UNBLOCK";
}
