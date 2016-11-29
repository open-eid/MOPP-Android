package ee.ria.aidl.token.tokenaidlservice;

import android.util.SparseArray;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.Cipher;

public class EstEidToken extends Token {
    private SMInterface sminterface;

    public EstEidToken(SMInterface sminterface) {
        this.sminterface = sminterface;
    }

    private byte[] readCertData(CertType type) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        sminterface.transmitExtended(new byte[] {0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
        sminterface.transmitExtended(new byte[] {0x00, (byte) 0xA4, 0x01, 0x04, 0x02, (byte) 0xEE, (byte) 0xEE });
        sminterface.transmitExtended(new byte[] {0x00, (byte) 0xA4, 0x02, 0x04, 0x02, type.value, (byte) 0xCE });

        for (byte i = 0; i <= 5; ++i) {
            byte[] data = sminterface.transmitExtended(new byte[] { 0x00, (byte)0xB0, i, 0x00, 0x00 });
            byteStream.write(data);
        }
        return byteStream.toByteArray();
    }

    @Override
    public void readCert(CertType type) {
        try {
            certListener.onCertificateResponse(type, readCertData(type));
        } catch (Exception e) {
            certListener.onCertificateError(e.getMessage());
        }
    }

    @Override
    public void readPersonalFile() {
        try {
            sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x00 });
            sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE });
            sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44 });
            SparseArray<String> result = new SparseArray<String>();
            for (byte i = 1; i <= 16; ++i) {
                byte[] data = sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xB2, i, 0x04, 0x00 });
                result.put(i, new String(data, "Windows-1252"));
            }
            personalFileListener.onPersonalFileResponse(result);
        } catch (Exception e) {
            personalFileListener.onPersonalFileError(e.getMessage());
        }
    }

    @Override
    public void sign(PinType type, byte[] data, String pin) {
        final PinType localType = type;
        final byte[] localData = data;

        try {
            if (!login(localType, pin.getBytes())) {
                signListener.onSignError(localType == PinType.PIN2 ? "PIN2 login failed" : "PIN1 login failed");
                return;
            }
            // sign
            sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x00 });
            sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE });
            sminterface.transmitExtended(new byte[] { 0x00, 0x22, (byte)0xF3, 0x01, 0x00 });
            sminterface.transmitExtended(new byte[] { 0x00, 0x22, 0x41, (byte) 0xB8, 0x02, (byte) 0x83, 0x00 } );
            switch (localType) {
                case PIN1:
                    signListener.onSignResponse(
                            sminterface.transmitExtended(Util.concat(new byte[] { 0x00, (byte) 0x88, 0x00, 0x00, (byte) localData.length}, localData)));
                    break;
                case PIN2:
                    signListener.onSignResponse(
                            sminterface.transmitExtended(Util.concat(new byte[] { 0x00, 0x2A, (byte)0x9E, (byte)0x9A, (byte) localData.length}, localData)));
                    break;
                default:
                    throw new Exception("Unsuported");
            }
        } catch (Exception e) {
            signListener.onSignError(localType == PinType.PIN2 ?
                    "Sign failed" + e.getMessage():
                    "Auth. failed" + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean login(PinType pinType, byte[] pin) throws Exception {
        byte[] recv = null;
        if (sminterface instanceof SMInterface.NFC) {
            X509Certificate x509 = Util.getX509Certificate(readCertData(pinType == PinType.PIN1 ? CertType.CertAuth : CertType.CertSign));
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, x509.getPublicKey());
            byte[] challenge = sminterface.transmitExtended(new byte[] { 0x00, (byte) 0x84, 0x00, 0x00, 0x00 });
            byte[] encoded = cipher.doFinal(Util.concat(challenge, pin));
            // sminterface.transmit(Util.concat(new byte[] { (byte) 0x80, 0x20, 0x00, 0x02, 0x00, 0x01, 0x00 }, encoded)); // Extended APDU
            sminterface.transmitExtended(Util.concat(new byte[] { (byte) 0x90, 0x20, 0x00, 0x02, (byte) 0xFF }, Arrays.copyOf(encoded, 0xFF)));
            recv = sminterface.transmit(Util.concat(new byte[] { (byte) 0x80, 0x20, 0x00, 0x02, (byte) (encoded.length - 0xFF) }, Arrays.copyOfRange(encoded, 0xFF, encoded.length)));
        } else {
            recv = sminterface.transmit(Util.concat(new byte[] { 0x00, 0x20, 0x00, pinType.value, (byte) pin.length}, pin ));
        }
        return SMInterface.checkSW(recv);
    }

    public boolean changePin(byte[] currentPin, byte[] newPin, PinType pinType) throws Exception {
        if (sminterface instanceof SMInterface.NFC) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        byte[] recv = sminterface.transmit(Util.concat(new byte[] {0x00 ,0x24, 0x00, pinType.value, (byte) (currentPin.length + newPin.length)}, currentPin, newPin));
        return SMInterface.checkSW(recv);
    }

    public boolean unblockPin(byte[] currentPuk, PinType pinType) throws Exception {
        if (sminterface instanceof SMInterface.NFC) {
            throw new Exception("PIN replace is not allowed over NFC");
        }
        if (!login(PinType.PUK, currentPuk))
            return false;
        byte[] recv = sminterface.transmit(new byte[] {0x00, 0x2C, 0x03, pinType.value, 0x00});
        return SMInterface.checkSW(recv);
    }
}

