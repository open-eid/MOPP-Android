package ee.ria.DigiDoc.android.signature.update.idcard;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.SparseArray;

import javax.annotation.Nullable;

import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import timber.log.Timber;

public final class IdCardOnSubscribe implements ObservableOnSubscribe<IdCardResponse> {

    private final Application application;

    @Nullable TokenService tokenService;

    public IdCardOnSubscribe(Application application) {
        this.application = application;
    }

    @Override
    public void subscribe(ObservableEmitter<IdCardResponse> emitter) throws Exception {
        Timber.e("subscribe");

        BroadcastReceiver cardPresentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.e("CARD PRESENT YO");
                if (tokenService != null) {
                    tokenService.readPersonalFile(new PersonalFileCallback() {
                        @Override
                        public void onPersonalFileResponse(SparseArray<String> result) {
                            Timber.e("onPersonalFileResponse: %s", result);

                            String personalCode = result.get(7);
                            String surname = result.get(1).trim();
                            String givenName1 = result.get(2).trim();
                            String givenName2 = result.get(3).trim();
                            StringBuilder name = new StringBuilder();
                            if (givenName1.length() > 0) {
                                name.append(givenName1);
                            }
                            if (givenName2.length() > 0) {
                                if (name.length() > 0) {
                                    name.append(" ");
                                }
                                name.append(givenName2);
                            }
                            if (surname.length() > 0) {
                                if (name.length() > 0) {
                                    name.append(" ");
                                }
                                name.append(surname);
                            }

                            emitter.onNext(IdCardResponse.data(IdCardData.create(personalCode,
                                    name.toString())));
                        }
                        @Override
                        public void onPersonalFileError(String msg) {
                            Timber.e("onPersonalFileError: %s", msg);
                        }
                    });
                }
            }
        };
        BroadcastReceiver cardAbsentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.e("CARD ABSENT YO");
                emitter.onNext(IdCardResponse.reader());
            }
        };
        ServiceConnection tokenServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Timber.e("onServiceConnected");
                tokenService = ((TokenService.LocalBinder) service).getService();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Timber.e("onServiceDisconnected");
                tokenService = null;
            }
        };

        application.registerReceiver(cardPresentReceiver,
                new IntentFilter(ACS.CARD_PRESENT_INTENT));
        application.registerReceiver(cardAbsentReceiver, new IntentFilter(ACS.CARD_ABSENT_INTENT));
        application.bindService(new Intent(application, TokenService.class), tokenServiceConnection,
                Context.BIND_AUTO_CREATE);

        emitter.setCancellable(() -> {
            Timber.e("cancel");
            application.unbindService(tokenServiceConnection);
            application.unregisterReceiver(cardPresentReceiver);
            application.unregisterReceiver(cardAbsentReceiver);
        });
    }
}
