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

import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.mopplib.data.SignedContainer;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.tokenlibrary.Token;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import okio.ByteString;

public final class TokenServiceObservable {

    public static Observable<TokenServiceConnectData> connect(Application application) {
        return Observable.create(new ConnectOnSubscribe(application));
    }

    public static Single<IdCardData> read(TokenService tokenService) {
        return Single.fromCallable(() -> IdCardData.create(tokenService.readPersonalFile()));
    }

    public static Single<IdCardCertData> cert(TokenService tokenService, Token.CertType type) {
        return Single.fromCallable(() ->
                IdCardCertData.create(type, ByteString.of(tokenService.readCert(type))));
    }

    public static Single<SignedContainer> sign(TokenService tokenService, SignedContainer container,
                                               String profile, String pin2) {
        return cert(tokenService, Token.CertType.CertSign)
                .map(certData ->
                        container.sign(certData.data(), profile,
                                input -> ByteString.of(tokenService.sign(pin2, input.toByteArray(),
                                        certData.ellipticCurve()))));
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
