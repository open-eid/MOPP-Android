package ee.ria.DigiDoc.android.model.idcard;

import android.util.SparseArray;

import com.google.common.base.Optional;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.DigiDoc.android.model.EIDType;
import ee.ria.mopplib.data.SignedContainer;
import ee.ria.scardcomlibrary.SmartCardReader;
import ee.ria.scardcomlibrary.SmartCardReaderManager;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.TokenFactory;
import ee.ria.tokenlibrary.exception.PinVerificationException;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okio.ByteString;
import timber.log.Timber;

@Singleton
public final class IdCardService {

    private final Observable<Optional<SmartCardReader>> readerObservable;

    @Inject IdCardService(SmartCardReaderManager smartCardReaderManager) {
        readerObservable = smartCardReaderManager.reader()
                .publish()
                .refCount();
    }

    public final Observable<IdCardDataResponse> data() {
        return readerObservable
                .filter(new SmartCardReaderPredicate())
                .switchMap(readerOptional -> {
                    if (readerOptional.isPresent() && readerOptional.get().connected()) {
                        Token token = TokenFactory.getTokenImpl(readerOptional.get());
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
                                    .startWith(IdCardDataResponse.cardDetected());
                        }
                    } else if (readerOptional.isPresent()) {
                        return Observable.just(IdCardDataResponse.readerDetected());
                    }
                    return Observable.just(IdCardDataResponse.initial());
                })
                .observeOn(AndroidSchedulers.mainThread());
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

    public Single<IdCardData> editPin(Token token, Token.PinType pinType, String currentPin,
                                      String newPin) {
        return Single
                .fromCallable(() -> {
                    boolean result = token
                            .changePin(pinType, currentPin.getBytes(), newPin.getBytes());
                    if (!result) {
                        throw new PinVerificationException(pinType);
                    }
                    return data(token);
                });
    }

    public Single<IdCardData> unblockPin(Token token, Token.PinType pinType, String puk,
                                         String newPin) {
        return Single
                .fromCallable(() -> {
                    boolean result = token.unblockAndChangePin(pinType, puk.getBytes(),
                            newPin.getBytes());
                    if (!result) {
                        throw new PinVerificationException(pinType);
                    }
                    return data(token);
                });
    }

    private static final DateTimeFormatter CARD_DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM.yyyy")
            .toFormatter();

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
        String dateOfBirthString = personalFile.get(6).trim();
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

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(dateOfBirthString, CARD_DATE_FORMAT);
        } catch (Exception e) {
            dateOfBirth = null;
            Timber.e(e, "Could not parse date of birth %s", dateOfBirthString);
        }

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(expiryDateString, CARD_DATE_FORMAT);
        } catch (Exception e) {
            expiryDate = null;
            Timber.e(e, "Could not parse expiry date %s", expiryDateString);
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
                dateOfBirth, authCertificate, signCertificate, pukRetryCounter, documentNumber,
                expiryDate);
    }

    static final class SmartCardReaderPredicate implements Predicate<Optional<SmartCardReader>> {

        private boolean present = false;
        private boolean connected = false;

        @Override
        public boolean test(Optional<SmartCardReader> current) {
            boolean present = current.isPresent();
            boolean connected = current.isPresent() && current.get().connected();

            boolean result = this.present == present && this.connected == connected;

            this.present = present;
            this.connected = connected;

            return !result;
        }
    }
}
