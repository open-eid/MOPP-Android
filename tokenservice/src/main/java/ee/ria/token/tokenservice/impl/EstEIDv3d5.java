package ee.ria.token.tokenservice.impl;

import android.util.SparseArray;

import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.util.AlgorithmUtils;
import ee.ria.token.tokenservice.util.Util;

public class EstEIDv3d5 extends EstEIDToken {

    public EstEIDv3d5(CardReader cardReader) {
        super(cardReader);
    }

    @Override
    public byte[] sign(PinType type, String pin, byte[] data) throws Exception {
        verifyPin(type, pin.getBytes());
        try {
            selectMasterFile();
            selectCatalogue();
            transmitExtended(new byte[]{0x00, 0x22, (byte) 0xF3, 0x01, 0x00});
            transmitExtended(new byte[]{0x00, 0x22, 0x41, (byte) 0xB8, 0x02, (byte) 0x83, 0x00});
            switch (type) {
                case PIN1:
                    return transmitExtended(Util.concat(new byte[]{0x00, (byte) 0x88, 0x00, 0x00, (byte) data.length}, data));
                case PIN2:
                    byte[] padded = AlgorithmUtils.addPadding(data);
                    return transmitExtended(Util.concat(new byte[]{0x00, 0x2A, (byte) 0x9E, (byte) 0x9A, (byte) padded.length}, padded));
                default:
                    throw new Exception("Unsuported");
            }
        } catch (Exception e) {
            throw new Exception(type == PinType.PIN2 ? "Sign failed " + e.getMessage() : "Auth. failed " + e.getMessage());
        }
    }

    @Override
    public SparseArray<String> readPersonalFile() throws Exception {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44});
        SparseArray<String> result = new SparseArray<>();
        for (byte i = 1; i <= 16; ++i) {
            byte[] data = transmitExtended(new byte[]{0x00, (byte) 0xB2, i, 0x04, 0x00});
            result.put(i, new String(data, "Windows-1252"));
        }
        return result;
    }

    @Override
    public boolean changePin(PinType pinType, byte[] currentPin, byte[] newPin) throws Exception {
        if (isNfcComChannel()) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        byte[] recv = transmit(Util.concat(new byte[]{0x00, 0x24, 0x00, pinType.value, (byte) (currentPin.length + newPin.length)}, currentPin, newPin));
        return checkSW(recv);
    }

    @Override
    public boolean unblockPin(PinType pinType, byte[] puk) throws Exception {
        if (isNfcComChannel()) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        verifyPin(PinType.PUK, puk);
        byte[] recv = transmit(new byte[]{0x00, 0x2C, 0x03, pinType.value, 0x00});
        return checkSW(recv);
    }

    @Override
    public byte[] readCert(CertType type) throws Exception {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, type.value, (byte) 0xCE});
        return readCertRecords();
    }

    @Override
    void selectMasterFile() throws Exception {
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
    }

    @Override
    void selectCatalogue() throws Exception {
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x04, 0x02, (byte) 0xEE, (byte) 0xEE});
    }

}
