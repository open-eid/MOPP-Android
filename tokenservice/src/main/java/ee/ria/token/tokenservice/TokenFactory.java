package ee.ria.token.tokenservice;

import android.content.Context;
import android.util.Log;

import ee.ria.token.tokenservice.impl.EstEIDv3d4;

public class TokenFactory {

    private Context context;

    public TokenFactory(Context context) {
        this.context = context;
    }

    //TODO: get card version impl?
    public Token getTokenImpl() {
        SMInterface sminterface = SMInterface.getInstance(context, SMInterface.ACS);
        if (sminterface == null) {
            return null;
        }
        sminterface.connect(new SMInterface.Connected() {
            @Override
            public void connected() {
                Log.i("SIMINTERFACE_CONNECT", "connected!");
            }
        });
        return new EstEIDv3d4(sminterface);
    }
}
