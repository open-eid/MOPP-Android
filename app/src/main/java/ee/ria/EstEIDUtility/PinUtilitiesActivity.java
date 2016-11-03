package ee.ria.EstEIDUtility;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class PinUtilitiesActivity extends AppCompatActivity {

    TextView content;
    private SMInterface sminterface = null;
    private EstEIDToken eidToken;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_utilities);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        content = (TextView) findViewById(R.id.pin_util_content);
        enableButtons(false);
    }

    @Override
    protected void onResume () {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (nfcAdapter == null) {
            return;
        }
        sminterface = new SMInterface.NFC((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                enableButtons(sminterface != null);
            }
        });
        eidToken = new EstEIDToken(sminterface, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sminterface != null) {
            sminterface.close();
        }
    }

    public void connectReader(View view) {
        sminterface = SMInterface.getInstance(this, SMInterface.ACS);
        if (sminterface == null) {
            content.setText("No readers connected");
            return;
        }
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                enableButtons(sminterface != null);
            }
        });
        eidToken = new EstEIDToken(sminterface, this);
    }

    public void changePin(View view) {
        EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        EditText newPinPuk = (EditText)findViewById(R.id.pin_util_new);
        try {
            Token.PinType type = null;
            switch (view.getId()) {
                case R.id.pin_util_change1: type = Token.PinType.PIN1; break;
                case R.id.pin_util_change2: type = Token.PinType.PIN2; break;
                case R.id.pin_util_change_puk: type = Token.PinType.PUK; break;
            }
            boolean status = eidToken.changePin(currentPinPuk.getText().toString().getBytes(),
                    newPinPuk.getText().toString().getBytes(),
                    type);
            content.setText(status ? type.name() + " change success" : type.name() + " change failed");
        } catch (Exception e) {
            e.printStackTrace();
            content.setText(e.getMessage());
        }
    }

    public void unblockPin(View view) {
        EditText currentPinPuk = (EditText)findViewById(R.id.pin_util_current);
        try {
            Token.PinType type = null;
            switch (view.getId()) {
                case R.id.pin_util_unblock1: type = Token.PinType.PIN1; break;
                case R.id.pin_util_unblock2: type = Token.PinType.PIN2; break;
            }
            boolean status = eidToken.unblockPin(currentPinPuk.getText().toString().getBytes(), type);
            content.setText(status ? type.name() + " unblock success" : type.name() + " unblock failed");
        } catch (Exception e) {
            e.printStackTrace();
            content.setText(e.getMessage());
        }
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

}
