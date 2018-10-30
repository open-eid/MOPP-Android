package ee.ria.DigiDoc.idcard;

import android.util.Pair;
import android.util.SparseArray;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ee.ria.DigiDoc.smartcardreader.ApduResponseException;
import ee.ria.DigiDoc.smartcardreader.SmartCardReader;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;
import timber.log.Timber;

class ID1 implements Token {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM yyyy")
            .toFormatter();

    private final SmartCardReader reader;

    ID1(SmartCardReader reader) {
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

    private static final Map<CertificateType, Pair<Byte, Byte>> CERT_MAP = new HashMap<>();
    static {
        CERT_MAP.put(CertificateType.AUTHENTICATION, new Pair<>((byte) 0xF1, (byte) 0x01));
        CERT_MAP.put(CertificateType.SIGNING, new Pair<>((byte) 0xF2, (byte) 0x1F));
    }

    @Override
    public byte[] certificate(CertificateType type) throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x04, 0x00, new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x00}, null);
        reader.transmit(0x00, 0xA4, 0x00, 0x0C, null, null);
        reader.transmit(0x00, 0xA4, 0x01, 0x0C, new byte[] {(byte) 0xAD, CERT_MAP.get(type).first}, null);
        reader.transmit(0x00, 0xA4, 0x01, 0x0C, new byte[] {0x34, CERT_MAP.get(type).second}, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        while (true) {
            try {
                stream.write(reader.transmit(0x00, 0xB0, stream.size() >> 8, stream.size(), null, 0x00));
            } catch (ApduResponseException e) {
                if (e.sw1 == 0x6B && e.sw2 == 0x00) {
                    break;
                } else {
                    throw new SmartCardReaderException(e);
                }
            } catch (IOException e) {
                throw new SmartCardReaderException(e);
            }
        }
        return stream.toByteArray();
    }

    private static final Map<CodeType, Byte> PIN_MAP = new HashMap<>();
    static {
        PIN_MAP.put(CodeType.PIN1, (byte) 0x01);
        PIN_MAP.put(CodeType.PIN2, (byte) 0x05);
        PIN_MAP.put(CodeType.PUK, (byte) 0x02);
    }

    private static final Map<CodeType, Byte> VERIFY_PIN_MAP = new HashMap<>();
    static {
        VERIFY_PIN_MAP.put(CodeType.PIN1, (byte) 0x01);
        VERIFY_PIN_MAP.put(CodeType.PIN2, (byte) 0x85);
        VERIFY_PIN_MAP.put(CodeType.PUK, (byte) 0x02);
    }

    @Override
    public int codeRetryCounter(CodeType type) throws SmartCardReaderException {
        if (type.equals(CodeType.PIN2)) {
            reader.transmit(0x00, 0xA4, 0x04, 0x0C, new byte[] {0x51, 0x53, 0x43, 0x44, 0x20, 0x41, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F, 0x6E}, null);
        } else {
            reader.transmit(0x00, 0xA4, 0x04, 0x00, new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x00}, null);
        }
        return reader.transmit(0x00, 0xCB, 0x3F, 0xFF, new byte[] {0x4D, 0x08, 0x70, 0x06, (byte) 0xBF, (byte) 0x81, PIN_MAP.get(type), 0x02, (byte) 0xA0, (byte) 0x80}, 0x00)[13];
    }

    @Override
    public void changeCode(CodeType type, byte[] currentCode, byte[] newCode) throws SmartCardReaderException {
        verifyCode(type, currentCode);
        if (type.equals(CodeType.PIN2)) {
            reader.transmit(0x00, 0xA4, 0x04, 0x0C, new byte[] {0x51, 0x53, 0x43, 0x44, 0x20, 0x41, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F, 0x6E}, null);
        } else {
            reader.transmit(0x00, 0xA4, 0x04, 0x00, new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x00}, null);
        }
        reader.transmit(0x00, 0x24, 0x00, VERIFY_PIN_MAP.get(type), Bytes.concat(code(currentCode), code(newCode)), null);
    }

    @Override
    public void unblockAndChangeCode(byte[] pukCode, CodeType type, byte[] newCode) throws SmartCardReaderException {
        verifyCode(CodeType.PUK, pukCode);
        // block code if not yet blocked
        while (codeRetryCounter(type) != 0) {
            try {
                verifyCode(type, new byte[] {(byte) 0xFF});
            } catch (CodeVerificationException ignored) {}
        }
        if (type.equals(CodeType.PIN2)) {
            reader.transmit(0x00, 0xA4, 0x04, 0x0C, new byte[] {0x51, 0x53, 0x43, 0x44, 0x20, 0x41, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F, 0x6E}, null);
        }
        reader.transmit(0x00, 0x2C, 0x02, VERIFY_PIN_MAP.get(type), code(newCode), null);
    }

    @Override
    public byte[] calculateSignature(byte[] pin2, byte[] hash, boolean ecc) throws SmartCardReaderException {
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] pin1, byte[] data, boolean ecc) throws SmartCardReaderException {
        return new byte[0];
    }

    private void verifyCode(CodeType type, byte[] code) throws SmartCardReaderException {
        if (type.equals(CodeType.PIN2)) {
            reader.transmit(0x00, 0xA4, 0x04, 0x0C, new byte[] {0x51, 0x53, 0x43, 0x44, 0x20, 0x41, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F, 0x6E}, null);
        } else {
            reader.transmit(0x00, 0xA4, 0x04, 0x00, new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x00}, null);
        }
        try {
            reader.transmit(0x00, 0x20, 0x00, VERIFY_PIN_MAP.get(type), code(code), null);
        } catch (ApduResponseException e) {
            if (e.sw1 == 0x63 || (e.sw1 == 0x69 && e.sw2 == (byte) 0x83)) {
                throw new CodeVerificationException(type);
            }
            throw e;
        }
    }

    private static byte[] code(byte[] code) {
        byte[] padded = Arrays.copyOf(code, 12);
        Arrays.fill(padded, code.length, padded.length, (byte) 0xFF);
        return padded;
    }
}
