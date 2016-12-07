package ee.ria.EstEIDUtility.fragment;

import android.app.Service;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.token.Token;

public class PIN1ChangeFragment extends Fragment {

    public static final String TAG = "PIN1_CHANGE_FRAGMENT";

    private RadioGroup radioPinGroup;
    private RadioButton radioPIN1;
    private RadioButton radioPUK;
    private TextView currentPinTitle;
    private EditText currentPinView;
    private EditText newPinView;
    private EditText newPinAgainView;
    private Button changeButton;

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;

    private boolean tokenServiceBound;

    public PIN1ChangeFragment() {}

    private void unBindTokenService() {
        if (tokenServiceConnection != null && tokenServiceBound) {
            getActivity().unbindService(tokenServiceConnection);
            tokenServiceBound = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        unBindTokenService();
    }

    @Override
    public void onStart() {
        super.onStart();
        connectTokenService();
    }

    private void connectTokenService() {
        tokenServiceConnection = new TokenServiceConnection(getActivity(), new TokenServiceCreatedCallback());
        tokenServiceConnection.connectService();
        tokenServiceBound = true;
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.pin1_change, container, false);

        radioPinGroup = (RadioGroup) fragLayout.findViewById(R.id.radioButtons);
        radioPIN1 = (RadioButton) fragLayout.findViewById(R.id.radioPIN1);
        radioPUK = (RadioButton) fragLayout.findViewById(R.id.radioPUK);
        currentPinTitle = (TextView) fragLayout.findViewById(R.id.currentPinTitle);

        currentPinView = (EditText) fragLayout.findViewById(R.id.currentPin);
        newPinView = (EditText) fragLayout.findViewById(R.id.newPin);
        newPinAgainView = (EditText) fragLayout.findViewById(R.id.newPinAgain);

        changeButton = (Button) fragLayout.findViewById(R.id.changeButton);
        return fragLayout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        radioPinGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                refreshLayout(radioPinGroup.getCheckedRadioButtonId());
            }
        });

        changeButton.setOnClickListener(new ChangeButtonClickListener());


        super.onActivityCreated(savedInstanceState);
    }

    class ChangeButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            //TODO: get all pins and puks
            String currentPin = currentPinView.getText().toString();
            String newPin = newPinView.getText().toString();
            String newPinAgain = newPinAgainView.getText().toString();

            if (!pinsValid(newPin, newPinAgain)) {
                return;
            }

            Log.d(TAG, "Current PIN: " + currentPin + " New PIN: " + newPin);
            tokenService.changePin(Token.PinType.PIN1, currentPin, newPin, new ChangePin1Callback());

            Log.d(TAG, "onClick: " + currentPin + " " + newPin + " " + newPinAgain);
        }
    }

    private boolean pinsValid(String newPin, String newPinAgain) {
        boolean result = true;
        if (newPin.length() < 4) {
            newPinView.setError("PIN length must be at least 4 characters");
            result = false;
        }
        if (!newPin.equals(newPinAgain)) {
            newPinAgainView.setError("PINs don't match");
            result = false;
        }
        return result;
    }

    class ChangePin1Callback implements ChangePinCallback {

        @Override
        public void success() {
            NotificationUtil.showNotification(getActivity(), "PIN 1 changed succesfully", NotificationUtil.NotificationType.SUCCESS);
        }

        @Override
        public void error(Exception e) {
            if (e != null) {
                NotificationUtil.showNotification(getActivity(), "PIN 1 change failed " + e.getMessage(), NotificationUtil.NotificationType.ERROR);
            } else {
                //TODO: retries left message
                NotificationUtil.showNotification(getActivity(), "PIN 1 change failed ", NotificationUtil.NotificationType.ERROR);
            }

        }
    }
    private void refreshLayout(int checkedRadioButtonId) {
        Log.d(TAG, "refreshLayout: ");
        switch (checkedRadioButtonId) {
            case R.id.radioPIN1:
                //TODO: check if PIN1 is blocked
                boolean blocked = false;
                if (blocked) {
                    radioPIN1.setChecked(false);
                    radioPUK.setChecked(true);
                    radioPIN1.setEnabled(false);
                }
                currentPinTitle.setText(getResources().getString(R.string.valid_pin_1));
                currentPinView.setHint(getResources().getString(R.string.valid_pin_1_hint));

                InputFilter[] inputFilters = {new InputFilter.LengthFilter(12)};

                currentPinView.setFilters(inputFilters);
                newPinView.setFilters(inputFilters);
                newPinAgainView.setFilters(inputFilters);

                break;
            case R.id.radioPUK:
                currentPinTitle.setText(getResources().getString(R.string.valid_puk));
                currentPinView.setHint(getResources().getString(R.string.valid_puk_hint));
                break;
        }
    }

}