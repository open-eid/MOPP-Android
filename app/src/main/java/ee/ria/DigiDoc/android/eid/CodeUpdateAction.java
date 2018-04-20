package ee.ria.DigiDoc.android.eid;

import android.os.Parcelable;
import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.R;
import ee.ria.tokenlibrary.Token;

@AutoValue
abstract class CodeUpdateAction implements Parcelable {

    @StringRes abstract int titleRes();

    @StringRes abstract int textRes();

    @StringRes abstract int currentRes();

    @StringRes abstract int newRes();

    @StringRes abstract int repeatRes();

    @StringRes abstract int positiveButtonRes();

    static CodeUpdateAction create(Token.PinType pinType, @CodeUpdateType String updateType) {
        int titleRes;
        int textRes;
        int currentRes;
        int newRes;
        int repeatRes;
        int positiveButtonRes = updateType.equals(CodeUpdateType.EDIT)
                ? R.string.eid_home_code_update_positive_button_edit
                : R.string.eid_home_code_update_positive_button_unblock;

        if (pinType == Token.PinType.PIN1) {
            if (updateType.equals(CodeUpdateType.EDIT)) {
                titleRes = R.string.eid_home_code_update_title_pin1_edit;
                textRes = R.string.eid_home_code_update_text_pin1_edit;
                currentRes = R.string.eid_home_code_update_current_pin1_edit;
            } else {
                titleRes = R.string.eid_home_code_update_title_pin1_unblock;
                textRes = R.string.eid_home_code_update_text_pin1_unblock;
                currentRes = R.string.eid_home_code_update_current_pin1_unblock;
            }
            newRes = R.string.eid_home_code_update_new_pin1;
            repeatRes = R.string.eid_home_code_update_repeat_pin1;
        } else if (pinType == Token.PinType.PIN2) {
            if (updateType.equals(CodeUpdateType.EDIT)) {
                titleRes = R.string.eid_home_code_update_title_pin2_edit;
                textRes = R.string.eid_home_code_update_text_pin2_edit;
                currentRes = R.string.eid_home_code_update_current_pin2_edit;
            } else {
                titleRes = R.string.eid_home_code_update_title_pin2_unblock;
                textRes = R.string.eid_home_code_update_text_pin2_unblock;
                currentRes = R.string.eid_home_code_update_current_pin2_unblock;
            }
            newRes = R.string.eid_home_code_update_new_pin2;
            repeatRes = R.string.eid_home_code_update_repeat_pin2;
        } else {
            titleRes = R.string.eid_home_code_update_title_puk_edit;
            textRes = R.string.eid_home_code_update_text_puk_edit;
            currentRes = R.string.eid_home_code_update_current_puk_edit;
            newRes = R.string.eid_home_code_update_new_puk;
            repeatRes = R.string.eid_home_code_update_repeat_puk;
        }

        return new AutoValue_CodeUpdateAction(titleRes, textRes, currentRes, newRes, repeatRes,
                positiveButtonRes);
    }
}
