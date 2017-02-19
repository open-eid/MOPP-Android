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
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.DateUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.tokenlibrary.Token;
import timber.log.Timber;

public class PinChangeActivity extends AppCompatActivity {

    public static final String TAG = PinChangeActivity.class.getName();

    @BindView(R.id.radioButtons) RadioGroup radioPinGroup;
    @BindView(R.id.radioPIN) RadioButton radioPIN;
    @BindView(R.id.radioPUK) RadioButton radioPUK;
    @BindView(R.id.currentPinTitle) TextView currentPinTitle;
    @BindView(R.id.currentPinPuk) EditText currentPinPukView;
    @BindView(R.id.newPin) EditText newPinView;
    @BindView(R.id.newPinAgain) EditText newPinAgainView;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.pinInfoTitle) TextView pinInfoTitle;
    @BindView(R.id.pinInfo) TextView pinInfo;
    @BindView(R.id.newPinTitle) TextView newPinTitle;
    @BindView(R.id.newPinAgainTitle) TextView newPinAgainTitle;
    @BindView(R.id.changeButton) Button changeButton;

    private TokenService tokenService;
    private RetryCounterCallback pinBlockedCallback;
    private ChangePinCallback pinChangeCallback;

    private BroadcastReceiver cardPresentReceiver;
    private BroadcastReceiver cardAbsentReciever;

    private boolean serviceBound;
    boolean pinBlocked;
    boolean cardProvided;

    private Token.PinType pinType;
    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pinType = (Token.PinType) getIntent().getSerializableExtra(Constants.PIN_TYPE_KEY);
        setLayoutTitle();
        setContentView(R.layout.pin_change);
        ButterKnife.bind(this);
        notificationUtil = new NotificationUtil(this);
        radioPIN.setChecked(true);
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
    }

    @OnClick({R.id.radioPIN, R.id.radioPUK})
    void onChangePinMethodChange() {
        refreshLayout(radioPinGroup.getCheckedRadioButtonId());
    }

    @OnClick(R.id.changeButton)
    void onChangeButtonClicked() {
        final String currentPinPuk = currentPinPukView.getText().toString();
        final String newPin = newPinView.getText().toString();
        final String newPinAgain = newPinAgainView.getText().toString();

        PersonalFileCallback callback = new PersonalFileCallback() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                Date dateOfBirth;
                try {
                    dateOfBirth = DateUtils.DATE_FORMAT.parse(result.get(6));
                } catch (ParseException e) {
                    Timber.e(e, "Error parsing date of birth from personal file");
                    notificationUtil.showFailMessage("Couldn't read date of birth");
                    return;
                }

                if (!pinsValid(currentPinPuk, newPin, newPinAgain, dateOfBirth)) {
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

            @Override
            public void onPersonalFileError(String msg) {
                Timber.d("onPersonalFileError: %s", msg);
            }
        };
        tokenService.readPersonalFile(callback);
    }

    private boolean pinsValid(String currentPinPuk, String newPin, String newPinAgain, Date dateOfBirth) {
        switch (radioPinGroup.getCheckedRadioButtonId()) {
            case R.id.radioPIN:
                if (currentPinPuk.equals(newPin)) {
                    CharSequence pinsEqual = getText(R.string.pin_old_equals_new);
                    notificationUtil.showFailMessage(pinsEqual);
                    newPinView.setError(pinsEqual);
                    return false;
                }
        }
        switch (pinType) {
            case PIN1:
                return validatePin1(newPin, newPinAgain, dateOfBirth);
            case PIN2:
                return validatePin2(newPin, newPinAgain, dateOfBirth);
        }
        return true;
    }

    private boolean validatePin2(String newPin, String newPinAgain, Date dateOfBirth) {
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
        } else if (containsDateOfBirth(newPin, dateOfBirth)) {
            notificationUtil.showFailMessage(getText(R.string.pin_contains_date_of_birth));
            return false;
        }

        return true;
    }

    private boolean validatePin1(String newPin, String newPinAgain, Date dateOfBirth) {
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
        } else if (containsDateOfBirth(newPin, dateOfBirth)) {
            notificationUtil.showFailMessage(getText(R.string.pin_contains_date_of_birth));
            return false;
        }

        return true;
    }

    private boolean containsDateOfBirth(String newPin, Date dateOfBirth) {
        String mmdd = DateUtils.MMDD_FORMAT.format(dateOfBirth);
        String yyyy = DateUtils.YYYY_FORMAT.format(dateOfBirth);
        String ddmm = DateUtils.DDMM_FORMAT.format(dateOfBirth);
        String[] dates = {mmdd, ddmm, yyyy};
        for (String partOfBirth : Arrays.asList(dates)) {
            if (newPin.contains(partOfBirth)) {
                return true;
            }
        }
        return false;
    }

    private void readRetryCounter() {
        if (!pinBlocked && cardProvided) {
            tokenService.readRetryCounter(pinType, pinBlockedCallback);
        }
    }

    class ChangeCallback implements ChangePinCallback {

        @Override
        public void success() {
            switch (pinType) {
                case PIN1:
                    notificationUtil.showSuccessMessage(getText(R.string.pin1_change_success));
                case PIN2:
                    notificationUtil.showSuccessMessage(getText(R.string.pin2_change_success));
            }
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
