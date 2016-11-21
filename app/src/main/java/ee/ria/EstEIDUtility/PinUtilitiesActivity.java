package ee.ria.EstEIDUtility;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import ee.ria.aidl.token.tokenaidllibrary.PinActionsListener;
import ee.ria.aidl.token.tokenaidllibrary.TokenAidlInterface;

public class PinUtilitiesActivity extends AppCompatActivity {

    TextView content;
    private TokenAidlInterface service;
    private RemoteServiceConnection serviceConnection;
    private boolean serviceBound = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_utilities);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        content = (TextView) findViewById(R.id.pin_util_content);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    private void connectService() {
        serviceConnection = new RemoteServiceConnection();
        Intent i = new Intent("ee.ria.aidl.token.tokenaidlservice.TokenService");
        i.setPackage("ee.ria.aidl.token.tokenaidlservice");
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void changePin(View view) {
        EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        EditText newPinPuk = (EditText)findViewById(R.id.pin_util_new);
        try {
            Token.PinType type = getPinType(view.getId());
            switch (view.getId()) {
                case R.id.pin_util_change1: service.changePin1(new ServiceCallbackStub(type), currentPinPuk.getText().toString(), newPinPuk.getText().toString()); break;
                case R.id.pin_util_change2: service.changePin2(new ServiceCallbackStub(type), currentPinPuk.getText().toString(), newPinPuk.getText().toString()); break;
                case R.id.pin_util_change_puk: service.changePuk(new ServiceCallbackStub(type), currentPinPuk.getText().toString(), newPinPuk.getText().toString()); break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            content.setText(e.getMessage());
        }
    }

    public void unblockPin(View view) {
        EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        try {
            Token.PinType type = getPinType(view.getId());
            switch (view.getId()) {
                case R.id.pin_util_unblock1: service.unblockPin1(new ServiceCallbackStub(type), currentPinPuk.getText().toString()); break;
                case R.id.pin_util_unblock2: service.unblockPin2(new ServiceCallbackStub(type), currentPinPuk.getText().toString()); break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            content.setText(e.getMessage());
        }
    }

    private Token.PinType getPinType(int id) {
        switch (id) {
            case R.id.pin_util_change1: return Token.PinType.PIN1;
            case R.id.pin_util_change2: return Token.PinType.PIN2;
            case R.id.pin_util_unblock1: return Token.PinType.PIN1;
            case R.id.pin_util_unblock2: return Token.PinType.PIN2;
            case R.id.pin_util_change_puk: return Token.PinType.PUK;
        }
        return null;
    }

    public void enableButtons( boolean enable) {
        findViewById(R.id.pin_util_current).setEnabled(enable);
        findViewById(R.id.pin_util_new).setEnabled(enable);
        findViewById(R.id.pin_util_change1).setEnabled(enable);
        findViewById(R.id.pin_util_change2).setEnabled(enable);
        findViewById(R.id.pin_util_change_puk).setEnabled(enable);
        findViewById(R.id.pin_util_unblock1).setEnabled(enable);
        findViewById(R.id.pin_util_unblock2).setEnabled(enable);
    }

    class RemoteServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            service = TokenAidlInterface.Stub.asInterface((IBinder) boundService);
            serviceBound = true;
            enableButtons(true);
            Toast.makeText(PinUtilitiesActivity.this, "Service connected", Toast.LENGTH_LONG)
                    .show();
        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
            serviceBound = false;
            enableButtons(false);
            Toast.makeText(PinUtilitiesActivity.this, "Service disconnected", Toast.LENGTH_LONG)
                    .show();
        }

    }

    private class ServiceCallbackStub extends PinActionsListener.Stub {

        private Token.PinType pinType;

        public ServiceCallbackStub(Token.PinType pinType) {
            super();
            this.pinType = pinType;
        }

        @Override
        public void onPinActionSuccessful() throws RemoteException {
            showResultOnUiThread(pinType.name() + " change success");
        }

        @Override
        public void onPinActionFailed(final String reason) throws RemoteException {
            showResultOnUiThread(pinType.name() + " change failed" + "reason: " + reason);
        }

        private void showResultOnUiThread(final String result) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    content.setText(result);
                }
            });
        }
    }

}
