package ee.ria.DigiDoc.android.model.idcard;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.scardcomlibrary.CardReader;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.TokenFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Singleton
public final class IdCardService {

    private final Observable<TokenResponse> tokenObservable;

    @Inject IdCardService(Application application) {
        tokenObservable = Observable
                .create(new TokeOnSubscribe(application))
                .publish()
                .refCount();
    }

    public final Observable<IdCardDataResponse> data() {
        return tokenObservable
                .flatMap(tokenResponse -> {
                    Token token = tokenResponse.token();
                    if (token != null) {
                        return Observable
                                .fromCallable(() ->
                                        IdCardDataResponse.data(IdCardData
                                                .create(token.readPersonalFile())))
                                .startWith(IdCardDataResponse.cardDetected())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    } else {
                        return Observable.just(IdCardDataResponse.readerDetected());
                    }
                });
    }

    static final class TokeOnSubscribe implements ObservableOnSubscribe<TokenResponse> {

        private final Application application;

        private CardReader cardReader;
        private Token token;

        TokeOnSubscribe(Application application) {
            this.application = application;
        }

        @Override
        public void subscribe(ObservableEmitter<TokenResponse> emitter) throws Exception {
            cardReader = CardReader.getInstance(application, CardReader.Provider.ACS);

            BroadcastReceiver tokenAvailableReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    token = TokenFactory.getTokenImpl(cardReader);
                    if (token != null) {
                        emitter.onNext(TokenResponse.create(cardReader, token));
                    }
                }
            };
            BroadcastReceiver cardAbsentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    emitter.onNext(TokenResponse.create(cardReader, token = null));
                }
            };

            application.registerReceiver(tokenAvailableReceiver,
                    new IntentFilter(ACS.TOKEN_AVAILABLE_INTENT));
            application.registerReceiver(cardAbsentReceiver,
                    new IntentFilter(ACS.CARD_ABSENT_INTENT));

            emitter.setCancellable(() -> {
                if (cardReader.receiver != null) {
                    application.unregisterReceiver(cardReader.receiver);
                }
                if (cardReader.usbAttachReceiver != null) {
                    application.unregisterReceiver(cardReader.usbAttachReceiver);
                }
                if (cardReader.usbDetachReceiver != null) {
                    application.unregisterReceiver(cardReader.usbDetachReceiver);
                }
                application.unregisterReceiver(tokenAvailableReceiver);
                application.unregisterReceiver(cardAbsentReceiver);
                cardReader = null;
                token = null;
            });
        }
    }

    @AutoValue
    static abstract class TokenResponse {

        abstract CardReader cardReader();

        @Nullable abstract Token token();

        static TokenResponse create(CardReader cardReader, @Nullable Token token) {
            return new AutoValue_IdCardService_TokenResponse(cardReader, token);
        }
    }
}
