/*
 * Copyright 2017 - 2023 Riigi Infosüsteemi Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.idcard;

import android.util.Log;
import android.util.SparseArray;

import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import ee.ria.DigiDoc.smartcardreader.ApduResponseException;
import ee.ria.DigiDoc.smartcardreader.SmartCardReader;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;
import timber.log.Timber;

import static com.google.common.primitives.Bytes.concat;
import static ee.ria.DigiDoc.idcard.AlgorithmUtils.addPadding;

abstract class EstEIDToken implements Token {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM.yyyy")
            .toFormatter();

    private final SmartCardReader reader;

    EstEIDToken(SmartCardReader reader) {
        this.reader = reader;
    }

    @Override
    public PersonalData personalData() throws SmartCardReaderException {
        selectMasterFile();
        selectCatalogue();
        reader.transmit(0x00, 0xA4, 0x02, 0x04, new byte[] {0x50, 0x44}, null);

        SparseArray<String> data = new SparseArray<>();
        for (int i = 1; i <= 9; i++) {
            byte[] record = reader.transmit(0x00, 0xB2, i, 0x04, null, 0x00);
            try {
                data.put(i, new String(record, "Windows-1252").trim());
            } catch (UnsupportedEncodingException e) {
                throw new SmartCardReaderException(e);
            }
        }

        String surname = data.get(1);
        String givenName1 = data.get(2);
        String givenName2 = data.get(3);
        String citizenship = data.get(5);
        String dateOfBirthString = data.get(6);
        String personalCode = data.get(7);
        String documentNumber = data.get(8);
        String expiryDateString = data.get(9);

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
            Timber.log(Log.ERROR, e, "Could not parse date of birth %s", dateOfBirthString);
        }
        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(expiryDateString, DATE_FORMAT);
        } catch (Exception e) {
            expiryDate = null;
            Timber.log(Log.ERROR, e, "Could not parse expiry date %s", expiryDateString);
        }

        return PersonalData.create(surname, givenNames.toString(), citizenship, dateOfBirth,
                personalCode, documentNumber, expiryDate);
    }

    @Override
    public void changeCode(CodeType type, byte[] currentCode, byte[] newCode)
            throws SmartCardReaderException {
        verifyCode(type, currentCode);
        reader.transmit(0x00, 0x24, 0x00, type.value, concat(currentCode, newCode), null);
    }

    @Override
    public void unblockAndChangeCode(byte[] pukCode, CodeType type, byte[] newCode)
            throws SmartCardReaderException {
        verifyCode(CodeType.PUK, pukCode);
        // block code if not yet blocked
        byte i = 0;
        while (codeRetryCounter(type) != 0) {
            try {
                verifyCode(type, concat(new byte[newCode.length - 1], new byte[] {i++}));
            } catch (CodeVerificationException ignored) {}
        }
        reader.transmit(0x00, 0x2C, 0x00, type.value, concat(pukCode, newCode), null);
    }

    @Override
    public int codeRetryCounter(CodeType type) throws SmartCardReaderException {
        selectMasterFile();
        reader.transmit(0x00, 0xA4, 0x02, 0x0C, new byte[] {0x00, 0x16}, null);
        return reader.transmit(0x00, 0xB2, type.retryValue, 0x04, null, 0x00)[5];
    }

    @Override
    public byte[] certificate(CertificateType type) throws SmartCardReaderException {
        selectMasterFile();
        selectCatalogue();
        reader.transmit(0x00, 0xA4, 0x02, 0x04, new byte[] {type.value, (byte) 0xCE}, null);
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] result = reader.transmit(0x00, 0xB0, 0x00, 0x00, null, 0x00);
            stream.write(result);
            int remaining = 4
                    + Integer.parseInt(Hex.toHexString(new byte[] {result[2], result[3]}), 16)
                    - 256;
            int i = 1;
            while (remaining >= 256) {
                stream.write(reader.transmit(0x00, 0xB0, i++, 0x00, null, 0x00));
                remaining -= 256;
            }
            stream.write(reader.transmit(0x00, 0xB0, i, 0x00, null, remaining));
            return stream.toByteArray();
        } catch (IOException | NumberFormatException e) {
            throw new SmartCardReaderException(e);
        }
    }

    @Override
    public byte[] calculateSignature(byte[] pin2, byte[] hash, boolean ecc)
            throws SmartCardReaderException {
        selectMasterFile();
        selectCatalogue();
        selectSecurityEnvironment((byte) 0x01);
        // TODO select keys
        verifyCode(CodeType.PIN2, pin2);
        return reader.transmit(0x00, 0x2A, 0x9E, 0x9A, addPadding(hash, ecc), null);
    }

    @Override
    public byte[] decrypt(byte[] pin1, byte[] data, boolean ecc) throws SmartCardReaderException {
        byte[] prefix = ecc
                ? new byte[] {(byte) 0xA6, 0x66, 0x7F, 0x49, 0x63, (byte) 0x86, 0x61}
                : new byte[] {0x00};
        selectMasterFile();
        selectCatalogue();
        selectSecurityEnvironment((byte) 0x06);
        // TODO select keys
        verifyCode(CodeType.PIN1, pin1);
        return reader.transmit(0x00, 0x2A, 0x80, 0x86, concat(prefix, data), 0x00);
    }

    abstract void selectMasterFile() throws SmartCardReaderException;

    abstract void selectCatalogue() throws SmartCardReaderException;

    abstract void selectSecurityEnvironment(byte operation) throws SmartCardReaderException;

    private void verifyCode(CodeType type, byte[] code) throws SmartCardReaderException {
        try {
            reader.transmit(0x00, 0x20, 0x00, type.value, code, null);
        } catch (ApduResponseException e) {
            if (e.sw1 == 0x63 || (e.sw1 == 0x69 && e.sw2 == (byte) 0x83)) {
                throw new CodeVerificationException(type);
            }
            throw e;
        }
    }
}
