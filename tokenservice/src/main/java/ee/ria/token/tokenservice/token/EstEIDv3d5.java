package ee.ria.token.tokenservice.token;

import android.util.SparseArray;

import java.io.UnsupportedEncodingException;

import ee.ria.token.tokenservice.reader.SmartCardComChannel;
import ee.ria.token.tokenservice.util.AlgorithmUtils;
import ee.ria.token.tokenservice.util.Util;

public class EstEIDv3d5 extends EstEIDToken {

    public EstEIDv3d5(SmartCardComChannel comChannel) {
        super(comChannel);
    }

    @Override
    public byte[] sign(PinType type, String pin, byte[] data) {
        verifyPin(type, pin.getBytes());
        try {
            selectMasterFile();
            selectCatalogue();
            manageSecurityEnvironment();
            switch (type) {
                case PIN1:
                    return transmitExtended(Util.concat(new byte[]{0x00, (byte) 0x88, 0x00, 0x00, (byte) data.length}, data));
                case PIN2:
                    byte[] padded = AlgorithmUtils.addPadding(data);
                    return transmitExtended(Util.concat(new byte[]{0x00, 0x2A, (byte) 0x9E, (byte) 0x9A, (byte) padded.length}, padded));
                default:
                    throw new Exception("Unsupported");
            }
        } catch (Exception e) {
            throw new SignOperationFailedException(type, e);
        }
    }

    @Override
    public SparseArray<String> readPersonalFile(){
        selectMasterFile();
        selectCatalogue();
        selectPersonalDataFile();
        SparseArray<String> result = new SparseArray<>();
        for (byte i = 1; i <= 16; ++i) {
            byte[] data = readRecord(i);
            try {
                result.put(i, new String(data, "Windows-1252"));
            } catch (UnsupportedEncodingException e) {
                throw new TokenException(e);
            }
        }
        return result;
    }

    @Override
    public boolean changePin(PinType pinType, byte[] currentPin, byte[] newPin) {
        if (!isSecureChannel()) {
            throw new SecureOperationOverUnsecureChannelException("PIN replace is not allowed");
        }
        byte[] recv = transmit(Util.concat(new byte[]{0x00, 0x24, 0x00, pinType.value, (byte) (currentPin.length + newPin.length)}, currentPin, newPin));
        return checkSW(recv);
    }

    @Override
    public boolean unblockAndChangePin(PinType pinType, byte[] puk, byte[] newPin) {
        if (!isSecureChannel()) {
            throw new SecureOperationOverUnsecureChannelException("PIN replace is not allowed");
        }
        verifyPin(PinType.PUK, puk);
        byte[] recv = new byte[0];
        switch (pinType) {
            case PIN1:
                //PIN1 = 00hex 2Chex 00hex 01hex 0Chex 3132333435363738hex || 34333221hex
                recv = transmit(Util.concat(new byte[]{0x00, 0x2C, 0x00, pinType.value, 0x0C}, puk, newPin));
                break;
            case PIN2:
                //PIN2 = 00hex 2Chex 00hex 02hex 0Dhex 3132333435363738hex || 3534333221hex
                recv = transmit(Util.concat(new byte[]{0x00, 0x2C, 0x00, pinType.value, 0x0D}, puk, newPin));
                break;
        }
        return checkSW(recv);
    }

    @Override
    public boolean unblockPin(PinType pinType, byte[] puk) {
        if (!isSecureChannel()) {
            throw new SecureOperationOverUnsecureChannelException("PIN unblock is not allowed");
        }
        verifyPin(PinType.PUK, puk);
        byte[] recv = transmit(new byte[]{0x00, 0x2C, 0x03, pinType.value, 0x00});
        return checkSW(recv);
    }

    @Override
    public byte[] readCert(CertType type) {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, type.value, (byte) 0xCE});
        return readCertRecords();
    }

    @Override
    void selectMasterFile() {
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
    }

    @Override
    void selectCatalogue(){
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x04, 0x02, (byte) 0xEE, (byte) 0xEE});
    }

    @Override
    void manageSecurityEnvironment() {
        transmitExtended(new byte[]{0x00, 0x22, (byte) 0xF3, 0x01, 0x00});
        transmitExtended(new byte[]{0x00, 0x22, 0x41, (byte) 0xB8, 0x05, (byte) 0x83, 0x03, (byte) 0x80});
    }

    @Override
    void selectPersonalDataFile() {
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44});
    }

}
