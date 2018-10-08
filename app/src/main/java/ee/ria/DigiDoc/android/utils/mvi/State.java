package ee.ria.DigiDoc.android.utils.mvi;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static ee.ria.DigiDoc.android.utils.mvi.State.ACTIVE;
import static ee.ria.DigiDoc.android.utils.mvi.State.CLEAR;
import static ee.ria.DigiDoc.android.utils.mvi.State.IDLE;

@StringDef({IDLE, ACTIVE, CLEAR})
@Retention(RetentionPolicy.SOURCE)
public @interface State {

    String IDLE = "IDLE";
    String ACTIVE = "ACTIVE";
    String CLEAR = "CLEAR";
}
