/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.token.tokenservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import ee.ria.scardcomlibrary.CardReader;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.token.tokenservice.callback.UseCounterCallback;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.TokenFactory;
import ee.ria.tokenlibrary.exception.PinVerificationException;
import timber.log.Timber;

public class TokenService extends Service {

    private static final String TAG = TokenService.class.getName();

    private BroadcastReceiver tokenAvailableReceiver;

    private final IBinder mBinder = new LocalBinder();

    private Token token;
    private CardReader cardReader;

    public boolean isTokenAvailable() {
        return token != null;
    }

    public class LocalBinder extends Binder {
        public TokenService getService() {
            if (cardReader != null) {
                return TokenService.this;
            }
            return null;
        }
    }

    private class TokenAvailableReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            token = TokenFactory.getTokenImpl(cardReader);
            if (token != null) {
                Intent cardInsertedIntent = new Intent(ACS.CARD_PRESENT_INTENT);
                sendBroadcast(cardInsertedIntent);
            }
        }
    }

    @Override
    public void onCreate() {
        tokenAvailableReceiver = new TokenAvailableReceiver();
        registerReceiver(tokenAvailableReceiver, new IntentFilter(ACS.TOKEN_AVAILABLE_INTENT));
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        cardReader = CardReader.getInstance(this, CardReader.Provider.ACS);
        return mBinder;
    }

    public void readUseCounter(Token.CertType certType, UseCounterCallback callback) {
        if (token == null) {
            return;
        }
        try {
            int counterByte = token.readUseCounter(certType);
            callback.onCounterRead(counterByte);
        } catch (Exception e) {
            Timber.e(e, "Error reading use counter from card");
        }
    }

    public void readRetryCounter(Token.PinType pinType, RetryCounterCallback callback) {
        if (token == null) {
            return;
        }
        try {
            byte counterByte = token.readRetryCounter(pinType);
            callback.onCounterRead(counterByte);
        } catch (Exception e) {
            Timber.e(e, "Error reading retry counter from card");
        }
    }

    public void sign(Token.PinType pinType, String pin, byte[] data, SignCallback callback) {
        try {
            byte[] sign = token.sign(pinType, pin, data);
            callback.onSignResponse(sign);
        } catch (PinVerificationException e) {
            Timber.e(e, "Invalid PIN provided for signing");
            callback.onSignError(null, e);
        } catch (Exception e) {
            Timber.e(e, "Error occurred when trying to sign with %s", pinType.name());
            callback.onSignError(e, null);
        }
    }

    @Override
    public void onDestroy() {
        if (cardReader != null) {
            this.unregisterReceiver(cardReader.receiver);
            this.unregisterReceiver(cardReader.usbAttachReceiver);
            this.unregisterReceiver(cardReader.usbDetachReceiver);
        }
        unregisterReceiver(tokenAvailableReceiver);
        super.onDestroy();
    }

    public void readPersonalFile(PersonalFileCallback callback) {
        try {
            SparseArray<String> result = token.readPersonalFile();
            callback.onPersonalFileResponse(result);
        } catch (Exception e) {
            Timber.e(e, "Error reading personal file from card");
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
            Timber.e(e, "Error changing %s", pinType.name());
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
            Timber.e(e, "Error when trying to unblock and change %s", pinType.name());
            callback.error(e);
        }
    }

    public void readCert(Token.CertType type, CertCallback certCallback) {
        try {
            byte[] certBytes = token.readCert(type);
            certCallback.onCertificateResponse(certBytes);
        } catch (Exception e) {
            Timber.e(e, "Error reading certificate: %s from card", type.name());
            certCallback.onCertificateError(e);
        }
    }

}
