package ee.ria.DigiDoc.idcard;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ee.ria.DigiDoc.common.exception.SignatureUpdateError;
import timber.log.Timber;

public class NFC {
    private final IsoDep card;
    private final byte[] can;

    private byte[] keyEnc;
    private byte[] keyMAC;
    private byte ssc;

    public final static class NFCException extends IOException implements SignatureUpdateError {
        private final String message;

        public NFCException(String message) {
            super(message);
            this.message = message;
        }

        @Override
        public String getMessage(Context context) {
            return message;
        }
    }

    public final static class Result {
        public final int code;
        public final byte[] data;

        public Result(byte[] data) {
            if ((data != null) && (data.length >= 2)) {
                this.data = data;
                this.code = (((int) data[data.length - 2] & 0xff) << 8) | ((int) data[data.length - 1] & 0xff);
            } else {
                this.data = new byte[0];
                this.code = 0;
            }
        }
        public Result(int code, byte[] data) {
            this.code = code;
            if ((data != null) && (data.length >= 2)) {
                this.data = data;
            } else {
                this.data = new byte[0];
            }
        }
    }

    private static final class TLV {
        public final byte[] data;
        public final int tag;
        public final int start;
        public final int end;

        public TLV(byte[] data, int start, int end) throws NFCException {
            this.data = data;
            int pos = start;
            //this.dataLength = dataLength;
            //this.startPos = pos;
            if ((data[pos] & 0x1F) == 0x1F && (data[pos + 1] & 0x80) == 0x80) {
                tag = (((int) data[pos++] & 0xff) << 16) | (((int) data[pos++] & 0xff) << 8) | ((int) data[pos++] & 0xff);
            } else if ((data[pos] & 0x1F) == 0x1F) {
                tag = (((int) data[pos++] & 0xff) << 8) | ((int) data[pos++] & 0xff);
            } else {
                tag = ((int) data[pos++] & 0xff);
            }
            int length = ((int) data[pos++] & 0xff);
            if ((length & 0x80) != 0) {
                int numberOfLengthBytes = length & 0x7F;
                if (numberOfLengthBytes > 4) {
                    throw new NFCException("TLV Message size invalid");
                }
                length = 0;
                for (int i = 0; i < numberOfLengthBytes; ++i) {
                    length <<= 8;
                    length += ((int) data[pos++] & 0xff);
                }
            }
            this.start = pos;
            this.end = pos + length;
        }

        public TLV(byte[] data) throws IOException {
            this(data, 0, data.length);
        }

        public static byte[] wrap (@Nonnull byte[] cmd, byte[] data) {
            int len = (data != null) ? data.length : 0;
            byte[] r = new byte[cmd.length + 1 + len];
            System.arraycopy(cmd, 0, r, 0, cmd.length);
            r[cmd.length] = (byte) len;
            if (data != null) System.arraycopy(data, 0, r, cmd.length + 1, data.length);
            return r;
        }

        public static byte[] wrap (int cmd, byte[] data) {
            int len = (data != null) ? data.length : 0;
            byte[] r = new byte[1 + 1 + len];
            r[0] = (byte) cmd;
            r[1] = (byte) len;
            if (data != null) System.arraycopy(data, 0, r, 1 + 1, data.length);
            return r;
        }

        public static TLV decode(String context, byte[] data, int start, int end, int...tags) throws NFCException {
            TLV tlv = null;
            for (int tag: tags) {
                tlv = new TLV(data, start, end);
                if (tlv.tag != tag) throw new NFCException(context + String.format(": Invalid tag - expected %x, got %x", tag, tlv.tag));
                start = tlv.start;
                end = tlv.end;
            }
            return tlv;
        }

        public static TLV decodeResult(String context, byte[] data, int... tags) throws NFCException {
            int code = (((int) data[data.length - 2] & 0xff) << 8) | ((int) data[data.length - 1] & 0xff);
            if (code != 0x9000) throw new NFCException(context + String.format(": Invalid result %x", code));
            int start = 0;
            int end = data.length - 2;
            TLV tlv = null;
            for (int tag: tags) {
                tlv = new TLV(data, start, end);
                if (tlv.tag != tag) throw new NFCException(context + String.format(": Invalid tag - expected %x, got %x", tag, tlv.tag));
                start = tlv.start;
                end = tlv.end;
            }
            return tlv;
        }
    }

