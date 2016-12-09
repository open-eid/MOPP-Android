package ee.ria.EstEIDUtility.activity;

import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.token.Token;

public class PinChangeActivity extends AppCompatActivity {

    private static final String TAG = "PinChangeActivity";

    private RadioGroup radioPinGroup;
    private RadioButton radioPIN;
    private RadioButton radioPUK;
    private TextView currentPinTitle;
    private EditText currentPinPukView;
    private EditText newPinView;
    private EditText newPinAgainView;
    private Button changeButton;

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;

    private LinearLayout success;
    private TextView successText;
    private LinearLayout fail;
    private TextView failText;

    private TextView title;
    private TextView pinInfoTitle;
    private TextView pinInfo;
    private TextView newPinTitle;
    private TextView newPinAgainTitle;

    private boolean tokenServiceBound;

    private RetryCounterCallback pinBlockedCallback;
    private ChangePinCallback pinChangeCallback;

    private Token.PinType pinType;

    boolean pinBlocked;
    //TODO: better refactor this and get callback when card connect
    boolean cardProvided;

    @Override
    public void onStop() {
        super.onStop();
        unBindTokenService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pinType = (Token.PinType) getIntent().getSerializableExtra(Constants.PIN_TYPE);
        setLayoutTitle();
        setContentView(R.layout.pin_change);

        radioPinGroup = (RadioGroup) findViewById(R.id.radioButtons);
        radioPIN = (RadioButton) findViewById(R.id.radioPIN);
        radioPUK = (RadioButton) findViewById(R.id.radioPUK);
        currentPinTitle = (TextView) findViewById(R.id.currentPinTitle);

        currentPinPukView = (EditText) findViewById(R.id.currentPinPuk);
        newPinView = (EditText) findViewById(R.id.newPin);
        newPinAgainView = (EditText) findViewById(R.id.newPinAgain);

        success = (LinearLayout) findViewById(R.id.success);
        fail = (LinearLayout) findViewById(R.id.fail);
        successText = (TextView) success.findViewById(R.id.text);
        failText = (TextView) fail.findViewById(R.id.text);

        title = (TextView) findViewById(R.id.title);
        pinInfoTitle = (TextView) findViewById(R.id.pinInfoTitle);
        pinInfo = (TextView) findViewById(R.id.pinInfo);
        newPinTitle = (TextView) findViewById(R.id.newPinTitle);
        newPinAgainTitle = (TextView) findViewById(R.id.newPinAgainTitle);

        radioPIN.setChecked(true);

        changeButton = (Button) findViewById(R.id.changeButton);
    }

    @Override
    public void onStart() {
        super.onStart();
        pinBlockedCallback = new PinBlockedCallback();
        pinChangeCallback = new ChangeCallback();
        connectTokenService();

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

            cardProvided = true;

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
            showFailMessage(pinTooShortMessage);
            return false;
        }
        if (!newPin.equals(newPinAgain)) {
            String pinNoMatchMessage = getString(R.string.pin2_no_match);
            newPinAgainView.setError(pinNoMatchMessage);
            showFailMessage(pinNoMatchMessage);
            return false;
        } else if ("00000".equals(newPin) || "12345".equals(newPin)) {
            String pinTooEasyMessage = getString(R.string.pin2_too_easy);
            newPinView.setError(pinTooEasyMessage);
            showFailMessage(pinTooEasyMessage);
            return false;
        }
        return true;
    }

    private boolean validatePin1(String newPin, String newPinAgain) {
        if (newPin.length() < 4) {
            String pinTooShortMessage = String.format(getString(R.string.new_pin1_length_short), newPin.length());
            newPinView.setError(pinTooShortMessage);
            showFailMessage(pinTooShortMessage);
            return false;
        }
        if (!newPin.equals(newPinAgain)) {
            String pinNoMatchMessage = getString(R.string.pin1_no_match);
            newPinAgainView.setError(pinNoMatchMessage);
            showFailMessage(pinNoMatchMessage);
            return false;
        } else if ("0000".equals(newPin) || "1234".equals(newPin)) {
            String pinTooEasyMessage = getString(R.string.pin1_too_easy);
            newPinView.setError(pinTooEasyMessage);
            showFailMessage(pinTooEasyMessage);
            return false;
        }
        return true;
    }

    private void showSuccessMessage() {
        fail.setVisibility(View.GONE);
        successText.setText(getText(R.string.pin1_change_success));
        success.setVisibility(View.VISIBLE);
        pinBlocked = false;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                success.setVisibility(View.GONE);
            }
        }, 5000);
    }

    private void showFailMessage(CharSequence message) {
        failText.setText(message);
        fail.setVisibility(View.VISIBLE);
        if (!pinBlocked && cardProvided) {
            tokenService.readRetryCounter(pinType, pinBlockedCallback);
        }
    }

    class ChangeCallback implements ChangePinCallback {

        @Override
        public void success() {
            showSuccessMessage();
            clearTexts();
            radioPIN.setEnabled(true);
            refreshLayout(R.id.radioPIN);
        }

        @Override
        public void error(Exception e) {
            clearTexts();
            //TODO: handle exceptions in TokenService to be able to differ what was exactly the reason and show different messages
            if (e != null) {
                showFailMessage(createPinChangeFailedMessage());
            } else {
                showFailMessage(createPinChangeFailedMessage());
            }
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
            cardProvided = true;
            if (counterByte > 0) {
                radioPIN.setEnabled(true);
                radioPUK.setChecked(false);
                radioPIN.setChecked(true);
                refreshLayout(R.id.radioPIN);
            } else {
                pinBlocked = true;
                showFailMessage(pinBlockedMessage());
                radioPIN.setChecked(false);
                radioPUK.setChecked(true);
                radioPIN.setEnabled(false);
                refreshLayout(R.id.radioPUK);
            }
        }

        @Override
        public void cardNotProvided() {
            cardProvided = false;
            showFailMessage(getText(R.string.card_not_provided));
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

    private void connectTokenService() {
        tokenServiceConnection = new TokenServiceConnection(this, new TokenServiceCreatedCallback());
        tokenServiceConnection.connectService();
        tokenServiceBound = true;
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
            tokenService.readRetryCounter(pinType, pinBlockedCallback);
        }

        @Override
        public void failed() {
            Log.d(TAG, "failed to bind token service");
        }

        @Override
        public void disconnected() {
            Log.d(TAG, "token service disconnected");
        }
    }

    private void unBindTokenService() {
        if (tokenServiceConnection != null && tokenServiceBound) {
            unbindService(tokenServiceConnection);
            tokenServiceBound = false;
        }
    }

}
