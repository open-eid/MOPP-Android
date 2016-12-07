package ee.ria.EstEIDUtility.activity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.UnblockPinCallback;
import ee.ria.token.tokenservice.token.Token;

public class PinUtilitiesActivity extends AppCompatActivity {

    public enum FragmentToLaunch {PIN1, PIN2}

    public static final String PIN_FRAGMENT_TO_LAUNCH = "ee.ria.EstEIDUtility.activity.PIN_Fragment_To_Launch";

    private static final String TAG = "PinUtilitiesActivity";

    private TextView content;
    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;
    private boolean serviceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_utilities);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View changePin1 = findViewById(R.id.changePin1);
        changePin1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPinChangeActivity(FragmentToLaunch.PIN1);
            }
        });

        View changePin2 = findViewById(R.id.changePin2);
        changePin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPinChangeActivity(FragmentToLaunch.PIN2);
            }
        });
    }

    private void launchPinChangeActivity(FragmentToLaunch fragmentToLaunch) {
        Intent intent = new Intent(this, PinChangeActivity.class);
        intent.putExtra(PinUtilitiesActivity.PIN_FRAGMENT_TO_LAUNCH, fragmentToLaunch);
        startActivity(intent);
    }
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ServiceCreatedCallback callback = new TokenServiceCreatedCallback();
        tokenServiceConnection = new TokenServiceConnection(this, callback);
        tokenServiceConnection.connectService();
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
            serviceBound = true;
        }

        @Override
        public void failed() {
            Log.e(TAG, "failed: ", null);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            serviceBound = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }


    public void readPIN1RetryCounter(View view) {
        try {
            RetryCounterCallback callback = new RetryCounterTaskCallback();
            tokenService.readRetryCounter(Token.PinType.PIN1, callback);
        } catch (Exception e) {
            Log.e(TAG, "changePin: ", e);
            content.setText(e.getMessage());
        }
    }

    public void readPIN2RetryCounter(View view) {
        try {
            RetryCounterCallback callback = new RetryCounterTaskCallback();
            tokenService.readRetryCounter(Token.PinType.PIN2, callback);
        } catch (Exception e) {
            Log.e(TAG, "changePin: ", e);
            content.setText(e.getMessage());
        }
    }

    public void readPUKRetryCounter(View view) {
        try {
            RetryCounterCallback callback = new RetryCounterTaskCallback();
            tokenService.readRetryCounter(Token.PinType.PUK, callback);
        } catch (Exception e) {
            Log.e(TAG, "changePin: ", e);
            content.setText(e.getMessage());
        }
    }

    public void changePin(View view) {
        String currentPinPuk = null;
        String newPinPuk = null;
        try {
            Token.PinType type = Token.PinType.PIN1;
            ChangePinCallback callback = new ChangePinTaskCallback(type);
            tokenService.changePin(type, currentPinPuk, newPinPuk, callback);
        } catch (Exception e) {
            Log.e(TAG, "changePin: ", e);
            content.setText(e.getMessage());
        }
    }

    public void unblockPin(View view) {
        //EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        String currentPin = null;
        try {
            //Token.PinType type = getPinType(view.getId());
            Token.PinType type = Token.PinType.PIN1;
            UnblockPinCallback callback = new UnblockPinTaskCallback(type);
            tokenService.unblockPin(type, currentPin, callback);
        } catch (Exception e) {
            Log.e(TAG, "unblockPin: ", e);
            content.setText(e.getMessage());
        }
    }

    private class RetryCounterTaskCallback implements RetryCounterCallback {
        @Override
        public void onCounterRead(byte counterByte) {
            Log.d(TAG, "onCounterRead: " + counterByte);
            content.setText("retries left: " + counterByte);
        }
    }

    //TODO: callback behaviour impl
    private class ChangePinTaskCallback implements ChangePinCallback {
        private Token.PinType pinType;

        ChangePinTaskCallback(Token.PinType pinType) {
            this.pinType = pinType;
        }

        @Override
        public void success() {
            Log.d(TAG, "success: ");
            String text = pinType.name() + " change success";
            content.setText(text);
        }

        @Override
        public void error(Exception e) {
            String text;
            if (e != null) {
                Log.e(TAG, "error: ", e);
                text = pinType.name() + " change failed. Reason: " + e.getMessage();
            } else {
                text = pinType.name() + " change failed for unknown reason";
            }
            content.setText(text);
        }
    }

    //TODO: callback behaviour impl
    private class UnblockPinTaskCallback implements UnblockPinCallback {
        private Token.PinType pinType;

        UnblockPinTaskCallback(Token.PinType pinType) {
            this.pinType = pinType;
        }

        @Override
        public void success() {
            String text = pinType.name() + " unblocked.";
            Log.d(TAG, "success: " + text);
            content.setText(text);
        }

        @Override
        public void error(Exception e) {
            String text;
            if (e != null) {
                Log.e(TAG, "error: ", e);
                text = pinType.name() + " unblocking failed. Reason: " + e.getMessage();
            } else {
                text = pinType.name() + " unblocking failed for unknown reason";
            }
            content.setText(text);
        }
    }

    /*public void enableButtons(boolean enable) {
        findViewById(R.id.pin_util_current).setEnabled(enable);
        findViewById(R.id.pin_util_new).setEnabled(enable);
        findViewById(R.id.pin_util_change1).setEnabled(enable);
        findViewById(R.id.pin_util_change2).setEnabled(enable);
        findViewById(R.id.pin_util_change_puk).setEnabled(enable);
        findViewById(R.id.pin_util_unblock1).setEnabled(enable);
        findViewById(R.id.pin_util_unblock2).setEnabled(enable);
    }

    private Token.PinType getPinType(int id) {
        switch (id) {
            case R.id.pin_util_change1:
                return Token.PinType.PIN1;
            case R.id.pin_util_change2:
                return Token.PinType.PIN2;
            case R.id.pin_util_unblock1:
                return Token.PinType.PIN1;
            case R.id.pin_util_unblock2:
                return Token.PinType.PIN2;
            case R.id.pin_util_change_puk:
                return Token.PinType.PUK;
        }
        return null;
    }*/

}
