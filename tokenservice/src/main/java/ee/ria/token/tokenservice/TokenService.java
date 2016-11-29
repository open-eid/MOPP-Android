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
import ee.ria.token.tokenservice.util.TokenFactory;

public class TokenService extends Service {

    private static final String TAG = "TokenService";

    private final IBinder mBinder = new LocalBinder();

    private Token token;
    private SMInterface sminterface;

    public class LocalBinder extends Binder {
        public TokenService getService() {
            return TokenService.this;
        }
    }

    class SMConnected extends SMInterface.Connected {

        @Override
        public void connected() {
            Log.i("SIMINTERFACE_CONNECT", "connected!");

            try {
                sminterface.transmitExtended(new byte[]{0x00, (byte) 0xA4, 0x00, 0x0C});
                byte[] versionBytes = sminterface.transmitExtended(new byte[]{0x00, (byte) 0xCA, 0x01, 0x00, 0x03});
                token = TokenFactory.getTokenImpl(versionBytes, sminterface);
            } catch (Exception e) {
                Log.e(TAG, "getTokenImpl: ", e);
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        SMConnected callback = new SMConnected();
        sminterface = getSIMInterface(callback);
        return mBinder;
    }

    private SMInterface getSIMInterface(SMConnected callback) {
        final SMInterface sminterface = SMInterface.getInstance(this, SMInterface.ACS);
        if (sminterface == null) {
            return null;
        }
        sminterface.connect(callback);
        return sminterface;
    }

    public void sign(Token.PinType pinType, String pin, byte[] data, SignCallback callback) {
        Log.d(TAG, "sign: with pin: " + pin);
        try {
            byte[] sign = token.sign(pinType, pin, data);
            callback.onSignResponse(sign);
        } catch (Exception e) {
            Log.e(TAG, "sign: ", e);
            callback.onSignError(e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        if (sminterface != null) {
            this.unregisterReceiver(sminterface.getReciever());
            Toast.makeText(this, "Service destroyed, unregisterreceiver called", Toast.LENGTH_LONG).show();
        }
        super.onDestroy();
    }

    public void readPersonalFile(PersonalFileCallback callback) {
        try {
            SparseArray<String> result = token.readPersonalFile();
            callback.onPersonalFileResponse(result);
        } catch (Exception e) {
            Log.e(TAG, "readPersonalFile: ", e);
            callback.onPersonalFileError(e.getMessage());
        }
    }

    public void changePin(Token.PinType pinType, String currentPin, String newPin, ChangePinCallback callback) {
        try {
            boolean changed = token.changePin(pinType, currentPin.getBytes(), newPin.getBytes());
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
            boolean unblocked = token.unblockPin(pinType, puk.getBytes());
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

    public void readCert(Token.CertType type, CertCallback certCallback) {
        try {
            byte[] certBytes = token.readCert(type);
            certCallback.onCertificateResponse(certBytes);
        } catch (Exception e) {
            Log.e(TAG, "readCertificateInHex: ", e);
            certCallback.onCertificateError(e.getMessage());
        }
    }
}