    public NFC(IsoDep card, byte[] can) throws NFCException {
        this.card = card;
        this.can = can;
        try {
            byte[][] keys = PACE(can);
            keyEnc = keys[0];
            keyMAC = keys[1];
        } catch (Exception exc) {
            Timber.log(Log.ERROR, "NFC Error: %s", exc.getMessage());
            throw new NFCException(exc.getMessage());
        }
    }

    private static final byte[] CMD_SEL_MASTER = Hex.decode("00a4040c");
    private static final byte[] SEL_MAIN_AID = Hex.decode("a000000077010800070000fe00000100");

    private static final byte[] CMD_SELECT_DF = Hex.decode("00a4010c");
    private static final byte[] CMD_READ_BINARY = Hex.decode("00B00000");
    private static final byte[] CMD_SIGN = Hex.decode("002A9E9A");

    public byte[] calculateSignature(byte[] data) throws NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        Result r = communicateSecure(CMD_SIGN, data);
        Timber.log(Log.DEBUG, "SIGN:%x %s", r.code, Hex.toHexString(r.data));
        return r.data;
    }

    private Result communicate(byte[] APDU) throws IOException {
        byte[] response = card.transceive(APDU);
        return new Result(response);
    }

    private Result communicatePlain(byte[] cmd, byte[] data) throws IOException {
        byte[] APDU = createPlainAPDU(cmd[1], cmd[2], cmd[3], data, false);
        byte[] response = card.transceive(APDU);
        return new Result(response);
    }

    public Result communicateSecure(byte[] cmd, byte[] data) {
        byte [] response = null;
        try {
            byte[] APDU = createSecureAPDU(cmd[1], cmd[2], cmd[3], data);
            Timber.log(Log.DEBUG, "APDU: %s", Hex.toHexString(APDU));
            response = card.transceive(APDU);
            Timber.log(Log.DEBUG, "RESPONSE: %s", Hex.toHexString(response));
            int code = (((int) response[response.length - 2] & 0xff) << 8) | ((int) response[response.length - 1] & 0xff);
            if (response.length > 2) {
                TLV tlv = new TLV(response, 0, response.length - 2);
                Timber.log(Log.DEBUG, "TLV:%x %s", tlv.tag, Hex.toHexString(tlv.data, tlv.start, tlv.end - tlv.start));
                if (tlv.tag == 0x87) {
                    TLV tlv_enc = tlv;
                    Timber.log(Log.DEBUG, "ENC:%x %s", tlv_enc.tag, Hex.toHexString(tlv_enc.data, tlv_enc.start, tlv_enc.end - tlv_enc.start));
                    TLV tlv_res = new TLV(tlv.data, tlv.end, response.length - 2);
                    Timber.log(Log.DEBUG, "RES:%x %s", tlv_res.tag, Hex.toHexString(tlv_res.data, tlv_res.start, tlv_res.end - tlv_res.start));
                    TLV tlv_mac = new TLV(tlv.data, tlv_res.end, response.length - 2);
                    Timber.log(Log.DEBUG, "MAC:%x %s", tlv_mac.tag, Hex.toHexString(tlv_mac.data, tlv_mac.start, tlv_mac.end - tlv_mac.start));
                    if (tlv_enc.data[tlv_enc.start] != 0x1) {
                        throw new IOException("Missing padding indicator");
                    }
                    byte[] decrypted = encryptDecryptData(Arrays.copyOfRange(tlv_enc.data, tlv_enc.start + 1, tlv_enc.end), Cipher.DECRYPT_MODE);
                    int indexOfTerminator = Hex.toHexString(decrypted).lastIndexOf("80") / 2;
                    byte[] pruned = Arrays.copyOf(decrypted, indexOfTerminator);
                    return new Result(code, pruned);
                } else {
                    TLV tlv_res = tlv;
                    Timber.log(Log.DEBUG, "RES:%x %s", tlv_res.tag, Hex.toHexString(tlv_res.data, tlv_res.start, tlv_res.end - tlv_res.start));
                    TLV tlv_mac = new TLV(tlv.data, tlv_res.end, response.length - 2);
                    Timber.log(Log.DEBUG, "MAC:%x %s", tlv_mac.tag, Hex.toHexString(tlv_mac.data, tlv_mac.start, tlv_mac.end - tlv_mac.start));
                }
            }
            return new Result(code, response);
        } catch (RuntimeException e) {
            Timber.log(Log.ERROR, "Exception in app with NFC: %s", e.getMessage());
        } catch (Exception exc) {
            Timber.log(Log.ERROR, "NFC Error: %s", exc.getMessage());
        }
        return new Result(response);
    }

    public byte[] readCertificate() {
        try {
            return getCertificate(false);
        } catch (Exception exc) {
            Timber.log(Log.ERROR, "NFC Error: %s", exc.getMessage());
        }
        return null;
    }

    private byte[] getMAC(byte[] APDU, byte[] keyMAC) {
        BlockCipher blockCipher = new AESEngine();
        CMac cmac = new CMac(blockCipher);
        cmac.init(new KeyParameter(keyMAC));
        cmac.update(APDU, 0, APDU.length);
        byte[] MAC = new byte[cmac.getMacSize()];
        cmac.doFinal(MAC, 0);
        return Arrays.copyOf(MAC, 8);
    }

    private byte[] createAPDU(byte[] template, byte[] data, int extra) {
        byte[] APDU = Arrays.copyOf(template, template.length + extra);
        System.arraycopy(data, 0, APDU, template.length, data.length);
        return APDU;
    }

    private byte[] createKey(byte[] unpadded, byte last) throws NoSuchAlgorithmException {
        byte[] padded = Arrays.copyOf(unpadded, unpadded.length + 4);
        padded[padded.length - 1] = last;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest.digest(padded);
    }

    private byte[] decryptNonce(byte[] encryptedNonce, byte[] CAN) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] decryptionKey = createKey(CAN, (byte) 3);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptionKey, "AES"), new IvParameterSpec(new byte[16]));
        return cipher.doFinal(encryptedNonce);
    }

    private static final byte[] MSE_SET_AT_PACE = Hex.decode("0022c1a4");
    private static final byte[] MSE_SET_AT_PACE_DATA = Hex.decode("800a04007f00070202040204830102");

    private static final byte[] GA_GET_NONCE = Hex.decode("10860000");
    private static final byte[] GA_GET_NONCE_DATA = Hex.decode("7c00");

    private static final byte[] GA_MAP_NONCE = Hex.decode("10860000");

    private static final byte[] GA_KEY_AGREEMENT = Hex.decode("10860000");

    private static final byte[] dataForMACIncomplete = Hex.decode("7f494f060a04007f000702020402048641");

    private static final byte[] GAMutualAuthenticationIncomplete = Hex.decode("008600000c7c0a8508");
    private static final byte[] GAMutualAuthentication = Hex.decode("008600000c7c0a8508");
    private static final byte[] GAMutualAuthentication_DATA = Hex.decode("7c0a8508");

    private static final byte DYN_AUTH_DATA = 0x7c;
    private static final byte[] readFile = Hex.decode("0cb000000d970100");
    private static final byte[] IASECCFID = {0x3f, 0x00};
    private static final byte[] AWP = {(byte) 0xad, (byte) 0xf1};
    private static final byte[] QSCD = {(byte) 0xad, (byte) 0xf2};
    private static final byte[] authCert = {0x34, 0x01};
    private static final byte[] signCert = {0x34, 0x1f};

    private byte[][] PACE(byte[] CAN) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        // Select Master AID
        Result res = communicatePlain(CMD_SEL_MASTER, SEL_MAIN_AID);
        // Select PACE
        res = communicatePlain(MSE_SET_AT_PACE, MSE_SET_AT_PACE_DATA);

        // Get nonce
        byte[] APDU = createPlainAPDU(GA_GET_NONCE[1], GA_GET_NONCE[2], GA_GET_NONCE[3], GA_GET_NONCE_DATA, true);
        APDU[0] = 0x10;
        res = communicate(APDU);
        Timber.log(Log.DEBUG, "Get nonce:%s", Hex.toHexString(res.data));
        TLV tlv_nonce = TLV.decodeResult("Get nonce", res.data, DYN_AUTH_DATA, 0x80);
        byte[] decryptedNonce = decryptNonce(Arrays.copyOfRange(tlv_nonce.data, tlv_nonce.start, tlv_nonce.end), CAN);

        // Generate an EC keypair and exchange public keys with the chip
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        BigInteger privateKey = new BigInteger(255, new SecureRandom()).add(BigInteger.ONE); // should be in [1, spec.getN()-1], but this is good enough for this application
        ECPoint publicKey = spec.getG().multiply(privateKey).normalize();

        byte[] pk = publicKey.getEncoded(false);
        APDU = createPlainAPDU(GA_MAP_NONCE[1], GA_MAP_NONCE[2], GA_MAP_NONCE[3], TLV.wrap(DYN_AUTH_DATA, TLV.wrap(0x81, pk)));
        APDU[0] = 0x10;
        Timber.log(Log.DEBUG, "APDU2: %s", Hex.toHexString(APDU));
        res = communicate(APDU);
        Timber.log(Log.DEBUG, "Map nonce:%s", Hex.toHexString(res.data));
        ECPoint cardPublicKey = spec.getCurve().decodePoint(Arrays.copyOfRange(res.data, 4, 69));

        // calculate the new base point, use it to generate a new keypair, and exchange public keys
        ECPoint sharedSecret = cardPublicKey.multiply(privateKey);
        ECPoint mappedECBasePoint = spec.getG().multiply(new BigInteger(1, decryptedNonce)).add(sharedSecret).normalize();
        privateKey = new BigInteger(255, new SecureRandom()).add(BigInteger.ONE);
        publicKey = mappedECBasePoint.multiply(privateKey).normalize();
        byte[] pk2 = publicKey.getEncoded(false);
        byte[] APDU2 = createPlainAPDU(GA_KEY_AGREEMENT[1], GA_KEY_AGREEMENT[2], GA_KEY_AGREEMENT[3], TLV.wrap(DYN_AUTH_DATA, TLV.wrap(0x83, pk2)));
        APDU2[0] = 0x10;
        Timber.log(Log.DEBUG, "APDU2: %s", Hex.toHexString(APDU2));

        res = communicate(APDU2);
        Timber.log(Log.DEBUG, "Key agreement:%s", Hex.toHexString(res.data));
        TLV tlv_key = TLV.decodeResult("Key agreement", res.data, DYN_AUTH_DATA, 0x84);
        cardPublicKey = spec.getCurve().decodePoint(Arrays.copyOfRange(tlv_key.data, tlv_key.start, tlv_key.end));

        // generate the session keys and exchange MACs to verify them
        byte[] secret = cardPublicKey.multiply(privateKey).normalize().getAffineXCoord().getEncoded();
        byte[] keyEnc = createKey(secret, (byte) 1);
        byte[] keyMAC = createKey(secret, (byte) 2);
        byte[] pk3 = cardPublicKey.getEncoded(false);
        APDU = createAPDU(dataForMACIncomplete, pk3, 65);
        byte[] MAC = getMAC(APDU, keyMAC);
        APDU2 = createPlainAPDU(GAMutualAuthentication[1], GAMutualAuthentication[2], GAMutualAuthentication[3], GAMutualAuthentication_DATA, MAC);
        Timber.log(Log.DEBUG, "APDU2: %s", Hex.toHexString(APDU2));
        res = communicate(APDU2);
        Timber.log(Log.DEBUG, "Mutual authentication:%s", Hex.toHexString(res.data));
        TLV tlv_mac = TLV.decodeResult("Mutual authentication", res.data, DYN_AUTH_DATA, 0x86);

        // verify chip's MAC and return session keys
        byte[] pk4 = publicKey.getEncoded(false);
        APDU = createAPDU(dataForMACIncomplete, pk4, 65);
        MAC = getMAC(APDU, keyMAC);
        if (!Arrays.equals(MAC, Arrays.copyOfRange(tlv_mac.data, tlv_mac.start, tlv_mac.end))) {
            throw new RuntimeException("Could not verify chip's MAC."); // *Should* never happen.
        }
        return new byte[][]{keyEnc, keyMAC};

    }

    private byte[] encryptDecryptData(byte[] data, int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyEnc, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] iv = Arrays.copyOf(cipher.doFinal(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ssc}), 16);
        cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(mode, secretKeySpec, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    private byte[] createPlainAPDU(byte INS, byte P1, byte P2, byte[] data, boolean hasResponse) {
        byte[] APDU = new byte[4 + 1 + data.length + ((hasResponse) ? 1 : 0)];
        APDU[1] = INS;
        APDU[2] = P1;
        APDU[3] = P2;
        APDU[4] = (byte) data.length;
        System.arraycopy(data, 0, APDU, 5, data.length);
        return APDU;
    }

    private byte[] createPlainAPDU(byte INS, byte P1, byte P2, byte[]... data) {
        int len = 0;
        for (byte[] d : data) len += d.length;
        byte[] APDU = new byte[4 + 1 + len + 1]; // Add LE
        APDU[1] = INS;
        APDU[2] = P1;
        APDU[3] = P2;
        APDU[4] = (byte) len;
        int pos = 5;
        for (byte[] d : data) {
            System.arraycopy(d, 0, APDU, pos, d.length);
            pos += d.length;
        }
        return APDU;
    }

    private byte[] createSecureAPDU(byte INS, byte P1, byte P2, byte[] data) throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        ssc++;

        int enc_len = ((data != null) && (data.length != 0)) ? 16 * (1 + data.length / 16) : 0;
        byte[] enc = new byte[enc_len];
        int mac_len = 48 + enc_len;
        byte[] mac_buf = new byte[mac_len];

        mac_buf[15] = ssc;
        mac_buf[16] = (byte) 0xc;
        mac_buf[17] = INS;
        mac_buf[18] = P1;
        mac_buf[19] = P2;
        mac_buf[20] = (byte) 0x80;
        mac_buf[32] = (enc_len > 0) ? (byte) 0x87 : (byte) 0x97;
        mac_buf[33] = (byte) (enc_len + 1);
        mac_buf[34] = (enc_len > 0) ? (byte) 0x1 : (byte) 0x0;
        if (enc_len > 0) {
            byte[] paddedData = Arrays.copyOf(data, enc_len);
            paddedData[data.length] = (byte) 0x80;
            enc = encryptDecryptData(paddedData, Cipher.ENCRYPT_MODE);
            System.arraycopy(enc, 0, mac_buf, 35, enc_len);
        }
        mac_buf[35 + enc_len] = (byte) 0x80;
        byte[] MAC = getMAC(mac_buf, keyMAC);

        int apdu_len = 4 + 1 + 3 + enc_len + 2 + 8 + 1;
        byte[] APDU = new byte[apdu_len];
        APDU[0] = 0xc;
        APDU[1] = INS;
        APDU[2] = P1;
        APDU[3] = P2;
        APDU[4] = (byte) (apdu_len - 6);
        APDU[5] = (enc_len > 0) ? (byte) 0x87 : (byte) 0x97;
        APDU[6] = (byte) (enc_len + 1);
        APDU[7] = (enc_len > 0) ? (byte) 0x1 : (byte) 0x0;
        System.arraycopy(enc, 0, APDU, 8, enc_len);
        APDU[8 + enc_len] = (byte) 0x8e;
        APDU[8 + enc_len + 1] = (byte) 0x8;
        System.arraycopy(MAC, 0, APDU, 8 + enc_len + 2, 8);
        ssc++;
        return APDU;
    }

    public byte[] getCertificate(boolean authOrSign) throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {

        // selectFile(IASECCFID, "the master application");
        communicateSecure(CMD_SELECT_DF, IASECCFID);

        // selectFile(authOrSign ? AWP : QSCD, "the application");
        communicateSecure(CMD_SELECT_DF, authOrSign ? AWP : QSCD);

        // selectFile(authOrSign ? authCert : signCert, "the certificate");
        communicateSecure(CMD_SELECT_DF, authOrSign ? authCert : signCert);

        byte[] certificate = new byte[0];
        byte[] readCert = Arrays.copyOf(readFile, readFile.length);
        // Construct the certificate byte array n=indexOfTerminator bytes at a time
        for (int i = 0; i < 16; i++) {

            // Set the P1/P2 values to incrementally read the certificate
            readCert[2] = (byte) (certificate.length / 256);
            readCert[3] = (byte) (certificate.length % 256);
            byte[] response = getResponse(new byte[0], readCert, "Read the certificate");
            if (response[response.length - 2] == 0x6b && response[response.length - 1] == 0x00) {
                throw new RuntimeException("Wrong read parameters.");
            }

            // Set the range containing a portion of the certificate and decrypt it
            int start = response[2] == 1 ? 3 : 4;
            int end = start + (response[start - 2] + 256) % 256 - 1;
            byte[] decrypted = encryptDecryptData(Arrays.copyOfRange(response, start, end), Cipher.DECRYPT_MODE);
            int indexOfTerminator = Hex.toHexString(decrypted).lastIndexOf("80") / 2;
            certificate = Arrays.copyOf(certificate, certificate.length + indexOfTerminator);
            System.arraycopy(decrypted, 0, certificate, certificate.length - indexOfTerminator, indexOfTerminator);

            if (response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00) {
                break;
            }
        }

        return certificate;

    }

    private byte[] getResponse(byte[] data, byte[] command, String log) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
        byte[] APDU = createSecureAPDU(command[1], command[2], command[3], data);
        byte[] response = card.transceive(APDU);
        Log.i(log, Hex.toHexString(response));
        return response;
    }

}
