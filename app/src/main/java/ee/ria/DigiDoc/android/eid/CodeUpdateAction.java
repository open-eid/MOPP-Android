package ee.ria.DigiDoc.android.eid;

import android.os.Parcelable;
import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.R;
import ee.ria.tokenlibrary.Token;

@AutoValue
abstract class CodeUpdateAction implements Parcelable {

    private static final int PIN1_MIN_LENGTH = 4;
    private static final int PIN2_MIN_LENGTH = 5;
    private static final int PUK_MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 12;

    @StringRes abstract int titleRes();
    @StringRes abstract int textRes();
    @StringRes abstract int currentRes();
    @StringRes abstract int newRes();
    @StringRes abstract int repeatRes();
    @StringRes abstract int positiveButtonRes();

    abstract int currentMinLength();
    abstract int currentMaxLength();
    abstract int newMinLength();
    abstract int newMaxLength();
    abstract int repeatMinLength();
    abstract int repeatMaxLength();

    @StringRes abstract int currentMinLengthErrorRes();

    @StringRes abstract int newMinLengthErrorRes();
    @StringRes abstract int newPersonalCodeErrorRes();
    @StringRes abstract int newDateOfBirthErrorRes();
    @StringRes abstract int newTooEasyErrorRes();

    @StringRes abstract int repeatMismatchErrorRes();

    static CodeUpdateAction create(Token.PinType pinType, @CodeUpdateType String updateType) {
        int titleRes;
        int textRes;
        int currentRes;
        int newRes;
        int repeatRes;
        int positiveButtonRes = updateType.equals(CodeUpdateType.EDIT)
                ? R.string.eid_home_code_update_positive_button_edit
                : R.string.eid_home_code_update_positive_button_unblock;
        int currentMinLength;
        int newMinLength;
        int repeatMinLength;
        int currentMinLengthErrorRes;
        int newMinLengthErrorRes;
        int newPersonalCodeErrorRes;
        int newDateOfBirthErrorRes;
        int newTooEasyErrorRes;
        int repeatMismatchErrorRes;

        if (pinType == Token.PinType.PIN1) {
            if (updateType.equals(CodeUpdateType.EDIT)) {
                titleRes = R.string.eid_home_code_update_title_pin1_edit;
                textRes = R.string.eid_home_code_update_text_pin1_edit;
                currentRes = R.string.eid_home_code_update_current_pin1_edit;
                currentMinLength = PIN1_MIN_LENGTH;
                newMinLength = PIN1_MIN_LENGTH;
                repeatMinLength = PIN1_MIN_LENGTH;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin1_edit;
            } else {
                titleRes = R.string.eid_home_code_update_title_pin1_unblock;
                textRes = R.string.eid_home_code_update_text_pin1_unblock;
                currentRes = R.string.eid_home_code_update_current_pin1_unblock;
                currentMinLength = PUK_MIN_LENGTH;
                newMinLength = PIN1_MIN_LENGTH;
                repeatMinLength = PIN1_MIN_LENGTH;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin1_unblock;
            }
            newRes = R.string.eid_home_code_update_new_pin1;
            repeatRes = R.string.eid_home_code_update_repeat_pin1;
            newMinLengthErrorRes = R.string.eid_home_code_update_new_error_min_length_pin1;
            newPersonalCodeErrorRes = R.string.eid_home_code_update_new_error_personal_code_pin1;
            newDateOfBirthErrorRes = R.string.eid_home_code_update_new_error_date_of_birth_pin1;
            newTooEasyErrorRes = R.string.eid_home_code_update_new_error_too_easy_pin1;
            repeatMismatchErrorRes = R.string.eid_home_code_update_repeat_error_mismatch_pin1;
        } else if (pinType == Token.PinType.PIN2) {
            if (updateType.equals(CodeUpdateType.EDIT)) {
                titleRes = R.string.eid_home_code_update_title_pin2_edit;
                textRes = R.string.eid_home_code_update_text_pin2_edit;
                currentRes = R.string.eid_home_code_update_current_pin2_edit;
                currentMinLength = PIN2_MIN_LENGTH;
                newMinLength = PIN2_MIN_LENGTH;
                repeatMinLength = PIN2_MIN_LENGTH;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin2_edit;
            } else {
                titleRes = R.string.eid_home_code_update_title_pin2_unblock;
                textRes = R.string.eid_home_code_update_text_pin2_unblock;
                currentRes = R.string.eid_home_code_update_current_pin2_unblock;
                currentMinLength = PUK_MIN_LENGTH;
                newMinLength = PIN2_MIN_LENGTH;
                repeatMinLength = PIN2_MIN_LENGTH;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin2_unblock;
            }
            newRes = R.string.eid_home_code_update_new_pin2;
            repeatRes = R.string.eid_home_code_update_repeat_pin2;
            newMinLengthErrorRes = R.string.eid_home_code_update_new_error_min_length_pin2;
            newPersonalCodeErrorRes = R.string.eid_home_code_update_new_error_personal_code_pin2;
            newDateOfBirthErrorRes = R.string.eid_home_code_update_new_error_date_of_birth_pin2;
            newTooEasyErrorRes = R.string.eid_home_code_update_new_error_too_easy_pin2;
            repeatMismatchErrorRes = R.string.eid_home_code_update_repeat_error_mismatch_pin2;
        } else {
            titleRes = R.string.eid_home_code_update_title_puk_edit;
            textRes = R.string.eid_home_code_update_text_puk_edit;
            currentRes = R.string.eid_home_code_update_current_puk_edit;
            newRes = R.string.eid_home_code_update_new_puk;
            repeatRes = R.string.eid_home_code_update_repeat_puk;
            currentMinLength = PUK_MIN_LENGTH;
            newMinLength = PUK_MIN_LENGTH;
            repeatMinLength = PUK_MIN_LENGTH;
            currentMinLengthErrorRes =
                    R.string.eid_home_code_update_current_error_min_length_puk_edit;
            newMinLengthErrorRes = R.string.eid_home_code_update_new_error_min_length_puk;
            newPersonalCodeErrorRes = R.string.eid_home_code_update_new_error_personal_code_puk;
            newDateOfBirthErrorRes = R.string.eid_home_code_update_new_error_date_of_birth_puk;
            newTooEasyErrorRes = R.string.eid_home_code_update_new_error_too_easy_puk;
            repeatMismatchErrorRes = R.string.eid_home_code_update_repeat_error_mismatch_puk;
        }

        return new AutoValue_CodeUpdateAction(titleRes, textRes, currentRes, newRes, repeatRes,
                positiveButtonRes, currentMinLength, MAX_LENGTH, newMinLength, MAX_LENGTH,
                repeatMinLength, MAX_LENGTH, currentMinLengthErrorRes, newMinLengthErrorRes,
                newPersonalCodeErrorRes, newDateOfBirthErrorRes, newTooEasyErrorRes,
                repeatMismatchErrorRes);
    }
}
