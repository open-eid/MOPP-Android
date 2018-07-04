package ee.ria.DigiDoc.android.model.idcard;

import org.openeid.cdoc4j.CDOCDecrypter;
import org.openeid.cdoc4j.DataFile;
import org.openeid.cdoc4j.ECRecipient;
import org.openeid.cdoc4j.RSARecipient;
import org.openeid.cdoc4j.exception.DecryptionException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.EIDType;
import ee.ria.DigiDoc.android.model.CertificateData;
import ee.ria.mopplib.data.SignedContainer;
import ee.ria.scardcomlibrary.SmartCardReaderManager;
import ee.ria.scardcomlibrary.SmartCardReaderStatus;
import ee.ria.tokenlibrary.PersonalData;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.TokenFactory;
import ee.ria.tokenlibrary.exception.PinVerificationException;
import ee.ria.tokenlibrary.util.Util;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okio.ByteString;
import timber.log.Timber;

import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;

@Singleton
public final class IdCardService {

    private final SmartCardReaderManager smartCardReaderManager;

    @Inject IdCardService(SmartCardReaderManager smartCardReaderManager) {
        this.smartCardReaderManager  = smartCardReaderManager;
    }

    public final Observable<IdCardDataResponse> data() {
        return smartCardReaderManager.status()
                .filter(duplicates())
                .switchMap(status -> {
                    if (status.equals(SmartCardReaderStatus.IDLE)) {
                        return Observable.just(IdCardDataResponse.initial());
                    } else if (status.equals(SmartCardReaderStatus.READER_DETECTED)) {
                        return Observable.just(IdCardDataResponse.readerDetected());
                    }
                    return Observable
                            .fromCallable(() -> {
                                Token token = TokenFactory
                                        .getTokenImpl(smartCardReaderManager.connectedReader());
                                if (token == null) {
                                    throw new IllegalStateException("Token is null");
                                }
                                return IdCardDataResponse.success(data(token), token);
                            })
                            .subscribeOn(Schedulers.io())
                            .startWith(IdCardDataResponse.cardDetected());
                })
                .onErrorReturn(IdCardDataResponse::failure)
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

    public Single<IdCardData> decrypt(Token token, File containerFile, String pin1) {
        return Single.fromCallable(() -> {
            List<DataFile> dataFiles = new CDOCDecrypter()
                    .withToken(new PKCS11Token(token, pin1))
                    .decrypt(new FileInputStream(containerFile));
            Timber.e("DATA FILES: %s", dataFiles);
            return data(token);
        });
    }

    public static IdCardData data(Token token) throws Exception {
        PersonalData personalData = token.personalData();
        ByteString authCertificateData = ByteString.of(token.readCert(Token.CertType.CertAuth));
        ByteString signCertificateData = ByteString.of(token.readCert(Token.CertType.CertSign));
        byte pin1RetryCounter = token.readRetryCounter(Token.PinType.PIN1);
        byte pin2RetryCounter = token.readRetryCounter(Token.PinType.PIN2);
        byte pukRetryCounter = token.readRetryCounter(Token.PinType.PUK);

        CertificateData authCertificate = CertificateData
                .create(pin1RetryCounter, authCertificateData);
        CertificateData signCertificate = CertificateData
                .create(pin2RetryCounter, signCertificateData);

        return IdCardData.create(EIDType.parseOrganization(authCertificate.organization()),
                personalData, authCertificate, signCertificate, pukRetryCounter);
    }

    static final class PKCS11Token implements org.openeid.cdoc4j.token.Token {

        private final Token token;
        private final String pin1;
        private final IdCardData data;

        PKCS11Token(Token token, String pin1) throws Exception {
            this.token = token;
            this.pin1 = pin1;
            data = data(token);
        }

        @Override
        public Certificate getCertificate() {
            try {
                return CertificateFactory.getInstance("X.509").generateCertificate(
                        new ByteArrayInputStream(data.authCertificate().data().toByteArray()));
            } catch (CertificateException e) {
                Timber.e(e);
                return null;
            }
        }

        @Override
        public byte[] decrypt(RSARecipient recipient) throws DecryptionException {
            try {
                return token.decrypt(pin1.getBytes(),
                        Util.concat(new byte[]{0x00}, recipient.getEncryptedKey()));
            } catch (PinVerificationException e) {
                throw new PinVerificationError(e, idCardData());
            } catch (Exception e) {
                Timber.e(e);
                throw new DecryptionException("Decrypt RSA recipient", e);
            }
        }

        @Override
        public byte[] decrypt(ECRecipient recipient) throws DecryptionException {
            return new byte[0];
        }

        private IdCardData idCardData() {
            try {
                return data(token);
            } catch (Exception e) {
                return data;
            }
        }
    }

    public static final class PinVerificationError extends DecryptionException {

        public final IdCardData idCardData;

        PinVerificationError(Exception cause, IdCardData idCardData) {
            super(cause.getMessage(), cause);
            this.idCardData = idCardData;
        }
    }
}
