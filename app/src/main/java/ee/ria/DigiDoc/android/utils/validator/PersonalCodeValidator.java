package ee.ria.DigiDoc.android.utils.validator;

import static ee.ria.DigiDoc.android.Constants.MAXIMUM_ESTONIAN_PERSONAL_CODE_LENGTH;
import static ee.ria.DigiDoc.android.Constants.MAXIMUM_LATVIAN_PERSONAL_CODE_LENGTH;

import android.util.Log;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;

import ee.ria.DigiDoc.common.DateOfBirthUtil;
import timber.log.Timber;

public class PersonalCodeValidator {

    public static void validateEstonianPersonalCode(EditText personalCode) {
        if (personalCode.getText() == null) return;

        int codeLength = personalCode.length();
        int maxCodeLength = MAXIMUM_ESTONIAN_PERSONAL_CODE_LENGTH;

        if (codeLength > maxCodeLength) {
            personalCode.getText().delete(maxCodeLength, codeLength);
            return;
        }

        if (codeLength == maxCodeLength &&
                !isPersonalCodeValid(personalCode.getText().toString())) {
            personalCode.getText().delete(maxCodeLength - 1, codeLength);
        }
    }

    public static boolean validateEstonianPersonalCode(String personalCode) {
        return personalCode == null ||
                personalCode.length() < MAXIMUM_ESTONIAN_PERSONAL_CODE_LENGTH ||
                isPersonalCodeValid(personalCode);
    }

    public static void validateLatvianPersonalCode(EditText personalCode) {
        String regex = "^\\d{6}-\\d{5}$";

        if (personalCode.getText() != null &&
                personalCode.length() > MAXIMUM_LATVIAN_PERSONAL_CODE_LENGTH &&
                !personalCode.getText().toString().matches(regex)
        ) {
            personalCode.getText()
                    .delete(MAXIMUM_LATVIAN_PERSONAL_CODE_LENGTH, personalCode.length());
        }
    }

    public static boolean isPersonalCodeValid(String personalCode) {
        return (isPersonalCodeLengthValid(personalCode) && isBirthDateValid(personalCode) &&
                isChecksumValid(personalCode)) ||
                (isPersonalCodeLengthValid(personalCode) && isMobileIdTestCode(personalCode));
    }

    private static boolean isPersonalCodeNumeric(String personalCode) {
        return StringUtils.isNumeric(personalCode);
    }

    private static boolean isBirthDateValid(String personalCode) {

        if (!isPersonalCodeNumeric(personalCode)) {
            return false;
        }

        try {
            LocalDate dateOfBirth = DateOfBirthUtil.parseDateOfBirth(personalCode);
            return dateOfBirth.isBefore(LocalDate.now());
        } catch (DateTimeException e) {
            Timber.log(Log.ERROR, "Invalid personal code birth of date", e);
            return false;
        }
    }

    private static boolean isChecksumValid(String personalCode) {
        int sum1 = 0;
        int sum2 = 0;

        int i = 0;
        int pos1 = 1;
        int pos2 = 3;

        while (i < 10) {
            int personalCodeNumber = 0;
            try {
                personalCodeNumber = Integer.parseInt(personalCode.substring(i, i + 1));
            } catch (NumberFormatException nfe) {
                Timber.log(Log.ERROR, nfe, "Unable to parse personal code number");
            }
            sum1 += personalCodeNumber * pos1;
            sum2 += personalCodeNumber * pos2;
            pos1 = pos1 == 9 ? 1 : pos1 + 1;
            pos2 = pos2 == 9 ? 1 : pos2 + 1;

            i += 1;
        }

        int result = sum1 % 11;
        if (result >= 10) {
            result = sum2 % 11;

            if (result >= 10) {
                result = 0;
            }
        }

        int lastNumber = Character.getNumericValue(personalCode.charAt(personalCode.length() - 1));

        return lastNumber == result;
    }

    private static boolean isPersonalCodeLengthValid(String personalCode) {
        return personalCode.length() == 11;
    }

    private static boolean isMobileIdTestCode(String personalCode) {
        List<String> testNumbers = List.of(
                "14212128020",
                "14212128021",
                "14212128022",
                "14212128023",
                "14212128024",
                "14212128025",
                "14212128026",
                "14212128027",
                "38002240211",
                "14212128029"
        );

        return testNumbers.contains(personalCode);
    }
}
