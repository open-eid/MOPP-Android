package ee.ria.DigiDoc.android.signature.update.nfc;

import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SERVICE_FAULT;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SID_BROADCAST_ACTION;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SID_BROADCAST_TYPE_KEY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.idcard.IdCardException;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;
import ee.ria.DigiDoc.smartid.dto.response.ServiceFault;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import okio.ByteString;
import timber.log.Timber;

public class NFCOnSubscribe implements ObservableOnSubscribe<NFCResponse> {
    private final Navigator navigator;
    private final SignedContainer container;
    private final LocalBroadcastManager broadcastManager;
    private final String uuid;
    private final String can;
    private final String pin2;

    Intent intent;

    public NFCOnSubscribe(Navigator navigator, Intent intent, SignedContainer container, String uuid,
                              String can, String pin2) {
        this.navigator = navigator;
        this.container = container;
        this.broadcastManager = LocalBroadcastManager.getInstance(navigator.activity());
        this.uuid = uuid;
        this.can = can;
        this.pin2 = pin2;

        this.intent = intent;
    }

    @Override
    public void subscribe(ObservableEmitter<NFCResponse> emitter) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (navigator.activity() == null) {
                    Timber.log(Log.ERROR, "Activity is null");
                    IllegalStateException ise = new IllegalStateException("Activity not found. Please try again after restarting application");
                    emitter.onError(ise);
                }
                switch (intent.getStringExtra(SID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT:
                        NotificationManagerCompat.from(navigator.activity()).cancelAll();
                        ServiceFault serviceFault =
                                ServiceFault.fromJson(intent.getStringExtra(SERVICE_FAULT));
                        Timber.log(Log.DEBUG, "Got SERVICE_FAULT status: %s", serviceFault.getStatus());
/*                        if (serviceFault.getStatus() == NO_RESPONSE) {
                            emitter.onError(SmartIdMessageException
                                    .create(navigator.activity(), serviceFault.getStatus()));
                        } else {
                            emitter.onError(SmartIdMessageException
                                    .create(navigator.activity(), serviceFault.getStatus(), serviceFault.getDetailMessage()));
                        }
*/
                        break;
/*                    case CREATE_SIGNATURE_DEVICE:
                        Timber.log(Log.DEBUG, "Selecting device (CREATE_SIGNATURE_DEVICE)");
                        emitter.onNext(SmartIdResponse.selectDevice(true));
                        break;
                    case CREATE_SIGNATURE_CHALLENGE:
                        Timber.log(Log.DEBUG, "Signature challenge (CREATE_SIGNATURE_CHALLENGE)");
                        String challenge =
                                intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE);
                        emitter.onNext(SmartIdResponse.challenge(challenge));

                        if (!PowerUtil.isPowerSavingMode(navigator.activity())) {
                            Timber.log(Log.DEBUG, "Creating notification channel");
                            NotificationUtil.createNotificationChannel(navigator.activity(),
                                    NOTIFICATION_CHANNEL, navigator.activity()
                                            .getResources()
                                            .getString(R.string.signature_update_signature_add_method_smart_id));
                        }

                        String challengeTitle = navigator.activity()
                                .getResources().getString(R.string.smart_id_challenge);
                        Notification notification = NotificationUtil.createNotification(navigator.activity(), NOTIFICATION_CHANNEL,
                                R.mipmap.ic_launcher, challengeTitle, challenge,
                                NotificationCompat.PRIORITY_HIGH, false);

                        sendNotification(navigator.activity(), challenge, notification);

                        break;
                    case CREATE_SIGNATURE_STATUS:
                        NotificationManagerCompat.from(navigator.activity()).cancelAll();
                        if (status.getStatus() == SessionStatusResponse.ProcessStatus.OK) {
                            Timber.log(Log.DEBUG, "Got CREATE_SIGNATURE_STATUS success status: %s", status.getStatus());
                            emitter.onNext(SmartIdResponse.success(container));
                            emitter.onComplete();
                        } else {
                            Timber.log(Log.DEBUG, "Got CREATE_SIGNATURE_STATUS error status: %s", status.getStatus());
                            emitter.onError(SmartIdMessageException
                                    .create(navigator.activity(), status.getStatus()));
                        }
                        break;
  */
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(SID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        ConfigurationProvider configurationProvider =
                ((Application) navigator.activity().getApplication()).getConfigurationProvider();
        String displayMessage = navigator.activity()
                .getString(R.string.signature_update_mobile_id_display_message);
/*        SmartIDSignatureRequest request = SmartCreateSignatureRequestHelper
                .create(container, uuid, configurationProvider.getSidV2RestUrl(),
                        configurationProvider.getSidV2SkRestUrl(), country,
                        personalCode, displayMessage);

        intent.putExtra(CREATE_SIGNATURE_REQUEST, request);
        intent.putStringArrayListExtra(CERTIFICATE_CERT_BUNDLE,
                new ArrayList<>(configurationProvider.getCertBundle()));
*/
        // fixme: Do we need service?
        //navigator.activity().startService(intent);
        Timber.log(Log.DEBUG, "Handling NFC sign intent");
        NfcManager manager = (NfcManager) navigator.activity().getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Timber.log(Log.ERROR, "NFC is not enabled");
            // fixme: Send error observable
            return;
        }
        Timber.log(Log.DEBUG, "Successfully created NFC adapter");
            adapter.enableReaderMode(navigator.activity(),
                    tag -> onTagDiscovered(adapter, tag),
                    NfcAdapter.FLAG_READER_NFC_A, null);
/*        adapter.enableReaderMode(navigator.activity(), { tag ->
                requireActivity().runOnUiThread {
                binding.detectionActionText.text = getString(R.string.card_detected)
        }
                val card = IsoDep.get(tag)
                card.timeout = 32768
                card.use {
        try {
            val comms = Comms(it, viewModel.userCan)
            val response = comms.readPersonalData(byteArrayOf(1, 2, 6, 3, 4, 8))
            viewModel.setUserFirstName(response[1])
            viewModel.setUserLastName(response[0])
            viewModel.setUserIdentificationNumber(response[2])
            viewModel.setGender(response[3])
            viewModel.setCitizenship(response[4])
            viewModel.setExpiration(response[5])
            requireActivity().runOnUiThread {
                val action = HomeFragmentDirections.actionHomeFragmentToUserFragment()
                findNavController().navigate(action)
            }
        } catch (e: Exception) {
            when (e) {
                is TagLostException -> requireActivity().runOnUiThread {
                    binding.detectionActionText.text =
                            getString(R.string.id_card_removed_early)
                    reset()
                }
                            else -> requireActivity().runOnUiThread {
                    binding.detectionActionText.text =
                            getString(R.string.nfc_reading_error)
                    viewModel.deleteCan(requireContext())
                    canState()
                    reset()
                }
            }
        } finally {
            adapter.disableReaderMode(activity)
        }
                }
            }, NfcAdapter.FLAG_READER_NFC_A, null)
*/
    }

    private byte[] keyEnc;
    private byte[] keyMAC;

    private static final byte[] SEL_MAIN_AID_CMD = Hex.decode("00A404001d871101");
    private static final byte[] SEL_MAIN_AID = Hex.decode("A000000077010800070000FE00000100");
    private static final byte[] IASECCFID = Hex.decode("3F00");
    private static final byte[] QSCD = Hex.decode("ADF2");
    private static final byte[] signCert = Hex.decode("341f");

    private static final byte[] SEL_QSCD0 = Hex.decode("0CA4040C2d872101");
    private static final byte[] SEL_QSCD_CMD = Hex.decode("00A4040C");
    private static final byte[] SEL_QSCD = Hex.decode("51534344204170706C69636174696F6E");

    //private static final byte[] VER_PIN2_CMD = Hex.decode("0c2000851d871101");
    private static final byte[] VER_PIN2_CMD = Hex.decode("00200085");

    private static final byte[] CMD_SET_ENV = Hex.decode("002241B6");
    private static final byte[] SET_ENV = Hex.decode("8004FF15080084019F");
    private static final byte[] CMD_COMP_SIG = Hex.decode("002A9E9A");

    @Nullable private IsoDep card;

    private void onTagDiscovered(NfcAdapter adapter, Tag tag) {
        Timber.log(Log.DEBUG, "Tag discovered: %s", tag.toString());
        card = IsoDep.get(tag);
        try {
            card.connect();
            card.setTimeout(32768);
            /* Get keys */
            byte[][] keys = PACE(card, can.getBytes(StandardCharsets.UTF_8));
            keyEnc = keys[0];
            keyMAC = keys[1];

            //byte[] vals = {1,2,6,3,4,8};
            //String[] response = readPersonalData(card, vals);
            //for (String res : response) {
            //    Timber.log(Log.DEBUG, res);
            //}
            // Test sign

            // Step 1 - get certificate
            selectFile(card, IASECCFID, "the master application");
            selectFile(card, QSCD, "the application");
            selectFile(card, signCert, "the certificate");

            byte[] certificate = new byte[0];
            byte[] readCert = Arrays.copyOf(readFile, readFile.length);
            // Construct the certificate byte array n=indexOfTerminator bytes at a time
            for (int i = 0; i < 16; i++) {

                // Set the P1/P2 values to incrementally read the certificate
                readCert[2] = (byte) (certificate.length / 256);
                readCert[3] = (byte) (certificate.length % 256);
                byte[] response = getResponse(card, new byte[0], readCert);
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
            Timber.log(Log.DEBUG, Hex.toHexString(certificate));
            Certificate cert = Certificate.create(ByteString.of(certificate, 0, certificate.length));

            // Step 2 - verify PIN2
            Result r = communicate(card, SEL_QSCD_CMD, SEL_QSCD);
            Timber.log(Log.DEBUG, "Select QSCD AID:%x %s", r.code, Hex.toHexString(r.msg));

            // pad the PIN and use the chip for verification
            byte[] paddedPIN = Hex.decode("ffffffffffffffffffffffff");
            byte[] pin2b = pin2.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(pin2b, 0, paddedPIN, 0, pin2b.length);
            r = communicate(card, VER_PIN2_CMD, paddedPIN);
            Timber.log(Log.DEBUG, "Verify PIN2:%x %s", r.code, Hex.toHexString(r.msg));
            if (r.code != 0x9000) {
                if (r.code == 0x6983) {
                    throw new RuntimeException("Invalid PIN. Authentication method blocked.");
                } else {
                    throw new RuntimeException(String.format("Invalid PIN. Attempts left: %d.", (r.code & 0xff) + 64));
                }
            }

            r = communicate(card, CMD_SET_ENV, SET_ENV);
            Timber.log(Log.DEBUG, "Set ENV:%x %s", r.code, Hex.toHexString(r.msg));


            container.sign(cert.data(),
                    signData -> ByteString.of(calculateSignature(pin2.getBytes(StandardCharsets.US_ASCII),
                    signData.toByteArray(),
                    cert.ellipticCurve())));
        } catch (TagLostException exc) {
            Timber.log(Log.ERROR, exc.getMessage());
        } catch (IOException exc) {
            Timber.log(Log.ERROR, exc.getMessage());
        } catch (Exception exc) {
            Timber.log(Log.ERROR, exc.getMessage());
        } finally {
            adapter.disableReaderMode(navigator.activity());
            card = null;
        }

    }

    public byte[] calculateSignature(byte[] pin2, byte[] hash, boolean ecc) throws SmartCardReaderException {
        Result r = communicate(card, CMD_COMP_SIG, hash);
        Timber.log(Log.DEBUG, "Sign response:%x %s", r.code, Hex.toHexString(r.msg));
        byte[] dec = encryptDecryptData(Arrays.copyOfRange(r.data, 3, r.data.length - 16), Cipher.DECRYPT_MODE);
        Timber.log(Log.DEBUG, "Decrypted: %s", Hex.toHexString(dec));
        dec = removePadding(dec);
        Timber.log(Log.DEBUG, "Signature: %s", Hex.toHexString(dec));
        return dec;
    }

    /**
     * ID1 only has ECC keys so we don't need to pad it as we do RSA hashes,
     * but we need to pad hashes that are smaller than the key size with zeroes in front to resize
     * them to 48bytes in length because of API restrictions on the chip
     *
     * @param hash that needs to be signed
     * @return zero padded hash with 48 byte length or same hash if it's longer than 48 bytes
     * @throws IdCardException when padding the hash fails
     */
    private static byte[] padWithZeroes(byte[] hash) throws IdCardException {
        if (hash.length < 48) {
            try (ByteArrayOutputStream toSign = new ByteArrayOutputStream()) {
                toSign.write(new byte[48 - hash.length]);
                toSign.write(hash);
                return toSign.toByteArray();
            } catch (IOException exc) {
                Timber.log(Log.ERROR, exc.getMessage());
            }
        }
        return hash;
    }

    private static final byte[] personalDF = {0x50, 0x00};

    /**
     * Gets the contents of the personal data dedicated file
     *
     * @param lastBytes   the last bytes of the personal data file identifiers (0 < x < 16)
     * @return array containing the corresponding data strings
     */
    private String[] readPersonalData(IsoDep card, byte[] lastBytes) {

        String[] personalData = new String[lastBytes.length];
        int stringIndex = 0;

        // select the master application
        selectFile(card, IASECCFID, "the master application");

        // select the personal data dedicated file
        selectFile(card, personalDF, "Set authentication template");

        byte[] FID = Arrays.copyOf(personalDF, personalDF.length);
        // select and read the personal data elementary files
        for (byte index : lastBytes) {

            if (index > 15 || index < 1) throw new RuntimeException("Invalid personal data FID.");
            FID[1] = index;

            // store the decrypted datum
            byte[] response = readFile(card, FID, "a personal data EF");
            int indexOfTerminator = Hex.toHexString(response).lastIndexOf("80") / 2;
            personalData[stringIndex++] = new String(Arrays.copyOfRange(response, 0, indexOfTerminator));

        }
        return personalData;

    }

    private final byte[] removePadding(byte[] data) {
        int idx = data.length - 1;
        while ((idx > 0) && (data[idx] != (byte) 0x80)) idx -= 1;
        return Arrays.copyOfRange(data, 0, idx);
    }

    private static final byte[] selectFile = Hex.decode("0ca4010c1d871101");

    private void selectFile(IsoDep card, byte[] FID, String info) {
        byte[] response = getResponse(card, FID, selectFile);
        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            throw new RuntimeException(String.format("Could not select %s", info));
        }
    }

    private byte[] getResponse(IsoDep card, byte[] APDU) {
        try {
            byte[] response = card.transceive(APDU);
            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
                throw new RuntimeException(String.format("%s failed.", "NFCOnSubscribe.getResponse"));
            }
            Timber.log(Log.DEBUG, Hex.toHexString(response));
            return response;
        } catch (Exception exc) {
            Timber.log(Log.DEBUG, exc.getMessage());
            return null;
        }
    }

