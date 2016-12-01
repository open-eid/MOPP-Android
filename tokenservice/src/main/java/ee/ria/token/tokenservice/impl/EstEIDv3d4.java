package ee.ria.token.tokenservice.impl;

import android.util.SparseArray;

import java.io.ByteArrayOutputStream;

import ee.ria.token.tokenservice.SMInterface;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.exception.PinVerificationException;
import ee.ria.token.tokenservice.util.Util;

import static ee.ria.token.tokenservice.util.AlgorithmUtils.addPadding;

public class EstEIDv3d4 implements Token {

    private static final String TAG = "EstEIDv3d4";
    private SMInterface sminterface;

    public EstEIDv3d4(SMInterface sminterface) {
        this.sminterface = sminterface;
    }

    @Override
    public byte[] sign(PinType type, String pin, byte[] data) throws Exception {
        if (!verifyPin(type, pin.getBytes())) {
            throw new PinVerificationException(type == PinType.PIN2 ? "PIN2 login failed" : "PIN1 login failed");
        }
        try {
            sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C});
            sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE});
            sminterface.transmitExtended(new byte[]{0x00, 0x22, (byte) 0xF3, 0x01});
            switch (type) {
                case PIN1:
                    //TODO: challenge to use?
                    byte[] challenge = {0x3F, 0x4B, (byte) 0xE6, 0x4B, (byte) 0xC9, 0x06, 0x6F, 0x14, (byte) 0x8A, 0x39, 0x21, (byte) 0xD8, 0x7C, (byte) 0x94, 0x41, 0x40, (byte) 0x99, 0x72, 0x4B, 0x58, 0x75, (byte) 0xA1, 0x15, 0x78};
                    return sminterface.transmitExtended(Util.concat(new byte[]{0x00, (byte) 0x88, 0x00, 0x00, 0x24}, challenge));
                case PIN2:
                    return sminterface.transmitExtended(Util.concat(new byte[]{0x00, 0x2A, (byte) 0x9E, (byte) 0x9A, 0x23}, addPadding(data)));
                default:
                    throw new Exception("Unsupported");
            }
        } catch (Exception e) {
            throw new Exception(type == PinType.PIN2 ? "Sign failed " + e.getMessage() : "Auth. failed " + e.getMessage());
        }
    }

    @Override
    public byte readRetryCounter(PinType pinType) throws Exception {
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x00, 0x16});
        byte[] bytes = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xB2, pinType.retryValue, 0x04, 0x00});
        return bytes[5];
    }

    @Override
    public SparseArray<String> readPersonalFile() throws Exception {
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, (byte) 0x50, (byte) 0x44});
        SparseArray<String> result = new SparseArray<>();
        for (byte i = 1; i <= 16; ++i) {
            byte[] data = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xB2, i, 0x04, 0x00});
            result.put(i, new String(data, "Windows-1252"));
        }
        return result;
    }

    @Override
    public boolean changePin(PinType pinType, byte[] previousPin, byte[] newPin) throws Exception {
        byte[] recv = sminterface.transmit(Util.concat(new byte[]{0x00, 0x24, 0x00, pinType.value, (byte) (previousPin.length + newPin.length)}, previousPin, newPin));
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
        byte[] recv = sminterface.transmit(new byte[]{0x00, 0x2C, 0x03, pinType.value});
        return SMInterface.checkSW(recv);
    }

    @Override
    public byte[] readCert(CertType type) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE});
        sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x02, 0x04, 0x02, (byte) 0xAA, (byte) 0xCE});

        for (byte i = 0; i <= 5; ++i) {
            byte[] data = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xB0, i, 0x00, 0x00});
            byteStream.write(data);
        }
        return byteStream.toByteArray();
    }

    private boolean verifyPin(PinType type, byte[] pin) throws Exception {
        byte[] recv = sminterface.transmit(Util.concat(new byte[]{0x00, 0x20, 0x00, type.value, (byte) pin.length}, pin));
        return SMInterface.checkSW(recv);
    }

}
