package ee.ria.DigiDoc.android.signature.update.nfc;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.annotation.Nullable;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.idcard.NFC;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import okio.ByteString;
import timber.log.Timber;

public class NFCOnSubscribe implements ObservableOnSubscribe<NFCResponse> {
    private final Navigator navigator;
    private final SignedContainer container;
    private final String can;
    private final byte[] pin2;
    private final RoleData role;
    private NFC nfc;

    public NFCOnSubscribe(Navigator navigator, SignedContainer container,
                              String can, byte[] pin2, @Nullable RoleData roleData) {
        this.navigator = navigator;
        this.container = container;
        this.can = can;
        this.pin2 = pin2;
        this.role = roleData;
    }

    @Override
    public void subscribe(ObservableEmitter<NFCResponse> emitter) {
        Timber.log(Log.DEBUG, "Handling NFC sign intent");
        NfcManager manager = (NfcManager) navigator.activity().getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Timber.log(Log.ERROR, "NFC is not enabled");
            emitter.onNext(NFCResponse.createWithStatus(SessionStatusResponse.ProcessStatus.GENERAL_ERROR, navigator.activity().getString(R.string.signature_update_nfc_adapter_missing)));
        } else {
            Timber.log(Log.DEBUG, "Successfully created NFC adapter");
            adapter.enableReaderMode(navigator.activity(),
                    tag -> {
                        if (AccessibilityUtils.isTalkBackEnabled()) {
                            AccessibilityUtils.interrupt(navigator.activity());
                        }
                        emitter.onNext(NFCResponse.createWithStatus(SessionStatusResponse.ProcessStatus.OK, navigator.activity().getString(R.string.signature_update_nfc_detected)));
                        NFCResponse response = onTagDiscovered(adapter, tag);
                        Timber.log(Log.DEBUG, "NFC::completed");
                        if (response != null) {
                            if (response.status() != SessionStatusResponse.ProcessStatus.OK) {
                                Timber.log(Log.ERROR, String.format("NFC status: %s", response.status()));
                                emitter.onError(NFCResponse.createException(response.message()));
                                emitter.onComplete();
                            } else {
                                emitter.onNext(response);
                                emitter.onComplete();
                            }
                        } else {
                            emitter.onNext(NFCResponse.success(container));
                            emitter.onComplete();
                        }
                    }, NfcAdapter.FLAG_READER_NFC_A, null);
        }
    }

    private static final byte[] SEL_QSCD_CMD = Hex.decode("00A4040C");
    private static final byte[] SEL_QSCD = Hex.decode("51534344204170706C69636174696F6E");

    //private static final byte[] VER_PIN2_CMD = Hex.decode("0c2000851d871101");
    private static final byte[] VER_PIN2_CMD = Hex.decode("00200085");

    private static final byte[] CMD_SET_ENV_SIGN = Hex.decode("002241B6");
    private static final byte[] SET_ENV_SIGN = Hex.decode("8004FF15080084019F");

    @Nullable private IsoDep card;

    private NFCResponse onTagDiscovered(NfcAdapter adapter, Tag tag) {
        Timber.log(Log.DEBUG, "Tag discovered: %s", tag.toString());
        card = IsoDep.get(tag);
        NFCResponse result = null;
        try {
            card.connect();
            card.setTimeout(32768);
            nfc = new NFC(card, can.getBytes(StandardCharsets.UTF_8));

            // Step 1 - get certificate
            byte[] certificate = nfc.readCertificate();
            if (certificate == null) {
                Timber.log(Log.ERROR, "Failed to read certificate. Certificate is null");
                throw new RuntimeException(navigator.activity().getString(R.string.signature_update_nfc_technical_error));
            }
            Timber.log(Log.DEBUG, "CERT: %s", Hex.toHexString(certificate));
            Certificate cert = Certificate.create(ByteString.of(certificate, 0, certificate.length));

            // Step 2 - verify PIN2
            NFC.Result r = nfc.communicateSecure(SEL_QSCD_CMD, SEL_QSCD);
            Timber.log(Log.DEBUG, "Select QSCD AID: %x %s", r.code, Hex.toHexString(r.data));

            // pad the PIN and use the chip for verification
            byte[] paddedPIN = Hex.decode("ffffffffffffffffffffffff");
            System.arraycopy(pin2, 0, paddedPIN, 0, pin2.length);
            r = nfc.communicateSecure(VER_PIN2_CMD, paddedPIN);
            if (null != pin2 && pin2.length > 0) {
                Arrays.fill(pin2, (byte) 0);
            }
            Timber.log(Log.DEBUG, "Verify PIN2: %x %s", r.code, Hex.toHexString(r.data));
            if (r.code != 0x9000) {
                if (r.code == 0x6983) {
                    throw new RuntimeException(navigator.activity().getString(R.string.signature_update_id_card_sign_pin2_locked));
                } else if ((r.code & 0xfff0) == 0x63c0) {
                    throw new RuntimeException(String.format(navigator.activity().getString(R.string.signature_update_id_card_sign_pin2_invalid), r.code & 0xf));
                } else {
                    Timber.log(Log.ERROR, "PIN2 verification error");
                    throw new RuntimeException(navigator.activity().getString(R.string.signature_update_nfc_technical_error));
                }
            }

            r = nfc.communicateSecure(CMD_SET_ENV_SIGN, SET_ENV_SIGN);
            Timber.log(Log.DEBUG, "Set ENV: %x %s", r.code, Hex.toHexString(r.data));

            container.sign(navigator.activity(), cert.data(),
                    signData -> ByteString.of(nfc.calculateSignature(signData.toByteArray())), role);
        } catch (TagLostException exc) {
            Timber.log(Log.ERROR, exc.getMessage());
            result = NFCResponse.createWithStatus(SessionStatusResponse.ProcessStatus.GENERAL_ERROR, navigator.activity().getString(R.string.signature_update_nfc_tag_lost));
        } catch (IOException exc) {
            Timber.log(Log.ERROR, exc.getMessage());
            result = NFCResponse.createWithStatus(SessionStatusResponse.ProcessStatus.TECHNICAL_ERROR, exc.getMessage());
        } catch (Exception exc) {
            Timber.log(Log.ERROR, exc.getMessage());
            result = NFCResponse.createWithStatus(SessionStatusResponse.ProcessStatus.TECHNICAL_ERROR, exc.getMessage());
        } finally {
            adapter.disableReaderMode(navigator.activity());
            card = null;
        }
        return result;
    }
}
