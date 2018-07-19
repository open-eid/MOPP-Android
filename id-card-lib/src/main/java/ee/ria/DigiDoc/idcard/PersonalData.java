package ee.ria.DigiDoc.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import timber.log.Timber;

@AutoValue
public abstract class PersonalData {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM.yyyy")
            .toFormatter();

    public abstract String surname();

    public abstract String givenNames();

    public abstract String citizenship();

    @Nullable public abstract LocalDate dateOfBirth();

    public abstract String personalCode();

    public abstract String documentNumber();

    @Nullable public abstract LocalDate expiryDate();

    static PersonalData create(String surname, String givenName1, String givenName2,
                               String citizenship, String dateOfBirthString, String personalCode,
                               String documentNumber, String expiryDateString) {
        StringBuilder givenNames = new StringBuilder(givenName1);
        if (givenName2.length() > 0) {
            if (givenNames.length() > 0) {
                givenNames.append(" ");
            }
            givenNames.append(givenName2);
        }
        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(dateOfBirthString, DATE_FORMAT);
        } catch (Exception e) {
            dateOfBirth = null;
            Timber.e(e, "Could not parse date of birth %s", dateOfBirthString);
        }
        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(expiryDateString, DATE_FORMAT);
        } catch (Exception e) {
            expiryDate = null;
            Timber.e(e, "Could not parse expiry date %s", expiryDateString);
        }
        return new AutoValue_PersonalData(surname, givenNames.toString(), citizenship, dateOfBirth,
                personalCode, documentNumber, expiryDate);
    }
}
