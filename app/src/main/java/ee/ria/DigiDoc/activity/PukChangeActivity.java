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

package ee.ria.DigiDoc.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.util.DateUtils;
import ee.ria.DigiDoc.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.tokenlibrary.Token;
import timber.log.Timber;

public class PukChangeActivity extends AppCompatActivity {

    public static final String TAG = PukChangeActivity.class.getName();

    @BindView(R.id.currentPuk) EditText currentPukView;
    @BindView(R.id.newPuk) EditText newPukView;
    @BindView(R.id.newPukAgain) EditText newPukAgainView;
    @BindView(R.id.changePukButton) Button changePukButton;

    private TokenService tokenService;
    private RetryCounterCallback pukBlockedCallback;
    private ChangePinCallback pukChangeCallback;

    private BroadcastReceiver cardPresentReceiver;
    private BroadcastReceiver cardAbsentReciever;

    private boolean serviceBound;
    boolean cardProvided;

    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.puk_change);
        ButterKnife.bind(this);
        notificationUtil = new NotificationUtil(this);
        Timber.tag(TAG);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(cardPresentReceiver, new IntentFilter(ACS.CARD_PRESENT_INTENT));
        registerReceiver(cardAbsentReciever, new IntentFilter(ACS.CARD_ABSENT_INTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cardPresentReceiver != null) {
            unregisterReceiver(cardPresentReceiver);
        }
        if (cardAbsentReciever != null) {
            unregisterReceiver(cardAbsentReciever);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        pukBlockedCallback = new PukBlockedCallback();
        pukChangeCallback = new ChangeCallback();

        Intent intent = new Intent(this, TokenService.class);
        bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);

        cardPresentReceiver = new CardPresentReceiver();
        cardAbsentReciever = new CardAbsentReceiver();
    }

    @OnClick(R.id.changePukButton)
    void onChangeButtonClicked() {
        final String currentPuk = currentPukView.getText().toString();
        final String newPuk = newPukView.getText().toString();
        final String newPukAgain = newPukAgainView.getText().toString();
        tokenService.readPersonalFile(new PersonalFileCallback() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                try {
                    if (currentPuk.equals(newPuk)) {
                        CharSequence pinsEqual = getText(R.string.puk_old_equals_new);
                        newPukView.setError(pinsEqual);
                        notificationUtil.showFailMessage(pinsEqual);
                    } else if (newPuk.length() < 8) {
                        String pukTooShortMessage = String.format(getString(R.string.new_puk_length_short), newPuk.length());
                        newPukView.setError(pukTooShortMessage);
                        notificationUtil.showFailMessage(pukTooShortMessage);
                    } else if (!newPuk.equals(newPukAgain)) {
                        String pukNoMatchMessage = getString(R.string.puk_no_match);
                        newPukAgainView.setError(pukNoMatchMessage);
                        notificationUtil.showFailMessage(pukNoMatchMessage);
                    } else if ("00000000".equals(newPuk) || "12345678".equals(newPuk)) {
                        String pukTooEasyMessage = getString(R.string.puk_too_easy);
                        newPukView.setError(pukTooEasyMessage);
                        notificationUtil.showFailMessage(pukTooEasyMessage);
                    } else if (containsDateOfBirth(newPuk, DateUtils.DATE_FORMAT.parse(result.get(6)))) {
                        notificationUtil.showFailMessage(getText(R.string.puk_contains_date_of_birth));
                    } else {
                        tokenService.changePin(Token.PinType.PUK, currentPuk, newPuk, pukChangeCallback);
                    }
                } catch (ParseException e) {
                    Timber.e(e, "Error parsing date of birth from personal file");
                    notificationUtil.showFailMessage("Couldn't read date of birth");
                }
            }

            @Override
            public void onPersonalFileError(String msg) {
                Timber.d("onPersonalFileError: %s", msg);
            }
        });
    }

    private boolean containsDateOfBirth(String newPuk, Date dateOfBirth) {
        String mmdd = DateUtils.MMDD_FORMAT.format(dateOfBirth);
        String yyyy = DateUtils.YYYY_FORMAT.format(dateOfBirth);
        String ddmm = DateUtils.DDMM_FORMAT.format(dateOfBirth);
        for (String partOfBirth : Arrays.asList(new String[] {mmdd, ddmm, yyyy})) {
            if (newPuk.contains(partOfBirth)) {
                return true;
            }
        }
        return false;
    }

    private void readRetryCounter() {
        if (cardProvided) {
            tokenService.readRetryCounter(Token.PinType.PUK, pukBlockedCallback);
        }
    }

    private class ChangeCallback implements ChangePinCallback {

        @Override
        public void success() {
            notificationUtil.showSuccessMessage(getText(R.string.puk_change_success));
            clearTexts();

            currentPukView.setEnabled(false);
            newPukView.setEnabled(false);
            newPukAgainView.setEnabled(false);
            changePukButton.setEnabled(false);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    PukChangeActivity.this.finish();
                }
            }, 2000);
        }

        @Override
        public void error(Exception e) {
            clearTexts();
            notificationUtil.showFailMessage(getString(R.string.puk_change_failed));
            readRetryCounter();
        }

    }

    private void clearTexts() {
        currentPukView.setText("");
        newPukView.setText("");
        newPukAgainView.setText("");
    }

    private class PukBlockedCallback implements RetryCounterCallback {

        @Override
        public void onCounterRead(byte counterByte) {
            if (counterByte == 0) {
                notificationUtil.showFailMessage(getText(R.string.puk_blocked));
            }
        }

    }

    class CardPresentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            cardProvided = true;

            notificationUtil.clearMessages();
            changePukButton.setEnabled(true);

            readRetryCounter();
        }

    }

    class CardAbsentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            cardProvided = false;
            notificationUtil.showFailMessage(getText(R.string.card_not_provided));
            changePukButton.setEnabled(false);
        }
    }

    private ServiceConnection tokenServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
            tokenService = binder.getService();
            if (!tokenService.isTokenAvailable()) {
                notificationUtil.showWarningMessage(getText(R.string.insert_card_wait));
            }
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

}
