package ee.ria.token.tokenservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.token.tokenservice.callback.UnblockPinCallback;
import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.token.PinVerificationException;
import ee.ria.token.tokenservice.token.Token;

public class TokenService extends Service {

    private static final String TAG = "TokenService";

    private final IBinder mBinder = new LocalBinder();

    private Token token;
    private CardReader cardReader;

    public class LocalBinder extends Binder {
        public TokenService getService() {
            if (cardReader != null) {
                return TokenService.this;
            }
            return null;
        }
    }

    class SMConnected extends CardReader.Connected {

        @Override
        public void connected() {
            Log.i("SIMINTERFACE_CONNECT", "connected!");
            try {
                token = TokenFactory.getTokenImpl(cardReader);
            } catch (Exception e) {
                Log.e(TAG, "getTokenImpl: ", e);
            }
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        SMConnected callback = new SMConnected();
        cardReader = getCardReader(callback);
        return mBinder;
    }

    private CardReader getCardReader(SMConnected callback) {
        final CardReader cardReader = CardReader.getInstance(this, CardReader.ACS);
        if (cardReader == null) {
            return null;
        }
        cardReader.connect(callback);
        return cardReader;
    }

    public void readRetryCounter(Token.PinType pinType, RetryCounterCallback callback) {
        if (token == null) {
            callback.cardNotProvided();
            return;
        }
        try {
            byte counterByte = token.readRetryCounter(pinType);
            callback.onCounterRead(counterByte);
        } catch (Exception e) {
            Log.e(TAG, "readRetryCounter: ", e);
        }
    }

    public void sign(Token.PinType pinType, String pin, byte[] data, SignCallback callback) {
        try {
            byte[] sign = token.sign(pinType, pin, data);
            callback.onSignResponse(sign);
        } catch (PinVerificationException e) {
            Log.e(TAG, "sign: ", e);
            callback.onSignError(null, e);
        } catch (Exception e) {
            Log.e(TAG, "sign: ", e);
            callback.onSignError(e, null);
        }
    }

    @Override
    public void onDestroy() {
        if (cardReader != null) {
            this.unregisterReceiver(cardReader.reciever);
            this.unregisterReceiver(cardReader.usbAttachReceiver);
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

    public void unblockAndChangePin(Token.PinType pinType, String puk, String newPin, ChangePinCallback callback) {
        try {
            boolean changed = token.unblockAndChangePin(pinType, puk.getBytes(), newPin.getBytes());
            if (changed) {
                callback.success();
            } else {
                callback.error(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "unblockAndChangePin: ", e);
            callback.error(e);
        }
    }

    public void unblockPin(Token.PinType pinType, String puk, UnblockPinCallback callback) {
        try {
            boolean unblocked = token.unblockPin(pinType, puk.getBytes());
            if (unblocked) {
                callback.success();
            } else {
                callback.error(new Exception("PIN change failed"));
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
            certCallback.onCertificateError(e);
        }
    }
}
