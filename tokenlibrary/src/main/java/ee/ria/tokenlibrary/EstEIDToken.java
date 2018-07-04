/*
 * Copyright 2017 Riigi Infosüsteemide Amet
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

package ee.ria.tokenlibrary;

import android.util.SparseArray;

import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import ee.ria.scardcomlibrary.SmartCardReader;
import ee.ria.scardcomlibrary.SmartCardReaderException;
import ee.ria.tokenlibrary.exception.PinVerificationException;

abstract class EstEIDToken implements Token {

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

        return PersonalData.create(data.get(1), data.get(2), data.get(3), data.get(5), data.get(6),
                data.get(7), data.get(8), data.get(9));
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
        } catch (IOException e) {
            throw new SmartCardReaderException(e);
        }
    }

    void verifyPin(PinType type, byte[] pin) throws PinVerificationException {
        try {
            reader.transmit(0x00, 0x20, 0x00, type.value, pin, null);
        } catch (SmartCardReaderException e) {
            throw new PinVerificationException(type);
        }
    }

    void blockPin(PinType pinType, int newPinLength) throws SmartCardReaderException {
        byte retries = readRetryCounter(pinType);
        for (int i = 0; i <= retries; i++) {
            reader.transmit(0x00, 0x20, 0x00, pinType.value, new byte[newPinLength], null);
        }
    }

    @Override
    public byte readRetryCounter(PinType pinType) throws SmartCardReaderException {
        selectMasterFile();
        reader.transmit(0x00, 0xA4, 0x02, 0x0C, new byte[] {0x00, 0x16}, null);
        byte[] bytes = reader.transmit(0x00, 0xB2, pinType.retryValue, 0x04, null, 0x00);
        return bytes[5];
    }

    @Override
    public byte[] decrypt(byte[] pin1, byte[] data) throws SmartCardReaderException {
        selectMasterFile();
        selectCatalogue();

        reader.transmit(0x00, 0x22, 0xF3, 0x06, null, null);

        reader.transmit(0x00, 0x22, 0x41, 0xA4,
                new byte[] {(byte) 0x83, 0x03, (byte) 0x80, 0x11, 0x00}, null);

        verifyPin(PinType.PIN1, pin1);

        return reader.transmit(0x00, 0x2A, 0x80, 0x86, data, 0x00);
    }

    abstract void selectMasterFile() throws SmartCardReaderException;

    abstract void selectCatalogue() throws SmartCardReaderException;

    abstract void manageSecurityEnvironment() throws SmartCardReaderException;
}
