package ee.ria.DigiDoc.common;

import java.time.DateTimeException;
import java.time.LocalDate;

public class DateOfBirthUtil {

    public static LocalDate parseDateOfBirth(String personalCode) throws DateTimeException {
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
                throw new IllegalArgumentException("Invalid personal code");
        }

        int year = Integer.parseInt(personalCode.substring(1, 3)) + century;
        int month = Integer.parseInt(personalCode.substring(3, 5));
        int day = Integer.parseInt(personalCode.substring(5, 7));

        return LocalDate.of(year, month, day);
    }
}
