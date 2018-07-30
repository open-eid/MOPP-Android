package ee.ria.DigiDoc.android.eid;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static ee.ria.DigiDoc.android.eid.CodeUpdateType.EDIT;
import static ee.ria.DigiDoc.android.eid.CodeUpdateType.UNBLOCK;

@StringDef({EDIT, UNBLOCK})
@Retention(RetentionPolicy.SOURCE)
@interface CodeUpdateType {

    String EDIT = "EDIT";
    String UNBLOCK = "UNBLOCK";
}
