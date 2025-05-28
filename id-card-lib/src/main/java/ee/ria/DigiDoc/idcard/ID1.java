package ee.ria.DigiDoc.idcard;

import static com.google.common.primitives.Bytes.concat;

import android.util.SparseArray;

import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ee.ria.DigiDoc.smartcardreader.ApduResponseException;
import ee.ria.DigiDoc.smartcardreader.SmartCardReader;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;

class ID1 implements Token {
    private static final Map<CertificateType, byte[]> CERT_MAP = new HashMap<>();
    static {
        CERT_MAP.put(CertificateType.AUTHENTICATION, new byte[] {(byte) 0xAD, (byte) 0xF1, 0x34, 0x01});
        CERT_MAP.put(CertificateType.SIGNING, new byte[] {(byte) 0xAD, (byte) 0xF2, 0x34, (byte) 0x1F});
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

    private final SmartCardReader reader;

    ID1(SmartCardReader reader) {
        this.reader = reader;
    }

    @Override
    public PersonalData personalData() throws SmartCardReaderException {
        selectMainAid();
        reader.transmit(0x00, 0xA4, 0x01, 0x0C, new byte[] {0x50, 0x00}, null);
        SparseArray<String> data = new SparseArray<>();
        for (int i = 1; i <= 8; i++) {
            reader.transmit(0x00, 0xA4, 0x02, 0x0C, new byte[] {0x50, (byte) i}, null);
            byte[] record = reader.transmit(0x00, 0xB0, 0x00, 0x00, null, 0x00);
            data.put(i, new String(record, StandardCharsets.UTF_8).trim());
        }
        return ID1PersonalDataParser.parse(data);
    }

    @Override
    public byte[] certificate(CertificateType type) throws SmartCardReaderException {
        selectMainAid();
        reader.transmit(0x00, 0xA4, 0x09, 0x0C, CERT_MAP.get(type), null);

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

    @Override
    public int codeRetryCounter(CodeType type) throws SmartCardReaderException {
        if (type.equals(CodeType.PIN2)) {
            selectQSCDAid();
        } else {
            selectMainAid();
        }
        return reader.transmit(0x00, 0xCB, 0x3F, 0xFF, new byte[] {0x4D, 0x08, 0x70, 0x06, (byte) 0xBF, (byte) 0x81, Objects.requireNonNull(PIN_MAP.get(type)), 0x02, (byte) 0xA0, (byte) 0x80}, 0x00)[13];
    }

    @Override
    public void changeCode(CodeType type, byte[] currentCode, byte[] newCode) throws SmartCardReaderException {
        if (type.equals(CodeType.PIN2)) {
            selectQSCDAid();
        } else {
            selectMainAid();
        }
        verifyCode(type, currentCode);
        reader.transmit(0x00, 0x24, 0x00, Objects.requireNonNull(VERIFY_PIN_MAP.get(type)), Bytes.concat(code(currentCode), code(newCode)), null);
    }

    @Override
    public void unblockAndChangeCode(byte[] pukCode, CodeType type, byte[] newCode) throws SmartCardReaderException {
        verifyCode(CodeType.PUK, pukCode);
        if (type.equals(CodeType.PIN2)) {
            selectQSCDAid();
        }
        reader.transmit(0x00, 0x2C, 0x02, Objects.requireNonNull(VERIFY_PIN_MAP.get(type)), code(newCode), null);
    }

    @Override
    public byte[] calculateSignature(byte[] pin2, byte[] hash, boolean ecc) throws SmartCardReaderException {
        selectQSCDAid();
        if (null != pin2 && pin2.length > 0) {
            verifyCode(CodeType.PIN2, pin2);
            Arrays.fill(pin2, (byte) 0);
        } else {
            throw new IllegalStateException("PIN2 is null or empty");
        }
        reader.transmit(0x00, 0x22, 0x41, 0xB6, new byte[] {(byte) 0x80, 0x04, (byte) 0xFF, 0x15, 0x08, 0x00, (byte) 0x84, 0x01, (byte) 0x9F}, null);
        return reader.transmit(0x00, 0x2A, 0x9E, 0x9A, padWithZeroes(hash), 0x00);
    }

    @Override
    public byte[] decrypt(byte[] pin1, byte[] data, boolean ecc) throws SmartCardReaderException {
        selectOberthurAid();
        byte[] prefix = new byte[] {0x00};
        verifyCode(CodeType.PIN1, pin1);
        reader.transmit(0x00, 0x22, 0x41, 0xB8, new byte[] {(byte) 0x80, 0x04, (byte) 0xFF, 0x30, 0x04, 0x00, (byte) 0x84, 0x01, (byte) 0x81}, null);
        return reader.transmit(0x00, 0x2A, 0x80, 0x86, concat(prefix, data), 0x00);
    }

    private void verifyCode(CodeType type, byte[] code) throws SmartCardReaderException {
        try {
            reader.transmit(0x00, 0x20, 0x00, Objects.requireNonNull(VERIFY_PIN_MAP.get(type)), code(code), null);
        } catch (ApduResponseException e) {
            if (e.sw1 == 0x63 || (e.sw1 == 0x69 && e.sw2 == (byte) 0x83)) {
                throw new CodeVerificationException(type);
            }
            throw e;
        }
    }

    private void selectMainAid() throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x04, 0x00, new byte[] {(byte) 0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x00}, null);
    }

    private void selectQSCDAid() throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x04, 0x0C, new byte[] {0x51, 0x53, 0x43, 0x44, 0x20, 0x41, 0x70, 0x70, 0x6C, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6F, 0x6E}, null);
    }

    private void selectOberthurAid() throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x04, 0x0C, new byte[] {(byte) 0xE8, 0x28, (byte) 0xBD, 0x08, 0x0F, (byte) 0xF2, 0x50, 0x4F, 0x54, 0x20, 0x41, 0x57, 0x50}, null);
    }

    private static byte[] code(byte[] code) {
        byte[] padded = Arrays.copyOf(code, 12);
        Arrays.fill(padded, code.length, padded.length, (byte) 0xFF);
        return padded;
    }

    /**
     * ID1 only has ECC keys so we don't need to pad it as we do RSA hashes,
     * but we need to pad hashes that are smaller than the key size with zeroes in front to resize
     * them to 48bytes in length because of API restrictions on the chip
     *
     * @param hash that needs to be signed
     * @return zero padded hash with 48 byte length or same hash if it's longer than 48 bytes
     * @throws IdCardException when padding the hash fails
     */
    private static byte[] padWithZeroes(byte[] hash) throws IdCardException {
        if (hash.length >= 48) {
            return hash;
        }
        try (ByteArrayOutputStream toSign = new ByteArrayOutputStream()) {
            toSign.write(new byte[48 - hash.length]);
            toSign.write(hash);
            return toSign.toByteArray();
        } catch (IOException e) {
            throw new IdCardException("Failed to Add padding to hash", e);
        }
    }
}
