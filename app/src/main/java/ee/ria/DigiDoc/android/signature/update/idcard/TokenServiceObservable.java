package ee.ria.DigiDoc.android.signature.update.idcard;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public final class TokenServiceObservable {

    public static Observable<TokenServiceConnectData> connect(Application application) {
        return Observable.create(new ConnectOnSubscribe(application));
    }

    public static Observable<IdCardData> read(TokenService tokenService) {
        return Observable
                .fromCallable(() -> {
                    SparseArray<String> data = tokenService.readPersonalFile();

                    String givenName1 = data.get(2).trim();
                    String givenName2 = data.get(3).trim();
                    String surname = data.get(1).trim();
                    String personalCode = data.get(7).trim();

                    StringBuilder givenNames = new StringBuilder(givenName1);
                    if (givenName2.length() > 0) {
                        if (givenNames.length() > 0) {
                            givenNames.append(" ");
                        }
                        givenNames.append(givenName2);
                    }

                    return IdCardData.create(givenNames.toString(), surname, personalCode);
                });
    }

    static final class ConnectOnSubscribe implements
            ObservableOnSubscribe<TokenServiceConnectData> {

        private final Application application;

        @Nullable private TokenService tokenService;

        ConnectOnSubscribe(Application application) {
            this.application = application;
        }

        @Override
        public void subscribe(ObservableEmitter<TokenServiceConnectData> emitter) throws Exception {
            ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    tokenService = ((TokenService.LocalBinder) service).getService();
                    emitter.onNext(TokenServiceConnectData.create(tokenService, false));
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    emitter.onComplete();
                    tokenService = null;
                }
            };
            BroadcastReceiver cardPresentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (tokenService != null) {
                        emitter.onNext(TokenServiceConnectData.create(tokenService, true));
                    }
                }
            };
            BroadcastReceiver cardAbsentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (tokenService != null) {
                        emitter.onNext(TokenServiceConnectData.create(tokenService, false));
                    }
                }
            };

            application.registerReceiver(cardPresentReceiver,
                    new IntentFilter(ACS.CARD_PRESENT_INTENT));
            application.registerReceiver(cardAbsentReceiver,
                    new IntentFilter(ACS.CARD_ABSENT_INTENT));
            application.bindService(new Intent(application, TokenService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);

            emitter.setCancellable(() -> {
                application.unbindService(serviceConnection);
                application.unregisterReceiver(cardAbsentReceiver);
                application.unregisterReceiver(cardPresentReceiver);
            });
        }
    }
}
