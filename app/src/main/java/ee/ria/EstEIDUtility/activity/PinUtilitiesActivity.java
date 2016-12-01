package ee.ria.EstEIDUtility.activity;

import android.app.Service;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.ChangePinCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.UnblockPinCallback;

public class PinUtilitiesActivity extends AppCompatActivity {

    private static final String TAG = "PinUtilitiesActivity";

    TextView content;
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
        content = (TextView) findViewById(R.id.pin_util_content);
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
            enableButtons(true);
            serviceBound = true;
            Toast.makeText(PinUtilitiesActivity.this, "Service connected", Toast.LENGTH_LONG).show();
        }

        @Override
        public void failed() {
            Log.e(TAG, "failed: ", null);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            serviceBound = false;
            enableButtons(false);
            Toast.makeText(PinUtilitiesActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
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
        EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        EditText newPinPuk = (EditText)findViewById(R.id.pin_util_new);
        try {
            Token.PinType type = getPinType(view.getId());
            ChangePinCallback callback = new ChangePinTaskCallback(type);
            tokenService.changePin(type, currentPinPuk.getText().toString(), newPinPuk.getText().toString(), callback);
        } catch (Exception e) {
            Log.e(TAG, "changePin: ", e);
            content.setText(e.getMessage());
        }
    }

    public void unblockPin(View view) {
        EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        try {
            Token.PinType type = getPinType(view.getId());
            UnblockPinCallback callback = new UnblockPinTaskCallback(type);
            tokenService.unblockPin(type, currentPinPuk.getText().toString(), callback);
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

    public void enableButtons(boolean enable) {
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
    }

}
