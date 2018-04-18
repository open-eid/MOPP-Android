package ee.ria.DigiDoc.android.model.idcard;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.model.EIDType;
import ee.ria.mopplib.data.SignedContainer;
import ee.ria.scardcomlibrary.CardReader;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.TokenFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okio.ByteString;
import timber.log.Timber;

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
                .switchMap(tokenResponse -> {
                    Token token = tokenResponse.token();
                    if (token != null) {
                        return Observable
                                .fromCallable(() -> {
                                    // don't know why try-catch is necessary
                                    try {
                                        return IdCardDataResponse.success(data(token), token);
                                    } catch (Exception e) {
                                        return IdCardDataResponse.failure(e);
                                    }
                                })
                                .onErrorReturn(IdCardDataResponse::failure)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .startWith(IdCardDataResponse.cardDetected());
                    } else {
                        return Observable.just(IdCardDataResponse.readerDetected());
                    }
                });
    }

    public Single<SignedContainer> sign(Token token, SignedContainer container, String pin2) {
        return Single
                .fromCallable(() -> {
                    IdCardData data = data(token);
                    return container.sign(data.signCertificate().data(),
                            signData -> ByteString.of(token.sign(Token.PinType.PIN2, pin2,
                                    signData.toByteArray(),
                                    data.signCertificate().ellipticCurve())));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
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

    private static final DateTimeFormatter EXPIRY_DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM.yyyy")
            .toFormatter();

    /**
     * TODO Make this private when signing flow is moved to this system.
     */
    public static IdCardData data(Token token) throws Exception {
        SparseArray<String> personalFile = token.readPersonalFile();
        ByteString authCertificateData = ByteString.of(token.readCert(Token.CertType.CertAuth));
        ByteString signCertificateData = ByteString.of(token.readCert(Token.CertType.CertSign));
        byte pin1RetryCounter = token.readRetryCounter(Token.PinType.PIN1);
        byte pin2RetryCounter = token.readRetryCounter(Token.PinType.PIN2);
        byte pukRetryCounter = token.readRetryCounter(Token.PinType.PUK);

        String surname = personalFile.get(1).trim();
        String givenName1 = personalFile.get(2).trim();
        String givenName2 = personalFile.get(3).trim();
        String citizenship = personalFile.get(5).trim();
        String personalCode = personalFile.get(7).trim();
        String documentNumber = personalFile.get(8).trim();
        String expiryDateString = personalFile.get(9).trim();

        StringBuilder givenNames = new StringBuilder(givenName1);
        if (givenName2.length() > 0) {
            if (givenNames.length() > 0) {
                givenNames.append(" ");
            }
            givenNames.append(givenName2);
        }

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(expiryDateString, EXPIRY_DATE_FORMAT);
        } catch (Exception e) {
            expiryDate = null;
            Timber.e("Could not parse expiry date %s", expiryDateString);
        }

        CertificateData authCertificate = CertificateData
                .create(pin1RetryCounter, authCertificateData);
        CertificateData signCertificate = CertificateData
                .create(pin2RetryCounter, signCertificateData);

        String type = null;
        if (authCertificate.organization().startsWith("ESTEID")) {
            if (authCertificate.organization().contains("MOBIIL-ID")) {
                type = EIDType.MOBILE_ID;
            } else if (authCertificate.organization().contains("DIGI-ID")) {
                type = EIDType.DIGI_ID;
            } else {
                type = EIDType.ID_CARD;
            }
        }

        return IdCardData.create(type, givenNames.toString(), surname, personalCode, citizenship,
                authCertificate, signCertificate, pukRetryCounter, documentNumber, expiryDate);
    }
}