    private byte[] getResponse(IsoDep card, byte[] data, byte[] command) {
        try {
            byte[] response = card.transceive(createSecureAPDU(data, command));
            Timber.log(Log.DEBUG, Hex.toHexString(response));
            return response;
        } catch (Exception exc) {
            Timber.log(Log.DEBUG, exc.getMessage());
            return null;
        }
    }

    private final class Result {
        public final byte[] data;
        public final byte[] msg;
        public final int code;
        public Result(byte[] data) {
            this.data = data;
            if ((data != null) && (data.length >= 2)) {
                msg = new byte[data.length - 2];
                System.arraycopy(data, 0, msg, 0, data.length - 2);
                code = (((int) data[data.length - 2] & 0xff) << 8) | (int) (data[data.length - 1] & 0xff);
            } else {
                msg = new byte[0];
                code = 0;
            }
        }
    }

    private Result communicate(IsoDep card, byte[] command, byte[] data) {
        try {
            byte[] APDU = createAPDU(command, data);
            Timber.log(Log.DEBUG, "APDU:%s", Hex.toHexString(APDU));
            byte[] response = card.transceive(APDU);
            Timber.log(Log.DEBUG, "Response:%s", Hex.toHexString(response));
            return new Result(response);
        } catch (Exception exc) {
            Timber.log(Log.DEBUG, exc.getMessage());
            return new Result(null);
        }
    }

