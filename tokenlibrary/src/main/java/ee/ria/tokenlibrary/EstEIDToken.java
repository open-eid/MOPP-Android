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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ee.ria.scardcomlibrary.SmartCardReader;
import ee.ria.scardcomlibrary.SmartCardReaderException;
import ee.ria.tokenlibrary.exception.PinVerificationException;
import ee.ria.tokenlibrary.exception.TokenException;

abstract class EstEIDToken implements Token {

    private final SmartCardReader reader;

    EstEIDToken(SmartCardReader reader) {
        this.reader = reader;
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

    private byte[] readCertRecords() throws SmartCardReaderException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (int i = 0; i <= 6; ++i) {
            byte[] data = reader.transmit(0x00, 0xB0, i, 0x00, null, 0x00);
            try {
                byteStream.write(data);
            } catch (IOException e) {
                throw new TokenException(e);
            }
        }
        return byteStream.toByteArray();
    }

    byte[] readRecord(byte recordNumber) throws SmartCardReaderException {
        return reader.transmit(0x00, 0xB2, recordNumber, 0x04, null, 0x00);
    }

    @Override
    public byte[] readCert(CertType type) throws SmartCardReaderException {
        selectMasterFile();
        selectCatalogue();
        reader.transmit(0x00, 0xA4, 0x02, 0x04, new byte[] {type.value, (byte) 0xCE}, null);
        return readCertRecords();
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

    abstract void selectPersonalDataFile() throws SmartCardReaderException;
}
