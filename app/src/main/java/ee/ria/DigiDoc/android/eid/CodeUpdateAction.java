package ee.ria.DigiDoc.android.eid;

import android.os.Parcelable;
import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.ryanharter.auto.value.parcel.ParcelAdapter;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.parcel.ImmutableIntegerListTypeAdapter;
import ee.ria.tokenlibrary.Token;

@AutoValue
abstract class CodeUpdateAction implements Parcelable {

    private static final int PIN1_MIN_LENGTH = 4;
    private static final int PIN2_MIN_LENGTH = 5;
    private static final int PUK_MIN_LENGTH = 8;

    abstract Token.PinType pinType();
    @CodeUpdateType abstract String updateType();

    @StringRes abstract int titleRes();
    @ParcelAdapter(ImmutableIntegerListTypeAdapter.class)
    abstract ImmutableList<Integer> textRowsRes();
    @StringRes abstract int currentRes();
    @StringRes abstract int newRes();
    @StringRes abstract int repeatRes();
    @StringRes abstract int positiveButtonRes();

    abstract int currentMinLength();
    abstract int newMinLength();
    abstract int repeatMinLength();

    @StringRes abstract int successMessageRes();

    @StringRes abstract int currentMinLengthErrorRes();
    @StringRes abstract int currentInvalidErrorRes();
    @StringRes abstract int currentInvalidFinalErrorRes();
    @StringRes abstract int currentBlockedErrorRes();

    @StringRes abstract int newMinLengthErrorRes();
    @StringRes abstract int newPersonalCodeErrorRes();
    @StringRes abstract int newDateOfBirthErrorRes();
    @StringRes abstract int newTooEasyErrorRes();
    @StringRes abstract int newSameAsCurrentErrorRes();

    @StringRes abstract int repeatMismatchErrorRes();