    /* Sequence counter */
    private byte ssc;
    /**
     * Constructs APDUs suitable for the secure channel.
     *
     * @param data       the data to be encrypted
     * @param incomplete the array to be used as a template
     * @return the constructed APDU
     */
    private byte[] createSecureAPDU(byte[] data, byte[] incomplete) {

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
        //Timber.log(Log.DEBUG, Hex.toHexString(macData));
        byte[] MAC = getMAC(macData, keyMAC);

        // construct the APDU using the encrypted data and the MAC
        byte[] APDU = Arrays.copyOf(incomplete, incomplete.length + encryptedData.length + MAC.length + 3);
        if (encryptedData.length > 0) {
            System.arraycopy(encryptedData, 0, APDU, incomplete.length, encryptedData.length);
            APDU[incomplete.length - 2] = (byte) (encryptedData.length + 1); // 01 padding-indicator
        }
        System.arraycopy(new byte[]{(byte) 0x8E, 0x08}, 0, APDU, incomplete.length + encryptedData.length, 2); // MAC is encapsulated using the tag 0x8E
        System.arraycopy(MAC, 0, APDU, incomplete.length + encryptedData.length + 2, MAC.length);
        APDU[4] = (byte) (APDU.length - 6);
        ssc++;
        //Timber.log(Log.DEBUG, "APDU:%s", Hex.toHexString(APDU));
        return APDU;

    }

