package ee.ria.token.tokenservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.token.tokenservice.callback.UnblockPinCallback;

public class TokenService extends Service {

    private static final String TAG = "TokenService";

    private EstEIDToken eidToken;
    private final IBinder mBinder = new LocalBinder();
    private SMInterface sminterface;

    public void sign(Token.PinType pinType, String pin, byte[] hashToSignHex, SignCallback callback) {
        Log.d(TAG, "sign: with pin: " + pin);
        eidToken.sign(pinType, hashToSignHex, pin, callback);
    }

    public class LocalBinder extends Binder {
        public TokenService getService() {
            return TokenService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        connectToReader();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(sminterface.getReciever());
        Toast.makeText(this, "Service destroyed, unregisterreceiver called", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    public void readPersonalFile(PersonalFileCallback callback) {
        try {
            SparseArray<String> result = eidToken.readPersonalFile();
            callback.onPersonalFileResponse(result);
        } catch (Exception e) {
            Log.e(TAG, "readPersonalFile: ", e);
            callback.onPersonalFileError(e.getMessage());
        }
    }

    public void changePin(Token.PinType pinType, String oldPin, String newPin, ChangePinCallback callback) {
        try {
            boolean changed = eidToken.changePin(oldPin.getBytes(), newPin.getBytes(), pinType);
            if (changed) {
                callback.success();
            } else {
                callback.error(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "changePin: ", e);
            callback.error(e);
        }
    }

    public void unblockPin(Token.PinType pinType, String puk, UnblockPinCallback callback) {
        try {
            boolean unblocked = eidToken.unblockPin(puk.getBytes(), pinType);
            if (unblocked) {
                callback.success();
            } else {
                callback.error(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "unblockPin: ", e);
            callback.error(e);
        }
    }

    public void readCertificateInHex(Token.CertType type, CertCallback certCallback) {
        try {
            byte[] certBytes = eidToken.readCert(type);
            certCallback.onCertificateResponse(certBytes);
        } catch (Exception e) {
            Log.e(TAG, "readCertificateInHex: ", e);
            certCallback.onCertificateError(e.getMessage());
        }
    }

    private void connectToReader() {
        sminterface = SMInterface.getInstance(this, SMInterface.ACS);
        if (sminterface == null) {
            return;
        }
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                Log.i("SIMINTERFACE_CONNECT", "connected!");
            }
        });
        eidToken = new EstEIDToken(sminterface);
    }

}