    static CodeUpdateAction create(Token.PinType pinType, @CodeUpdateType String updateType) {
        int titleRes;
        ImmutableList<Integer> textRowsRes;
        int currentRes;
        int newRes;
        int repeatRes;
        int positiveButtonRes = updateType.equals(CodeUpdateType.EDIT)
                ? R.string.eid_home_code_update_positive_button_edit
                : R.string.eid_home_code_update_positive_button_unblock;
        int currentMinLength;
        int newMinLength;
        int repeatMinLength;
        int successMessageRes;
        int currentMinLengthErrorRes;
        int currentInvalidErrorRes;
        int currentInvalidFinalErrorRes;
        int currentBlockedErrorRes;
        int newMinLengthErrorRes;
        int newPersonalCodeErrorRes;
        int newDateOfBirthErrorRes;
        int newTooEasyErrorRes;
        int newSameAsCurrentErrorRes;
        int repeatMismatchErrorRes;

        if (pinType == Token.PinType.PIN1) {
            if (updateType.equals(CodeUpdateType.EDIT)) {
                titleRes = R.string.eid_home_code_update_title_pin1_edit;
                textRowsRes = ImmutableList.of(
                        R.string.eid_home_code_update_text_row1_pin1_edit,
                        R.string.eid_home_code_update_text_row2_pin1_edit,
                        R.string.eid_home_code_update_text_row3_pin1_edit);
                currentRes = R.string.eid_home_code_update_current_pin1_edit;
                currentMinLength = PIN1_MIN_LENGTH;
                newMinLength = PIN1_MIN_LENGTH;
                repeatMinLength = PIN1_MIN_LENGTH;
                successMessageRes = R.string.eid_home_code_update_success_pin1_edit;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin1_edit;
                currentInvalidErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin1_edit;
                currentInvalidFinalErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin1_edit_final;
                currentBlockedErrorRes =
                        R.string.eid_home_code_update_current_error_blocked_pin1_edit;
            } else {
                titleRes = R.string.eid_home_code_update_title_pin1_unblock;
                textRowsRes = ImmutableList.of(
                        R.string.eid_home_code_update_text_row1_pin1_unblock,
                        R.string.eid_home_code_update_text_row2_pin1_unblock,
                        R.string.eid_home_code_update_text_row3_pin1_unblock,
                        R.string.eid_home_code_update_text_row4_pin1_unblock);
                currentRes = R.string.eid_home_code_update_current_pin1_unblock;
                currentMinLength = PUK_MIN_LENGTH;
                newMinLength = PIN1_MIN_LENGTH;
                repeatMinLength = PIN1_MIN_LENGTH;
                successMessageRes = R.string.eid_home_code_update_success_pin1_unblock;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin1_unblock;
                currentInvalidErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin1_unblock;
                currentInvalidFinalErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin1_unblock_final;
                currentBlockedErrorRes =
                        R.string.eid_home_code_update_current_error_blocked_pin1_unblock;
            }
            newRes = R.string.eid_home_code_update_new_pin1;
            repeatRes = R.string.eid_home_code_update_repeat_pin1;
            newMinLengthErrorRes = R.string.eid_home_code_update_new_error_min_length_pin1;
            newPersonalCodeErrorRes = R.string.eid_home_code_update_new_error_personal_code_pin1;
            newDateOfBirthErrorRes = R.string.eid_home_code_update_new_error_date_of_birth_pin1;
            newTooEasyErrorRes = R.string.eid_home_code_update_new_error_too_easy_pin1;
            newSameAsCurrentErrorRes = R.string.eid_home_code_update_new_error_same_as_current_pin1;
            repeatMismatchErrorRes = R.string.eid_home_code_update_repeat_error_mismatch_pin1;
        } else if (pinType == Token.PinType.PIN2) {
            if (updateType.equals(CodeUpdateType.EDIT)) {
                titleRes = R.string.eid_home_code_update_title_pin2_edit;
                textRowsRes = ImmutableList.of(
                        R.string.eid_home_code_update_text_row1_pin2_edit,
                        R.string.eid_home_code_update_text_row2_pin2_edit,
                        R.string.eid_home_code_update_text_row3_pin2_edit);
                currentRes = R.string.eid_home_code_update_current_pin2_edit;
                currentMinLength = PIN2_MIN_LENGTH;
                newMinLength = PIN2_MIN_LENGTH;
                repeatMinLength = PIN2_MIN_LENGTH;
                successMessageRes = R.string.eid_home_code_update_success_pin2_edit;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin2_edit;
                currentInvalidErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin2_edit;
                currentInvalidFinalErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin2_edit_final;
                currentBlockedErrorRes =
                        R.string.eid_home_code_update_current_error_blocked_pin2_edit;
            } else {
                titleRes = R.string.eid_home_code_update_title_pin2_unblock;
                textRowsRes = ImmutableList.of(
                        R.string.eid_home_code_update_text_row1_pin2_unblock,
                        R.string.eid_home_code_update_text_row2_pin2_unblock,
                        R.string.eid_home_code_update_text_row3_pin2_unblock,
                        R.string.eid_home_code_update_text_row4_pin2_unblock);
                currentRes = R.string.eid_home_code_update_current_pin2_unblock;
                currentMinLength = PUK_MIN_LENGTH;
                newMinLength = PIN2_MIN_LENGTH;
                repeatMinLength = PIN2_MIN_LENGTH;
                successMessageRes = R.string.eid_home_code_update_success_pin2_unblock;
                currentMinLengthErrorRes =
                        R.string.eid_home_code_update_current_error_min_length_pin2_unblock;
                currentInvalidErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin2_unblock;
                currentInvalidFinalErrorRes =
                        R.string.eid_home_code_update_current_error_invalid_pin2_unblock_final;
                currentBlockedErrorRes =
                        R.string.eid_home_code_update_current_error_blocked_pin2_unblock;
            }
            newRes = R.string.eid_home_code_update_new_pin2;
            repeatRes = R.string.eid_home_code_update_repeat_pin2;
            newMinLengthErrorRes = R.string.eid_home_code_update_new_error_min_length_pin2;
            newPersonalCodeErrorRes = R.string.eid_home_code_update_new_error_personal_code_pin2;
            newDateOfBirthErrorRes = R.string.eid_home_code_update_new_error_date_of_birth_pin2;
            newTooEasyErrorRes = R.string.eid_home_code_update_new_error_too_easy_pin2;
            newSameAsCurrentErrorRes = R.string.eid_home_code_update_new_error_same_as_current_pin2;
            repeatMismatchErrorRes = R.string.eid_home_code_update_repeat_error_mismatch_pin2;
        } else {
            titleRes = R.string.eid_home_code_update_title_puk_edit;
            textRowsRes = ImmutableList.of(
                    R.string.eid_home_code_update_text_row1_puk_edit,
                    R.string.eid_home_code_update_text_row2_puk_edit);
            currentRes = R.string.eid_home_code_update_current_puk_edit;
            newRes = R.string.eid_home_code_update_new_puk;
            repeatRes = R.string.eid_home_code_update_repeat_puk;
            currentMinLength = PUK_MIN_LENGTH;
            newMinLength = PUK_MIN_LENGTH;
            repeatMinLength = PUK_MIN_LENGTH;
            successMessageRes = R.string.eid_home_code_update_success_puk_edit;
            currentMinLengthErrorRes =
                    R.string.eid_home_code_update_current_error_min_length_puk_edit;
            currentInvalidErrorRes = R.string.eid_home_code_update_current_error_invalid_puk_edit;
            currentInvalidFinalErrorRes =
                    R.string.eid_home_code_update_current_error_invalid_puk_edit_final;
            currentBlockedErrorRes = R.string.eid_home_code_update_current_error_blocked_puk_edit;
            newMinLengthErrorRes = R.string.eid_home_code_update_new_error_min_length_puk;
            newPersonalCodeErrorRes = R.string.eid_home_code_update_new_error_personal_code_puk;
            newDateOfBirthErrorRes = R.string.eid_home_code_update_new_error_date_of_birth_puk;
            newTooEasyErrorRes = R.string.eid_home_code_update_new_error_too_easy_puk;
            newSameAsCurrentErrorRes = R.string.eid_home_code_update_new_error_same_as_current_puk;
            repeatMismatchErrorRes = R.string.eid_home_code_update_repeat_error_mismatch_puk;
        }

        return new AutoValue_CodeUpdateAction(pinType, updateType, titleRes, textRowsRes,
                currentRes, newRes, repeatRes, positiveButtonRes, currentMinLength, newMinLength,
                repeatMinLength, successMessageRes, currentMinLengthErrorRes,
                currentInvalidErrorRes, currentInvalidFinalErrorRes, currentBlockedErrorRes,
                newMinLengthErrorRes, newPersonalCodeErrorRes, newDateOfBirthErrorRes,
                newTooEasyErrorRes, newSameAsCurrentErrorRes, repeatMismatchErrorRes);
    }
}
