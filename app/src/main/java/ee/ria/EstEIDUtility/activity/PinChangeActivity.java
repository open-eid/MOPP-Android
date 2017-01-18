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

package ee.ria.EstEIDUtility.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.tokenlibrary.Token;

public class PinChangeActivity extends AppCompatActivity {

    private RadioGroup radioPinGroup;
    private RadioButton radioPIN;
    private RadioButton radioPUK;
    private TextView currentPinTitle;
    private EditText currentPinPukView;
    private EditText newPinView;
    private EditText newPinAgainView;
    private Button changeButton;

    private TokenService tokenService;
    private boolean serviceBound;

    private TextView title;
    private TextView pinInfoTitle;
    private TextView pinInfo;
    private TextView newPinTitle;
    private TextView newPinAgainTitle;

    private RetryCounterCallback pinBlockedCallback;
    private ChangePinCallback pinChangeCallback;

    private Token.PinType pinType;

    private BroadcastReceiver cardPresentReceiver;
    private BroadcastReceiver cardAbsentReciever;

    boolean pinBlocked;
    boolean cardProvided;

    private NotificationUtil notificationUtil;

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pinType = (Token.PinType) getIntent().getSerializableExtra(Constants.PIN_TYPE_KEY);
        setLayoutTitle();
        setContentView(R.layout.pin_change);

        radioPinGroup = (RadioGroup) findViewById(R.id.radioButtons);
        radioPIN = (RadioButton) findViewById(R.id.radioPIN);
        radioPUK = (RadioButton) findViewById(R.id.radioPUK);
        currentPinTitle = (TextView) findViewById(R.id.currentPinTitle);

        currentPinPukView = (EditText) findViewById(R.id.currentPinPuk);
        newPinView = (EditText) findViewById(R.id.newPin);
        newPinAgainView = (EditText) findViewById(R.id.newPinAgain);

        notificationUtil = new NotificationUtil(this);

        title = (TextView) findViewById(R.id.title);
        pinInfoTitle = (TextView) findViewById(R.id.pinInfoTitle);
        pinInfo = (TextView) findViewById(R.id.pinInfo);
        newPinTitle = (TextView) findViewById(R.id.newPinTitle);
        newPinAgainTitle = (TextView) findViewById(R.id.newPinAgainTitle);

        radioPIN.setChecked(true);

        changeButton = (Button) findViewById(R.id.changeButton);
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
        pinBlockedCallback = new PinBlockedCallback();
        pinChangeCallback = new ChangeCallback();

