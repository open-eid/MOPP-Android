package ee.ria.token.tokenservice.impl;

import android.util.SparseArray;

import ee.ria.token.tokenservice.reader.ScardComChannel;
import ee.ria.token.tokenservice.util.Util;

import static ee.ria.token.tokenservice.util.AlgorithmUtils.addPadding;

public class EstEIDv3d4 extends EstEIDToken {

    public EstEIDv3d4(ScardComChannel comChannel) {
        super(comChannel);
    }

    @Override
    public byte[] sign(PinType type, String pin, byte[] data) throws Exception {
        verifyPin(type, pin.getBytes());
        try {
            selectMasterFile();
            selectCatalogue();
            transmitExtended(new byte[]{0x00, 0x22, (byte) 0xF3, 0x01});
            switch (type) {
                case PIN1:
                    //TODO: check challenge
                    byte[] challenge = {0x3F, 0x4B, (byte) 0xE6, 0x4B, (byte) 0xC9, 0x06, 0x6F, 0x14, (byte) 0x8A, 0x39, 0x21, (byte) 0xD8, 0x7C, (byte) 0x94, 0x41, 0x40, (byte) 0x99, 0x72, 0x4B, 0x58, 0x75, (byte) 0xA1, 0x15, 0x78};
                    return transmitExtended(Util.concat(new byte[]{0x00, (byte) 0x88, 0x00, 0x00, 0x24}, challenge));
                case PIN2:
                    return transmitExtended(Util.concat(new byte[]{0x00, 0x2A, (byte) 0x9E, (byte) 0x9A, 0x23}, addPadding(data)));
                default:
                    throw new Exception("Unsupported");
            }
        } catch (Exception e) {
            throw new Exception(type == PinType.PIN2 ? "Sign failed " + e.getMessage() : "Auth. failed " + e.getMessage());
        }
    }

    @Override
    public SparseArray<String> readPersonalFile() throws Exception {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, (byte) 0x50, (byte) 0x44});
        SparseArray<String> result = new SparseArray<>();
        for (byte i = 1; i <= 16; ++i) {
            byte[] data = transmitExtended(new byte[]{0x00, (byte) 0xB2, i, 0x04, 0x00});
            result.put(i, new String(data, "Windows-1252"));
        }
        return result;
    }

    @Override
    public boolean changePin(PinType pinType, byte[] previousPin, byte[] newPin) throws Exception {
        byte[] recv = transmit(Util.concat(new byte[]{0x00, 0x24, 0x00, pinType.value, (byte) (previousPin.length + newPin.length)}, previousPin, newPin));
        return checkSW(recv);
    }

    @Override
    public boolean unblockPin(PinType pinType, byte[] puk) throws Exception {
        if (isNfcComChannel()) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        verifyPin(PinType.PUK, puk);
        byte[] recv = transmit(new byte[]{0x00, 0x2C, 0x03, pinType.value});
        return checkSW(recv);
    }

    @Override
    public byte[] readCert(CertType type) throws Exception {
        selectMasterFile();
        selectCatalogue();
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, (byte) 0xAA, (byte) 0xCE});
        return readCertRecords();
    }

    @Override
    void selectMasterFile() throws Exception {
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C});
    }

    @Override
    void selectCatalogue() throws Exception {
        transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE});
    }

}
