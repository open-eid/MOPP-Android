package ee.ria.token.tokenservice.impl;

import android.util.SparseArray;

import java.io.ByteArrayOutputStream;

import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.exception.PinVerificationException;
import ee.ria.token.tokenservice.reader.ScardComChannel;
import ee.ria.token.tokenservice.reader.impl.NFC;
import ee.ria.token.tokenservice.util.Util;

public abstract class EstEIDToken implements Token {

    private ScardComChannel comChannel;

    public EstEIDToken(ScardComChannel comChannel) {
        this.comChannel = comChannel;
    }

    protected void verifyPin(PinType type, byte[] pin) throws Exception {
        byte[] recv = transmit(Util.concat(new byte[]{0x00, 0x20, 0x00, type.value, (byte) pin.length}, pin));
        if (!checkSW(recv)) {
            //TODO: do we need a message here?
            throw new PinVerificationException(createExceptionMessage(type), type);
        }
    }

    protected byte[] readCertRecords() throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (byte i = 0; i <= 5; ++i) {
            byte[] data = transmitExtended(new byte[]{0x00, (byte) 0xB0, i, 0x00, 0x00});
            byteStream.write(data);
        }
        return byteStream.toByteArray();
    }

    @Override
    public byte readRetryCounter(PinType pinType) throws Exception {
        selectMasterFile();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x16});
        byte[] bytes = transmitExtended(new byte[]{0x00, (byte) 0xB2, pinType.retryValue, 0x04, 0x00});
        return bytes[5];
    }

    private String createExceptionMessage(PinType pinType) {
        switch (pinType) {
            case PIN1:
                return "PIN1 login failed";
            case PIN2:
                return "PIN2 login failed";
            case PUK:
                return "PUK login failed";
        }
        return null;
    }

    protected byte[] transmit(byte[] apdu) throws Exception {
        return comChannel.transmit(apdu);
    }

    protected byte[] transmitExtended(byte[] apdu) throws Exception {
        return comChannel.transmitExtended(apdu);
    }

    protected boolean isNfcComChannel() {
        return comChannel instanceof NFC;
    }

    protected boolean checkSW(byte[] resp) {
        byte sw1 = resp[resp.length - 2];
        byte sw2 = resp[resp.length - 1];
        return sw1 == (byte) 0x90 && sw2 == (byte) 0x00;
    }

    @Override
    public abstract byte[] sign(PinType type, String pin, byte[] data) throws Exception;
    @Override
    public abstract SparseArray<String> readPersonalFile() throws Exception;
    @Override
    public abstract boolean changePin(PinType pinType, byte[] currentPin, byte[] newPin) throws Exception;
    @Override
    public abstract boolean unblockPin(PinType pinType, byte[] puk) throws Exception;
    @Override
    public abstract byte[] readCert(CertType type) throws Exception;
    abstract void selectMasterFile() throws Exception;
    abstract void selectCatalogue() throws Exception;
}
