package ee.ria.token.tokenservice.token;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ee.ria.token.tokenservice.reader.SmartCardComChannel;
import ee.ria.token.tokenservice.reader.SmartCardCommunicationException;
import ee.ria.token.tokenservice.util.Util;

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

    byte[] readCertRecords() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (byte i = 0; i <= 5; ++i) {
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
