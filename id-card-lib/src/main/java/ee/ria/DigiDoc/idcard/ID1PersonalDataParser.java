package ee.ria.DigiDoc.idcard;

import android.util.SparseArray;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import timber.log.Timber;

class ID1PersonalDataParser {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM yyyy")
            .toFormatter();
    private static final int SURNAME_POS = 1;
    private static final int GIVEN_NAMES_POS = 2;
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

        LocalDate dateOfBirth = parseDateOfBirth(dateAndPlaceOfBirthString);
        LocalDate expiryDate = parseExpiryDate(expiryDateString);

        return PersonalData.create(surname, givenNames, citizenship, dateOfBirth,
                personalCode, documentNumber, expiryDate);
    }

    private static LocalDate parseExpiryDate(String expiryDateString) {
        try {
            return LocalDate.parse(expiryDateString, DATE_FORMAT);
        } catch (Exception e) {
            Timber.e(e, "Could not parse expiry date %s", expiryDateString);
            return null;
        }
    }

    private static LocalDate parseDateOfBirth(String dateAndPlaceOfBirthString) {
        if (dateAndPlaceOfBirthString == null) {
            Timber.e("Could not parse date of birth: no data");
            return null;
        }
        try {
            String dateOfBirthString = dateAndPlaceOfBirthString
                    .substring(0, dateAndPlaceOfBirthString.length() - 4);

            return LocalDate.parse(dateOfBirthString, DATE_FORMAT);
        } catch (Exception e) {
            Timber.e(e, "Could not parse date of birth %s", dateAndPlaceOfBirthString);
            return null;
        }
    }

}
