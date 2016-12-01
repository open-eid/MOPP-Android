package ee.ria.token.tokenservice.impl;

import android.util.SparseArray;

import java.io.ByteArrayOutputStream;

import ee.ria.token.tokenservice.SMInterface;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.util.Util;

public class EstEIDv3d5 implements Token {

    private SMInterface sminterface;

    public EstEIDv3d5(SMInterface sminterface) {
        this.sminterface = sminterface;
    }

    @Override
    public byte[] sign(PinType type, String pin, byte[] data) throws Exception {
        try {
            if (!verifyPin(type, pin.getBytes())) {
                throw new Exception(type == PinType.PIN2 ? "PIN2 login failed" : "PIN1 login failed");
            }
            sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});

            //TODO: 0x0C should be 0x04?
            sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE});
            sminterface.transmitExtended(new byte[]{0x00, 0x22, (byte) 0xF3, 0x01, 0x00});
            sminterface.transmitExtended(new byte[]{0x00, 0x22, 0x41, (byte) 0xB8, 0x02, (byte) 0x83, 0x00});
            switch (type) {
                case PIN1:
                    return sminterface.transmitExtended(Util.concat(new byte[]{0x00, (byte) 0x88, 0x00, 0x00, (byte) data.length}, data));
                case PIN2:
                    return sminterface.transmitExtended(Util.concat(new byte[]{0x00, 0x2A, (byte) 0x9E, (byte) 0x9A, (byte) data.length}, data));
                default:
                    throw new Exception("Unsuported");
            }
        } catch (Exception e) {
            throw new Exception(type == PinType.PIN2 ? "Sign failed " + e.getMessage() : "Auth. failed " + e.getMessage());
        }
    }

    @Override
    public SparseArray<String> readPersonalFile() throws Exception {
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44});
        SparseArray<String> result = new SparseArray<>();
        for (byte i = 1; i <= 16; ++i) {
            byte[] data = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xB2, i, 0x04, 0x00});
            result.put(i, new String(data, "Windows-1252"));
        }
        return result;
    }

    @Override
    public boolean changePin(PinType pinType, byte[] currentPin, byte[] newPin) throws Exception {
        if (sminterface instanceof SMInterface.NFC) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        byte[] recv = sminterface.transmit(Util.concat(new byte[]{0x00, 0x24, 0x00, pinType.value, (byte) (currentPin.length + newPin.length)}, currentPin, newPin));
        return SMInterface.checkSW(recv);
    }

    @Override
    public boolean unblockPin(PinType pinType, byte[] puk) throws Exception {
        if (sminterface instanceof SMInterface.NFC) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        if (!verifyPin(PinType.PUK, puk)) {
            throw new Exception("PUK is incorrect");
        }
        byte[] recv = sminterface.transmit(new byte[]{0x00, 0x2C, 0x03, pinType.value, 0x00});
        return SMInterface.checkSW(recv);
    }

    @Override
    public byte[] readCert(CertType type) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x04, 0x02, (byte) 0xEE, (byte) 0xEE});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, type.value, (byte) 0xCE});

        for (byte i = 0; i <= 5; ++i) {
            byte[] data = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xB0, i, 0x00, 0x00});
            byteStream.write(data);
        }
        return byteStream.toByteArray();
    }

    @Override
    public byte readRetryCounter(PinType pinType) throws Exception {
        //TODO:
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x16});
        byte[] bytes = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xB2, pinType.value, 0x04, 0x00});
        return bytes[5];
    }

    private boolean verifyPin(PinType pinType, byte[] pin) throws Exception {
        byte[] recv = sminterface.transmit(Util.concat(new byte[]{0x00, 0x20, 0x00, pinType.value, (byte) pin.length}, pin));
        return SMInterface.checkSW(recv);
    }

}
