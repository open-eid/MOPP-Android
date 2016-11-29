package ee.ria.EstEIDUtility.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import ee.ria.token.tokenservice.TokenService;

public class TokenServiceConnection implements ServiceConnection {

    private Context context;
    private ServiceCreatedCallback callback;

    public TokenServiceConnection(Context context, ServiceCreatedCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void connectService() {
        Intent intent = new Intent(context, TokenService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
        TokenService tokenService = binder.getService();
        if (tokenService != null) {
            callback.created(tokenService);
        } else {
            callback.failed();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        callback.disconnected();
    }

}
