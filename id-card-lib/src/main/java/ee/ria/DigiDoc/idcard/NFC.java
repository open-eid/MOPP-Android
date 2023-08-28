package ee.ria.DigiDoc.idcard;

import android.nfc.tech.IsoDep;
import android.util.Log;

import androidx.annotation.Nullable;

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
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class NFC {
    private final IsoDep card;
    private final byte[] can;

    private byte[] keyEnc;
    private byte[] keyMAC;
    private byte ssc;

    public final class Result {
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

    private final class TLV {
        public final byte[] data;
        public final int tag;
        public final int start;
        public final int end;

        public TLV(byte[] data, int start, int end) throws IOException {
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
                    // fixme: use proper exception type (Lauris)
                    throw new IOException("TLV Message size invalid");
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
    }

    public NFC(IsoDep card, byte[] can) {
        this.card = card;
        this.can = can;
        // fixme: Handle exceptions meaningfully
        try {
            byte[][] keys = PACE(can);
            keyEnc = keys[0];
            keyMAC = keys[1];
        } catch (Exception exc) {
            Timber.log(Log.ERROR, "NFC Error: %s", exc.getMessage());
        }
    }

    private static final byte[] CMD_READ_BINARY = Hex.decode("00B00000");
    private static final byte[] CMD_SIGN = Hex.decode("002A9E9A");

    public byte[] calculateSignature(byte[] pin2, byte[] data, boolean q) throws NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        Result r = communicate(CMD_SIGN, data);
        Timber.log(Log.DEBUG, "SIGN:%x %s", r.code, Hex.toHexString(r.data));
        return r.data;
    }

    public Result communicate(byte[] cmd, byte[] data) {
        byte [] response = null;
        try {
            byte[] APDU = createSecureAPDU(cmd[1], cmd[2], cmd[3], data);
            Timber.log(Log.DEBUG, "APDU: %s", Hex.toHexString(APDU));
            response = card.transceive(APDU);
            Timber.log(Log.DEBUG, "RESPONSE: %s", Hex.toHexString(response));
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
                    // fixme: Check MAC (Lauris)
                    if (tlv_enc.data[tlv_enc.start] != 0x1) {
                        throw new IOException("Missin padding indicator");
                    }
                    byte[] decrypted = encryptDecryptData(Arrays.copyOfRange(tlv_enc.data, tlv_enc.start + 1, tlv_enc.end), Cipher.DECRYPT_MODE);
                    int indexOfTerminator = Hex.toHexString(decrypted).lastIndexOf("80") / 2;
                    byte[] pruned = Arrays.copyOf(decrypted, indexOfTerminator);
                    // fixme: Use result code (Lauris)
                    return new Result(0x9000, pruned);
                } else {
                    TLV tlv_res = tlv;
                    Timber.log(Log.DEBUG, "RES:%x %s", tlv_res.tag, Hex.toHexString(tlv_res.data, tlv_res.start, tlv_res.end - tlv_res.start));
                    TLV tlv_mac = new TLV(tlv.data, tlv_res.end, response.length - 2);
                    Timber.log(Log.DEBUG, "MAC:%x %s", tlv_mac.tag, Hex.toHexString(tlv_mac.data, tlv_mac.start, tlv_mac.end - tlv_mac.start));
                    // fixme: Check MAC (Lauris)
                }
            }
            // fixme: Use result code (Lauris)
            return new Result(response);
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

    /**
     * Calculates the message authentication code
     *
     * @param APDU   the byte array on which the CMAC algorithm is performed
     * @param keyMAC the key for performing CMAC
     * @return MAC
     */
    private byte[] getMAC(byte[] APDU, byte[] keyMAC) {
        BlockCipher blockCipher = new AESEngine();
        CMac cmac = new CMac(blockCipher);
        cmac.init(new KeyParameter(keyMAC));
        cmac.update(APDU, 0, APDU.length);
        byte[] MAC = new byte[cmac.getMacSize()];
        cmac.doFinal(MAC, 0);
        return Arrays.copyOf(MAC, 8);
    }

    /**
     * Creates an application protocol data unit
     *
     * @param template the byte array to be used as a template
     * @param data     the data necessary for completing the APDU
     * @param extra    the missing length of the APDU being created
     * @return the complete APDU
     */
    private byte[] createAPDU(byte[] template, byte[] data, int extra) {
        byte[] APDU = Arrays.copyOf(template, template.length + extra);
        System.arraycopy(data, 0, APDU, template.length, data.length);
        return APDU;
    }

    /**
     * Creates a cipher key
     *
     * @param unpadded the array to be used as the basis for the key
     * @param last     the last byte in the appended padding
     * @return the constructed key
     */
    private byte[] createKey(byte[] unpadded, byte last) throws NoSuchAlgorithmException {
        byte[] padded = Arrays.copyOf(unpadded, unpadded.length + 4);
        padded[padded.length - 1] = last;
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest.digest(padded);
    }

    /**
     * Decrypts the nonce
     *
     * @param encryptedNonce the encrypted nonce received from the chip
     * @param CAN            the card access number provided by the user
     * @return the decrypted nonce
     */
    private byte[] decryptNonce(byte[] encryptedNonce, byte[] CAN) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] decryptionKey = createKey(CAN, (byte) 3);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptionKey, "AES"), new IvParameterSpec(new byte[16]));
        return cipher.doFinal(encryptedNonce);
    }

    /**
     * Communicates with the card and logs the response
     *
     * @param APDU  The command
     * @param log   Information for logging
     * @return The response
     */
    private byte[] getResponse(byte[] APDU, String log) throws IOException {
        byte[] response = card.transceive(APDU);
        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            throw new RuntimeException(String.format("%s failed.", log));
        }
        Timber.log(Log.DEBUG, Hex.toHexString(response));
        return response;
    }

    /**
     * Attempts to use the PACE protocol to create a secure channel with an Estonian ID-card
     *
     * @param CAN    the card access number
     */
    private byte[][] PACE(byte[] CAN) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        // select the IAS-ECC application on the chip
        getResponse(selectMaster, "Select the master application");

        // initiate PACE
        getResponse(MSESetAT, "Set authentication template");

        // get nonce
        byte[] response = getResponse(GAGetNonce, "Get nonce");
        byte[] decryptedNonce = decryptNonce(Arrays.copyOfRange(response, 4, response.length - 2), CAN);

        // generate an EC keypair and exchange public keys with the chip
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        BigInteger privateKey = new BigInteger(255, new SecureRandom()).add(BigInteger.ONE); // should be in [1, spec.getN()-1], but this is good enough for this application
        ECPoint publicKey = spec.getG().multiply(privateKey).normalize();
        response = getResponse(createAPDU(GAMapNonceIncomplete, publicKey.getEncoded(false), 66), "Map nonce");
        ECPoint cardPublicKey = spec.getCurve().decodePoint(Arrays.copyOfRange(response, 4, 69));

        // calculate the new base point, use it to generate a new keypair, and exchange public keys
        ECPoint sharedSecret = cardPublicKey.multiply(privateKey);
        ECPoint mappedECBasePoint = spec.getG().multiply(new BigInteger(1, decryptedNonce)).add(sharedSecret).normalize();
        privateKey = new BigInteger(255, new SecureRandom()).add(BigInteger.ONE);
        publicKey = mappedECBasePoint.multiply(privateKey).normalize();
        response = getResponse(createAPDU(GAKeyAgreementIncomplete, publicKey.getEncoded(false), 66), "Key agreement");
        cardPublicKey = spec.getCurve().decodePoint(Arrays.copyOfRange(response, 4, 69));

        // generate the session keys and exchange MACs to verify them
        byte[] secret = cardPublicKey.multiply(privateKey).normalize().getAffineXCoord().getEncoded();
        byte[] keyEnc = createKey(secret, (byte) 1);
        byte[] keyMAC = createKey(secret, (byte) 2);
        byte[] MAC = getMAC(createAPDU(dataForMACIncomplete, cardPublicKey.getEncoded(false), 65), keyMAC);
        response = getResponse(createAPDU(GAMutualAuthenticationIncomplete, MAC, 9), "Mutual authentication");

        // verify chip's MAC and return session keys
        MAC = getMAC(createAPDU(dataForMACIncomplete, publicKey.getEncoded(false), 65), keyMAC);
        if (!Hex.toHexString(response, 4, 8).equals(Hex.toHexString(MAC))) {
            throw new RuntimeException("Could not verify chip's MAC."); // *Should* never happen.
        }
        return new byte[][]{keyEnc, keyMAC};

    }

    /**
     * Selects a file and reads its contents
     *
     * @param FID   file identifier of the required file
     * @param info  string for logging
     * @return decrypted file contents
     */
    private byte[] readFile(byte[] FID, String info) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
        selectFile(FID, info);
        //byte[] APDU = createSecureAPDU(CMD_READ_BINARY[1], CMD_READ_BINARY[2], CMD_READ_BINARY[3], null);
        Result result = communicate(CMD_READ_BINARY, null);
        return result.data;
        //byte[] response = getResponse(new byte[0], readFile, "Read binary");
        //if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
        //    throw new RuntimeException(String.format("Could not read %s", info));
        //}
        //return encryptDecryptData(Arrays.copyOfRange(response, 3, 19), Cipher.DECRYPT_MODE);
    }

    /**
     * Encrypts or decrypts the APDU data
     *
     * @param data   the array containing the data to be processed
     * @param mode   indicates whether to en- or decrypt the data
     * @return the result of encryption or decryption
     */
    private byte[] encryptDecryptData(byte[] data, int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyEnc, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] iv = Arrays.copyOf(cipher.doFinal(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ssc}), 16);
        cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(mode, secretKeySpec, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    /**
     * Constructs APDUs suitable for the secure channel.
     *
     * @param data       the data to be encrypted
     * @param incomplete the array to be used as a template
     * @return the constructed APDU
     */
    private byte[] createSecureAPDU(byte[] data, byte[] incomplete) throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        ssc++;
        byte[] encryptedData = new byte[0];
        int length = 16 * (1 + data.length / 16);

        // construct the required array and calculate the MAC based on it
        byte[] macData = new byte[data.length > 0 ? 48 + length : 48];
        macData[15] = ssc; // first block contains the ssc
        System.arraycopy(incomplete, 0, macData, 16, 4); // second block has the command
        macData[20] = (byte) 0x80; // elements are terminated by 0x80 and zero-padded to the next block
        System.arraycopy(incomplete, 5, macData, 32, 3); // third block contains appropriately encapsulated data/Le
        if (data.length > 0) { // if the APDU has data, add padding and encrypt it
            byte[] paddedData = Arrays.copyOf(data, length);
            paddedData[data.length] = (byte) 0x80;
            encryptedData = encryptDecryptData(paddedData, Cipher.ENCRYPT_MODE);
            System.arraycopy(encryptedData, 0, macData, 35, encryptedData.length);
        }
        macData[35 + encryptedData.length] = (byte) 0x80;
        Timber.log(Log.DEBUG, "MAC2:%s", Hex.toHexString(macData));
        byte[] MAC = getMAC(macData, keyMAC);

        // construct the APDU using the encrypted data and the MAC
        byte[] APDU = Arrays.copyOf(incomplete, incomplete.length + encryptedData.length + MAC.length + 3);
        if (encryptedData.length > 0) {
            System.arraycopy(encryptedData, 0, APDU, incomplete.length, encryptedData.length);
        }
        System.arraycopy(new byte[]{(byte) 0x8E, 0x08}, 0, APDU, incomplete.length + encryptedData.length, 2); // MAC is encapsulated using the tag 0x8E
        System.arraycopy(MAC, 0, APDU, incomplete.length + encryptedData.length + 2, MAC.length);
        ssc++;
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
        // fixme: Is it correct encoding for zero-length payload? (Lauris)
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
        Timber.log(Log.DEBUG, "MAC1:%s", Hex.toHexString(mac_buf));
        byte[] MAC = getMAC(mac_buf, keyMAC);

        int apdu_len = 4 + 1 + 3 + enc_len + 2 + 8 + 1;
        byte[] APDU = new byte[apdu_len];
        APDU[0] = 0xc;
        APDU[1] = INS;
        APDU[2] = P1;
        APDU[3] = P2;
        APDU[4] = (byte) (apdu_len - 6);
        // fixme: Is it correct encoding for zero-length payload? (Lauris)
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

    /**
     * Selects a FILE by its identifier
     *
     */
    private void selectFile(byte[] FID, String info) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
        Timber.log(Log.DEBUG, "Select file: %s", Hex.toHexString(FID));
        byte[] response = getResponse(FID, selectFile, String.format("Select %s", info));
        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            throw new RuntimeException(String.format("Could not select %s", info));
        }
    }

    /**
     * Gets the contents of the personal data dedicated file
     *
     * @param lastBytes   the last bytes of the personal data file identifiers (0 < x < 16)
     * @return array containing the corresponding data strings
     */
    public String[] readPersonalData(byte[] lastBytes) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {

        String[] personalData = new String[lastBytes.length];
        int stringIndex = 0;

        // select the master application
        selectFile(IASECCFID, "the master application");

        // select the personal data dedicated file
        selectFile(personalDF, "the personal data DF");

        byte[] FID = Arrays.copyOf(personalDF, personalDF.length);
        // select and read the personal data elementary files
        for (byte index : lastBytes) {

            if (index > 15 || index < 1) throw new RuntimeException("Invalid personal data FID.");
            FID[1] = index;

            personalData[stringIndex++] = new String(readFile(FID, "a personal data EF"));
        }
        return personalData;

    }

    /**
     * Attempts to verify the selected PIN
     *
     * @param PIN user-provided PIN
     * @param oneOrTwo true for PIN1, false for PIN2
     */
    private void verifyPIN(byte[] PIN, boolean oneOrTwo) throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {

        selectFile(IASECCFID, "the master application");
        if (!oneOrTwo) {
            selectFile(QSCD, "the application");
        }

        // pad the PIN and use the chip for verification
        byte[] paddedPIN = Hex.decode("ffffffffffffffffffffffff");
        System.arraycopy(PIN, 0, paddedPIN, 0, PIN.length);
        byte[] response = getResponse(paddedPIN, oneOrTwo ? verifyPIN1 : verifyPIN2, "PIN verification");

        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            if (response[response.length - 2] == 0x69 && response[response.length - 1] == (byte) 0x83) {
                throw new RuntimeException("Invalid PIN. Authentication method blocked.");
            } else {
                throw new RuntimeException(String.format("Invalid PIN. Attempts left: %d.", response[response.length - 1] + 64));
            }
        }
    }

    /**
     * Retrieves the authentication or signature certificate from the chip
     *
     * @param authOrSign true for auth, false for sign cert
     * @return the requested certificate
     */
    public byte[] getCertificate(boolean authOrSign) throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {

        selectFile(IASECCFID, "the master application");

        selectFile(authOrSign ? AWP : QSCD, "the application");

        selectFile(authOrSign ? authCert : signCert, "the certificate");

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

    /**
     * Signs the authentication token hash
     *
     * @param PIN1  PIN1
     * @param token the token hash to be signed
     * @return authentication token hash signature
     */
    public byte[] authenticate(String PIN1, byte[] token) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {

        verifyPIN(PIN1.getBytes(StandardCharsets.UTF_8), true);

        selectFile(AWP, "the AWP application");

        byte[] response = getResponse(Env, MSESetEnv, "Set environment");
        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            throw new RuntimeException("Setting the environment failed.");
        }

        InternalAuthenticate[4] = (byte) (0x1d + 16 * (token.length / 16));
        InternalAuthenticate[6] = (byte) (0x11 + 16 * (token.length / 16));
        response = getResponse(token, InternalAuthenticate, "Internal Authenticate");

        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            throw new RuntimeException("Signing the token failed.");
        }

        byte[] signature = encryptDecryptData(Arrays.copyOfRange(response, 3, 115), Cipher.DECRYPT_MODE);
        int indexOfTerminator = Hex.toHexString(signature).lastIndexOf("80") / 2;

        return Arrays.copyOf(signature, indexOfTerminator);
    }


    private byte[] getResponse(byte[] data, byte[] command, String log) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
        byte[] APDU = createSecureAPDU(command[1], command[2], command[3], data);
        byte[] response = card.transceive(APDU);
        Log.i(log, Hex.toHexString(response));
        return response;
    }

    private static final byte[] selectMaster = Hex.decode("00a4040c10a000000077010800070000fe00000100");

    private static final byte[] MSESetAT = Hex.decode("0022c1a40f800a04007f0007020204020483010200");

    private static final byte[] GAGetNonce = Hex.decode("10860000027c0000");

    private static final byte[] GAMapNonceIncomplete = Hex.decode("10860000457c438141");

    private static final byte[] GAKeyAgreementIncomplete = Hex.decode("10860000457c438341");

    private static final byte[] GAMutualAuthenticationIncomplete = Hex.decode("008600000c7c0a8508");

    private static final byte[] dataForMACIncomplete = Hex.decode("7f494f060a04007f000702020402048641");

    private static final byte[] selectFile = Hex.decode("0ca4010c1d871101");

    private static final byte[] readFile = Hex.decode("0cb000000d970100");

    private static final byte[] verifyPIN1 = Hex.decode("0c2000011d871101");

    private static final byte[] verifyPIN2 = Hex.decode("0c2000851d871101");

    private static final byte[] MSESetEnv = Hex.decode("0c2241A41d871101");

    private static final byte[] Env = Hex.decode("8004FF200800840181");

    private static final byte[] InternalAuthenticate = Hex.decode("0c8800001d871101");

    private static final byte[] IASECCFID = {0x3f, 0x00};
    private static final byte[] personalDF = {0x50, 0x00};
    private static final byte[] AWP = {(byte) 0xad, (byte) 0xf1};
    private static final byte[] QSCD = {(byte) 0xad, (byte) 0xf2};
    private static final byte[] authCert = {0x34, 0x01};
    private static final byte[] signCert = {0x34, 0x1f};

}
