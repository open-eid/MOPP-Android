package ee.ria.DigiDoc.idcard;

import android.util.Log;
import android.util.SparseArray;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

import timber.log.Timber;

class ID1PersonalDataParser {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM yyyy")
            .toFormatter();
    private static final int SURNAME_POS = 1;
    private static final int GIVEN_NAMES_POS = 2;
    private static final int GENDER_POS = 3;
    private static final int CITIZENSHIP_POS = 4;
    private static final int DATE_AND_PLACE_OF_BIRTH_POS = 5;
    private static final int PERSONAL_CODE_POS = 6;
    private static final int DOCUMENT_NUMBER_POS = 7;
    private static final int EXPIRY_DATE_POS = 8;

    private ID1PersonalDataParser() {}

    static PersonalData parse(SparseArray<String> data) {
        String surname = data.get(SURNAME_POS);
        String givenNames = data.get(GIVEN_NAMES_POS);
        String citizenship = data.get(CITIZENSHIP_POS);
        String dateAndPlaceOfBirthString = data.get(DATE_AND_PLACE_OF_BIRTH_POS);
        String personalCode = data.get(PERSONAL_CODE_POS);
        String documentNumber = data.get(DOCUMENT_NUMBER_POS);
        String expiryDateString = data.get(EXPIRY_DATE_POS);

        if (data.get(GENDER_POS).isEmpty() && !personalCode.isEmpty()) {
            data.set(GENDER_POS, parseDigiIdGender(personalCode));
        }

        LocalDate dateOfBirth;

        if (!dateAndPlaceOfBirthString.isEmpty()) {
            dateOfBirth = parseDateOfBirth(dateAndPlaceOfBirthString);
        } else if (!personalCode.isEmpty()) {
            dateOfBirth = parseDigiIdDateOfBirth(personalCode);
        } else {
            throw new IllegalArgumentException("Personal code not found");
        }

        LocalDate expiryDate = parseExpiryDate(expiryDateString);

        return PersonalData.create(surname, givenNames, citizenship, dateOfBirth,
                personalCode, documentNumber, expiryDate);
    }

    private static LocalDate parseExpiryDate(String expiryDateString) {
        try {
            return LocalDate.parse(expiryDateString, DATE_FORMAT);
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Could not parse expiry date %s", expiryDateString);
            return null;
        }
    }

    private static LocalDate parseDateOfBirth(String dateAndPlaceOfBirthString) {
        if (dateAndPlaceOfBirthString == null) {
            Timber.log(Log.ERROR, "Could not parse date of birth: no data");
            return null;
        }
        try {
            String dateOfBirthString = dateAndPlaceOfBirthString
                    .substring(0, dateAndPlaceOfBirthString.length() - 4);

            return LocalDate.parse(dateOfBirthString, DATE_FORMAT);
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Could not parse date of birth %s", dateAndPlaceOfBirthString);
            return null;
        }
    }

    private static String parseDigiIdGender(String personalCode) {
        int genderNumber = Character.getNumericValue(personalCode.charAt(0));
        List<Integer> males = List.of(1, 3, 5, 7);
        List<Integer> females = List.of(2, 4, 6, 8);

        if (males.contains(genderNumber)) {
            return "M";
        } else if (females.contains(genderNumber)) {
            return "F";
        }

        throw new IllegalArgumentException("Invalid personal code");
    }

    private static LocalDate parseDigiIdDateOfBirth(String personalCode) {
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
                throw new IllegalArgumentException("Invalid personal code");
        }

        int year = Integer.parseInt(personalCode.substring(1, 3)) + century;
        int month = Integer.parseInt(personalCode.substring(3, 5));
        int day = Integer.parseInt(personalCode.substring(5, 7));

        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            Timber.log(Log.ERROR, "Invalid personal code birth of date", e);
            throw new IllegalArgumentException("Invalid personal code");
        }
    }

}
