package ee.ria.DigiDoc.idcard;

import android.util.SparseArray;

import com.google.common.base.Charsets;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import ee.ria.DigiDoc.smartcardreader.SmartCardReader;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;
import timber.log.Timber;

class ID1 extends EstEIDv3d5 {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM yyyy")
            .toFormatter();

    private final SmartCardReader reader;

    ID1(SmartCardReader reader) {
        super(reader);
        this.reader = reader;
    }

    @Override
    public PersonalData personalData() throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x04, 0x00, new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x00}, null);
        reader.transmit(0x00, 0xA4, 0x01, 0x0C, new byte[] {0x50, 0x00}, null);
        SparseArray<String> data = new SparseArray<>();
        for (int i = 1; i <= 8; i++) {
            reader.transmit(0x00, 0xA4, 0x01, 0x0C, new byte[] {0x50, (byte) i}, null);
            byte[] record = reader.transmit(0x00, 0xB0, 0x00, 0x00, null, 0x00);
            data.put(i, new String(record, Charsets.UTF_8).trim());
        }
        for (int i = 0; i < data.size(); i++) {
            Timber.e("PD%s - %s", data.keyAt(i), data.valueAt(i));
        }

        String surname = data.get(1);
        String givenNames = data.get(2);
        String citizenship = data.get(4);
        String dateAndPlaceOfBirthString = data.get(5);
        String personalCode = data.get(6);
        String documentNumber = data.get(7);
        String expiryDateString = data.get(8);

        String dateOfBirthString =
                dateAndPlaceOfBirthString.substring(0, dateAndPlaceOfBirthString.length() - 4);
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

        return PersonalData.create(surname, givenNames, citizenship, dateOfBirth,
                personalCode, documentNumber, expiryDate);
    }
}
