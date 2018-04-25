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

import ee.ria.scardcomlibrary.SmartCardComChannel;
import ee.ria.scardcomlibrary.SmartCardCommunicationException;
import ee.ria.tokenlibrary.exception.PinVerificationException;
import ee.ria.tokenlibrary.exception.TokenException;
import ee.ria.tokenlibrary.util.Util;

abstract class EstEIDToken implements Token {

    private SmartCardComChannel comChannel;

    EstEIDToken(SmartCardComChannel comChannel) {
        this.comChannel = comChannel;
    }

    void verifyPin(PinType type, byte[] pin) {
        byte[] recv = transmit(Util.concat(new byte[]{0x00, 0x20, 0x00, type.value, (byte) pin.length}, pin));
        if (!checkSW(recv)) {
            throw new PinVerificationException(type);
        }
    }

    void blockPin(PinType pinType, int newPinLength) {
        byte retries = readRetryCounter(pinType);
        for (int i = 0; i <= retries; i++) {
            transmit(Util.concat(new byte[]{0x00, 0x20, 0x00, pinType.value}, new byte[newPinLength], new byte[]{(byte) i}));
        }
    }

    private byte[] readCertRecords() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (byte i = 0; i <= 6; ++i) {
            byte[] data = transmitExtended(new byte[]{0x00, (byte) 0xB0, i, 0x00, 0x00});
            try {
                byteStream.write(data);
            } catch (IOException e) {
                throw new TokenException(e);
            }
        }
        return byteStream.toByteArray();
    }

    byte[] readRecord(byte recordNumber) throws SmartCardCommunicationException {
        return transmitExtended(new byte[]{0x00, (byte) 0xB2, recordNumber, 0x04, 0x00});
    }

    @Override
    public byte[] readCert(CertType type) {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, type.value, (byte) 0xCE});
        return readCertRecords();
    }

    @Override
    public byte readRetryCounter(PinType pinType) {
        selectMasterFile();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x16});
        byte[] bytes = transmitExtended(new byte[]{0x00, (byte) 0xB2, pinType.retryValue, 0x04, 0x00});
        return bytes[5];
    }

    @Override
    public int readUseCounter(CertType certType) {
        byte activeCertKey = readActiveCertKey(certType);

        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x13});

        byte[] bytes = transmitExtended(new byte[]{0x00, (byte) 0xB2, activeCertKey, 0x04, 0x00});

        int c = bytes[12];
        int d = bytes[13];
        int e = bytes[14];

        return 0xFFFFFF - (((c & 0xff) << 16) + ((d & 0xff) << 8) + (e & 0xff));
    }

    private byte readActiveCertKey(CertType certType) {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x33});
        byte[] bytes = transmitExtended(new byte[]{0x00, (byte) 0xB2, 0x01, 0x04, 0x00});
        switch (certType) {
            case CertAuth:
                int authKey = bytes[0x09] == 0x11 && bytes[0x0A] == 0x00 ? 3 : 4;
                return (byte) authKey;
            case CertSign:
                int certKey = bytes[0x13] == 0x01 && bytes[0x14] == 0x00 ? 1 : 2;
                return (byte) certKey;
            default:
                return 0;
        }
    }

    byte[] transmit(byte[] apdu) {
        return comChannel.transmit(apdu);
    }

    byte[] transmitExtended(byte[] apdu) {
        return comChannel.transmitExtended(apdu);
    }

    boolean isSecureChannel() {
        return comChannel.isSecureChannel();
    }

    boolean checkSW(byte[] resp) {
        byte sw1 = resp[resp.length - 2];
        byte sw2 = resp[resp.length - 1];
        return sw1 == (byte) 0x90 && sw2 == (byte) 0x00;
    }

    abstract void selectMasterFile();

    abstract void selectCatalogue();

    abstract void manageSecurityEnvironment();

    abstract void selectPersonalDataFile();
}
