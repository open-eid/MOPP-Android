package ee.ria.DigiDoc.android.utils.validator;

import static ee.ria.DigiDoc.android.Constants.MAXIMUM_PERSONAL_CODE_LENGTH;

import android.util.Log;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;

import timber.log.Timber;

public class PersonalCodeValidator {

    public static void validatePersonalCode(EditText personalCode) {
        if (personalCode.getText() != null &&
                personalCode.length() >= MAXIMUM_PERSONAL_CODE_LENGTH &&
                !isPersonalCodeValid(personalCode.getText().toString())) {
            personalCode.getText()
                    .delete(MAXIMUM_PERSONAL_CODE_LENGTH - 1, personalCode.length());
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

        int firstNumber = Character.getNumericValue(personalCode.charAt(0));

        int century;
        switch (firstNumber) {
            case 1:
            case 2:
                century = 1800;
                break;
            case 3:
            case 4:
                century = 1900;
                break;
            case 5:
            case 6:
                century = 2000;
                break;
            case 7:
            case 8:
                century = 2100;
                break;
            default:
                Timber.log(Log.DEBUG, "Invalid number: " + firstNumber);
                return false;
        }

        int year = Integer.parseInt(personalCode.substring(1, 3)) + century;
        int month = Integer.parseInt(personalCode.substring(3, 5));
        int day = Integer.parseInt(personalCode.substring(5, 7));

        try {
            LocalDate.of(year, month, day);
            return true;
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
            int personalCodeNumber = Integer.parseInt(personalCode.substring(i, i + 1));
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
