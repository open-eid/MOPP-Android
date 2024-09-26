package ee.ria.DigiDoc.android.model.idcard;

import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.crypto.CryptoException;
import ee.ria.DigiDoc.crypto.DecryptToken;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.idcard.CertificateType;
import ee.ria.DigiDoc.idcard.CodeType;
import ee.ria.DigiDoc.idcard.CodeVerificationException;
import ee.ria.DigiDoc.idcard.PersonalData;
import ee.ria.DigiDoc.idcard.Token;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderManager;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderStatus;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okio.ByteString;

@Singleton
public final class IdCardService {

    private final SmartCardReaderManager smartCardReaderManager;
    private final FileSystem fileSystem;

    @Inject IdCardService(SmartCardReaderManager smartCardReaderManager, FileSystem fileSystem) {
        this.smartCardReaderManager  = smartCardReaderManager;
        this.fileSystem = fileSystem;
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
                                Token token =
                                        Token.create(smartCardReaderManager.connectedReader());
                                return IdCardDataResponse.success(data(token), token);
                            })
                            .subscribeOn(Schedulers.io())
                            .startWithItem(IdCardDataResponse.cardDetected());
                })
                .onErrorReturn(IdCardDataResponse::failure)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<SignedContainer> sign(Context context, Token token, SignedContainer container,
                                        byte[] pin2, @Nullable RoleData roleData) {
        return Single
                .fromCallable(() -> {
                    IdCardData data = data(token);
                    return container.sign(data.signCertificate().data(),
                            signData -> ByteString.of(token.calculateSignature(pin2,
                                    signData.toByteArray(),
                                    data.signCertificate().ellipticCurve())), roleData);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<IdCardData> editPin(Token token, CodeType pinType, String currentPin,
                                      String newPin) {
        return Single
                .fromCallable(() -> {
                    token.changeCode(pinType, currentPin.getBytes(StandardCharsets.UTF_8), newPin.getBytes(StandardCharsets.US_ASCII));
                    return data(token);
                });
    }

    public Single<IdCardData> unblockPin(Token token, CodeType pinType, String puk, String newPin) {
        return Single
                .fromCallable(() -> {
                    token.unblockAndChangeCode(puk.getBytes(StandardCharsets.UTF_8), pinType, newPin.getBytes(StandardCharsets.US_ASCII));
                    return data(token);
                });
    }

    public Single<ImmutableList<File>> decrypt(Token token, File containerFile, String pin1) {
        return Single.fromCallable(() ->
                CryptoContainer.open(containerFile)
                        .decrypt(new IdCardToken(token), data(token).authCertificate(), pin1,
                                fileSystem.getContainerDataFilesDir(containerFile))
                        .dataFiles());
    }

    public static IdCardData data(Token token) throws Exception {
        PersonalData personalData = token.personalData();
        ByteString authenticationCertificateData = ByteString
                .of(token.certificate(CertificateType.AUTHENTICATION));
        ByteString signingCertificateData = ByteString
                .of(token.certificate(CertificateType.SIGNING));
        int pin1RetryCounter = token.codeRetryCounter(CodeType.PIN1);
        int pin2RetryCounter = token.codeRetryCounter(CodeType.PIN2);
        int pukRetryCounter = token.codeRetryCounter(CodeType.PUK);

        Certificate authCertificate = Certificate.create(authenticationCertificateData);
        Certificate signCertificate = Certificate.create(signingCertificateData);

        return IdCardData.create(authCertificate.type(), personalData, authCertificate,
                signCertificate, pin1RetryCounter, pin2RetryCounter, pukRetryCounter);
    }

    static final class IdCardToken implements DecryptToken {

        private final Token token;

        IdCardToken(Token token) {
            this.token = token;
        }

        @Override
        public byte[] decrypt(byte[] pin1, byte[] data, boolean ecc) throws CryptoException {
            try {
                return token.decrypt(pin1, data, ecc);
            } catch (CodeVerificationException e) {
                throw new Pin1InvalidException();
            } catch (SmartCardReaderException e) {
                throw new CryptoException("Decryption failed", e);
            }
        }
    }
}