    // Create secure APDU
    // Command has ty be 4 bytes (CLA, INS, P1, P2) CLA is overwritten by 0xc
    // Data can be null or zero-length array

    private byte[] createAPDU(@Nonnull byte[] cmd, @Nullable byte[] data) {
        ssc += 1;

        byte[] enc_data;
        int enc_len;
        if ((data == null) && (data.length == 0)) {
            enc_len = 0;
            enc_data = null;
        } else {
            enc_len = 16 * (1 + data.length / 16);
            byte[] padded_data = Arrays.copyOf(data, enc_len);
            padded_data[data.length] = (byte) 0x80;
            enc_data = encryptDecryptData(padded_data, Cipher.ENCRYPT_MODE);
        }

        // MAC
        // 0-15: 0..., ssc
        // 16-31: CMD, 0x80, 0...
        // 32: ENC_DATA header (0X87)
        // 33: ENC_DATA length
        // 34: Padding indicator (1)
        // 35-...: ENC_DATA, 0x80
        // 35+ENC_LEN: 0x80
        // 35+ENC_LEN+1-...: 0
        byte[] mac = new byte[48 + enc_len];
        mac[15] = ssc;
        System.arraycopy(cmd, 0, mac, 16, 4);
        mac[16] = (byte) 0xc;
        mac[20] = (byte) 0x80;
        mac[32] = (byte) 0x87;
        mac[33] = (byte) (enc_len + 1);
        mac[34] = (byte) 1;
        if (enc_len > 0) System.arraycopy(enc_data, 0, mac, 35, enc_len);
        mac[35 + enc_len] = (byte) 0x80;

        byte[] MAC = getMAC(mac, keyMAC);

        // APDU
        // 0-4: 4 bytes CMD
        // 4: length (excluding header, len and terminating 0)
        // 5: ENC_DATA header (0x87)
        // 6: ENC_DATA length
        // 7:
        // 8-...: ENC_DATA
        // 8+ENC_LEN: MAC header (0x8e)
        // 8+ENC_LEN+1: MAC length (8)
        // 8+ENC_LEN+2-...: MAC
        // 8+ENC_LEN+2+MAC_LEN: 0
        byte[] APDU = Arrays.copyOf(cmd, 8 + enc_len + 2 + MAC.length + 1);
        APDU[0] = (byte) 0xc;
        APDU[4] = (byte) (APDU.length - 6);
        APDU[5] = (byte) 0x87;
        APDU[6] = (byte) (enc_len + 1);
        APDU[7] = (byte) 1;
        if (enc_len > 0) System.arraycopy(enc_data, 0, APDU, 8, enc_len);
        APDU[8 + enc_len] = (byte) 0x8e;
        APDU[8 + enc_len + 1] = (byte) 8;
        System.arraycopy(MAC, 0, APDU, 8 + enc_len + 2, MAC.length);

        ssc += 1;

        return APDU;
    }

