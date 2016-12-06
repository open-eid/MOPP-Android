package ee.ria.token.tokenservice.token;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ee.ria.token.tokenservice.reader.SmartCardComChannel;
import ee.ria.token.tokenservice.reader.SmartCardCommunicationException;
import ee.ria.token.tokenservice.util.Util;

public abstract class EstEIDToken implements Token {

    private SmartCardComChannel comChannel;

    public EstEIDToken(SmartCardComChannel comChannel) {
        this.comChannel = comChannel;
    }

    protected void verifyPin(PinType type, byte[] pin) {
        byte[] recv = transmit(Util.concat(new byte[]{0x00, 0x20, 0x00, type.value, (byte) pin.length}, pin));
        if (!checkSW(recv)) {
            throw new PinVerificationException(type);
        }
    }

    protected byte[] readCertRecords() {
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

    protected byte[] readRecord(byte recordNumber) throws SmartCardCommunicationException {
        return transmitExtended(new byte[]{0x00, (byte) 0xB2, recordNumber, 0x04, 0x00});
    }

    @Override
    public byte readRetryCounter(PinType pinType) {
        selectMasterFile();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x16});
        byte[] bytes = transmitExtended(new byte[]{0x00, (byte) 0xB2, pinType.retryValue, 0x04, 0x00});
        return bytes[5];
    }

    protected byte[] transmit(byte[] apdu) {
        return comChannel.transmit(apdu);

    }

    protected byte[] transmitExtended(byte[] apdu) {
        return comChannel.transmitExtended(apdu);
    }

    protected boolean isSecureChannel() {
        return comChannel.isSecureChannel();
    }

    protected boolean checkSW(byte[] resp) {
        byte sw1 = resp[resp.length - 2];
        byte sw2 = resp[resp.length - 1];
        return sw1 == (byte) 0x90 && sw2 == (byte) 0x00;
    }

    abstract void selectMasterFile();
    abstract void selectCatalogue();
    abstract void manageSecurityEnvironment();
    abstract void selectPersonalDataFile();
}