        Intent intent = new Intent(this, TokenService.class);
        bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);

        cardPresentReceiver = new CardPresentReciever();
        cardAbsentReciever = new CardAbsentReciever();

        switch (pinType) {
            case PIN2:
                title.setText(getText(R.string.pin2_changing));
                radioPIN.setText(getText(R.string.using_pin2_option));
                currentPinTitle.setText(getText(R.string.valid_pin_2));
                currentPinPukView.setHint(getText(R.string.valid_pin_2_hint));
                pinInfoTitle.setText(getText(R.string.pin2_info_title));
                pinInfo.setText(getText(R.string.pin2_info));
                newPinTitle.setText(getText(R.string.new_pin2_title));
                newPinView.setHint(getText(R.string.new_pin2_hint));
                newPinAgainTitle.setText(getText(R.string.new_pin2_again_title));
                newPinAgainView.setHint(getText(R.string.new_pin2_again_hint));
                break;
        }

        radioPinGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refreshLayout(radioPinGroup.getCheckedRadioButtonId());
            }
        });

        changeButton.setOnClickListener(new ChangeButtonClickListener());
    }

    class ChangeButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String currentPinPuk = currentPinPukView.getText().toString();
            String newPin = newPinView.getText().toString();
            String newPinAgain = newPinAgainView.getText().toString();

            if (!pinsValid(newPin, newPinAgain)) {
                return;
            }

            switch (radioPinGroup.getCheckedRadioButtonId()) {
                case R.id.radioPIN:
                    tokenService.changePin(pinType, currentPinPuk, newPin, pinChangeCallback);
                    break;
                case R.id.radioPUK:
                    tokenService.unblockAndChangePin(pinType, currentPinPuk, newPin, pinChangeCallback);
                    break;
            }
        }

    }

    private boolean pinsValid(String newPin, String newPinAgain) {
        switch (pinType) {
            case PIN1:
                return validatePin1(newPin, newPinAgain);
            case PIN2:
                return validatePin2(newPin, newPinAgain);
        }
        return true;
    }

    private boolean validatePin2(String newPin, String newPinAgain) {
        if (newPin.length() < 5) {
            String pinTooShortMessage = String.format(getString(R.string.new_pin2_length_short), newPin.length());
            newPinView.setError(pinTooShortMessage);
            notificationUtil.showFailMessage(pinTooShortMessage);
            return false;
        }
        if (!newPin.equals(newPinAgain)) {
            String pinNoMatchMessage = getString(R.string.pin2_no_match);
            newPinAgainView.setError(pinNoMatchMessage);
            notificationUtil.showFailMessage(pinNoMatchMessage);
            return false;
        } else if ("00000".equals(newPin) || "12345".equals(newPin)) {
            String pinTooEasyMessage = getString(R.string.pin2_too_easy);
            newPinView.setError(pinTooEasyMessage);
            notificationUtil.showFailMessage(pinTooEasyMessage);
            return false;
        }
        return true;
    }

    private boolean validatePin1(String newPin, String newPinAgain) {
        if (newPin.length() < 4) {
            String pinTooShortMessage = String.format(getString(R.string.new_pin1_length_short), newPin.length());
            newPinView.setError(pinTooShortMessage);
            notificationUtil.showFailMessage(pinTooShortMessage);
            return false;
        }
        if (!newPin.equals(newPinAgain)) {
            String pinNoMatchMessage = getString(R.string.pin1_no_match);
            newPinAgainView.setError(pinNoMatchMessage);
            notificationUtil.showFailMessage(pinNoMatchMessage);
            return false;
        } else if ("0000".equals(newPin) || "1234".equals(newPin)) {
            String pinTooEasyMessage = getString(R.string.pin1_too_easy);
            newPinView.setError(pinTooEasyMessage);
            notificationUtil.showFailMessage(pinTooEasyMessage);
            return false;
        }
        return true;
    }

    private void readRetryCounter() {
        if (!pinBlocked && cardProvided) {
            tokenService.readRetryCounter(pinType, pinBlockedCallback);
        }
    }

    class ChangeCallback implements ChangePinCallback {

        @Override
        public void success() {
            notificationUtil.showSuccessMessage(getText(R.string.pin1_change_success));
            pinBlocked = false;
            clearTexts();
            radioPIN.setEnabled(true);
            radioPIN.setChecked(true);
            refreshLayout(R.id.radioPIN);
        }

        @Override
        public void error(Exception e) {
            clearTexts();
            notificationUtil.showFailMessage(createPinChangeFailedMessage());
            readRetryCounter();
        }

    }

    private void clearTexts() {
        currentPinPukView.setText("");
        newPinView.setText("");
        newPinAgainView.setText("");
    }

    private void refreshLayout(int checkedRadioButtonId) {
        switch (checkedRadioButtonId) {
            case R.id.radioPIN:
                pinRelatedLayoutChanges();
                break;
            case R.id.radioPUK:
                currentPinTitle.setText(getText(R.string.valid_puk));
                currentPinPukView.setHint(getText(R.string.valid_puk_hint));
                break;
        }
    }

    private void pinRelatedLayoutChanges() {
        switch (pinType) {
            case PIN1:
                currentPinTitle.setText(getText(R.string.valid_pin_1));
                currentPinPukView.setHint(getText(R.string.valid_pin_1_hint));
                break;
            case PIN2:
                currentPinTitle.setText(getText(R.string.valid_pin_2));
                currentPinPukView.setHint(getText(R.string.valid_pin_2_hint));
                break;
        }
    }

    private CharSequence pinBlockedMessage() {
        switch (pinType) {
            case PIN1:
                return getText(R.string.pin1_blocked);
            case PIN2:
                return getText(R.string.pin2_blocked);
        }
        return null;
    }

    private String createPinChangeFailedMessage() {
        switch (pinType) {
            case PIN1:
                return getString(R.string.pin1_change_failed);
            case PIN2:
                return getString(R.string.pin2_change_failed);
        }
        return null;
    }

    class PinBlockedCallback implements RetryCounterCallback {

        @Override
        public void onCounterRead(byte counterByte) {
            if (counterByte > 0) {
                radioPIN.setEnabled(true);
                radioPUK.setChecked(false);
                radioPIN.setChecked(true);
                refreshLayout(R.id.radioPIN);
            } else {
                pinBlocked = true;
                notificationUtil.showFailMessage(pinBlockedMessage());
                radioPIN.setChecked(false);
                radioPUK.setChecked(true);
                radioPIN.setEnabled(false);
                refreshLayout(R.id.radioPUK);
            }
        }

    }

    private void setLayoutTitle() {
        switch (pinType) {
            case PIN1:
                setTitle(R.string.change_pin);
                break;
            case PIN2:
                setTitle(R.string.change_pin2);
                break;
        }
    }

    class CardPresentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            cardProvided = true;

            notificationUtil.clearMessages();
            radioPIN.setEnabled(true);
            radioPUK.setEnabled(true);
            changeButton.setEnabled(true);

            readRetryCounter();
        }

    }

    class CardAbsentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            cardProvided = false;
            notificationUtil.showFailMessage(getText(R.string.card_not_provided));
            radioPIN.setEnabled(false);
            radioPUK.setEnabled(false);
            changeButton.setEnabled(false);
            pinBlocked = false;
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