    /**
     * Encrypts or decrypts the APDU data
     *
     * @param data   the array containing the data to be processed
     * @param mode   indicates whether to en- or decrypt the data
     * @return the result of encryption or decryption
     */

    private byte[] encryptDecryptData(byte[] data, int mode) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyEnc, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] iv = Arrays.copyOf(cipher.doFinal(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ssc}), 16);
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(mode, secretKeySpec, new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (Exception exc) {
            Timber.log(Log.DEBUG, exc.getMessage());
            return data;
        }
    }

    private static final byte[] selectMaster = Hex.decode("00a4040c10a000000077010800070000fe00000100");
    private static final byte[] MSESetAT = Hex.decode("0022c1a40f800a04007f0007020204020483010200");
    private static final byte[] GAGetNonce = Hex.decode("10860000027c0000");
    private static final byte[] GAMapNonceIncomplete = Hex.decode("10860000457c438141");
    private static final byte[] GAKeyAgreementIncomplete = Hex.decode("10860000457c438341");
    private static final byte[] dataForMACIncomplete = Hex.decode("7f494f060a04007f000702020402048641");
    private static final byte[] GAMutualAuthenticationIncomplete = Hex.decode("008600000c7c0a8508");

    /**
     * Attempts to use the PACE protocol to create a secure channel with an Estonian ID-card
     *
     * @param CAN    the card access number
     */
    private byte[][] PACE(IsoDep card, byte[] CAN) {

        // select the IAS-ECC application on the chip
        getResponse(card, selectMaster);

        // initiate PACE
        getResponse(card, MSESetAT);

        // get nonce
        byte[] response = getResponse(card, GAGetNonce);
        byte[] decryptedNonce = decryptNonce(Arrays.copyOfRange(response, 4, response.length - 2), CAN);

        // generate an EC keypair and exchange public keys with the chip
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        BigInteger privateKey = new BigInteger(255, new SecureRandom()).add(BigInteger.ONE); // should be in [1, spec.getN()-1], but this is good enough for this application
        ECPoint publicKey = spec.getG().multiply(privateKey).normalize();
        response = getResponse(card, createAPDU(GAMapNonceIncomplete, publicKey.getEncoded(false), 66));
        ECPoint cardPublicKey = spec.getCurve().decodePoint(Arrays.copyOfRange(response, 4, 69));

        // calculate the new base point, use it to generate a new keypair, and exchange public keys
        ECPoint sharedSecret = cardPublicKey.multiply(privateKey);
        ECPoint mappedECBasePoint = spec.getG().multiply(new BigInteger(1, decryptedNonce)).add(sharedSecret).normalize();
        privateKey = new BigInteger(255, new SecureRandom()).add(BigInteger.ONE);
        publicKey = mappedECBasePoint.multiply(privateKey).normalize();
        response = getResponse(card, createAPDU(GAKeyAgreementIncomplete, publicKey.getEncoded(false), 66));
        cardPublicKey = spec.getCurve().decodePoint(Arrays.copyOfRange(response, 4, 69));

        // generate the session keys and exchange MACs to verify them
        byte[] secret = cardPublicKey.multiply(privateKey).normalize().getAffineXCoord().getEncoded();
        byte[] keyEnc = createKey(secret, (byte) 1);
        byte[] keyMAC = createKey(secret, (byte) 2);
        byte[] MAC = getMAC(createAPDU(dataForMACIncomplete, cardPublicKey.getEncoded(false), 65), keyMAC);
        response = getResponse(card, createAPDU(GAMutualAuthenticationIncomplete, MAC, 9));

        // verify chip's MAC and return session keys
        MAC = getMAC(createAPDU(dataForMACIncomplete, publicKey.getEncoded(false), 65), keyMAC);
        if (!Hex.toHexString(response, 4, 8).equals(Hex.toHexString(MAC))) {
            throw new RuntimeException("Could not verify chip's MAC."); // *Should* never happen.
        }
        return new byte[][]{keyEnc, keyMAC};

    }

    /**
     * Decrypts the nonce
     *
     * @param encryptedNonce the encrypted nonce received from the chip
     * @param CAN            the card access number provided by the user
     * @return the decrypted nonce
     */
    private byte[] decryptNonce(byte[] encryptedNonce, byte[] CAN) {
        try {
            byte[] decryptionKey = createKey(CAN, (byte) 3);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptionKey, "AES"), new IvParameterSpec(new byte[16]));
            return cipher.doFinal(encryptedNonce);
        } catch (Exception exc) {
            Timber.log(Log.DEBUG, exc.getMessage());
            return encryptedNonce;
        }
    }

    /**
     * Creates a cipher key
     *
     * @param unpadded the array to be used as the basis for the key
     * @param last     the last byte in the appended padding
     * @return the constructed key
     */
    private byte[] createKey(byte[] unpadded, byte last) {
        byte[] padded = Arrays.copyOf(unpadded, unpadded.length + 4);
        padded[padded.length - 1] = last;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(padded);
        } catch (Exception exc) {
            Timber.log(Log.DEBUG, exc.getMessage());
            return unpadded;
        }
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

    private static final byte[] readFile = Hex.decode("0cb000000d970100");

    /**
     * Selects a file and reads its contents
     *
     * @param FID   file identifier of the required file
     * @param info  string for logging
     * @return decrypted file contents
     */
    private byte[] readFile(IsoDep card, byte[] FID, String info) {
        selectFile(card, FID, info);
        byte[] response = getResponse(card, new byte[0], readFile);
        if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != 0x00) {
            throw new RuntimeException(String.format("Could not read %s", info));
        }
        return encryptDecryptData(Arrays.copyOfRange(response, 3, 19), Cipher.DECRYPT_MODE);
    }
}
