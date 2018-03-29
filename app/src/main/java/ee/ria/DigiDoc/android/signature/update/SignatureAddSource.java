package ee.ria.DigiDoc.android.signature.update;

import android.app.Application;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardRequest;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardResponse;
import ee.ria.DigiDoc.android.signature.update.idcard.TokenServiceObservable;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdOnSubscribe;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdRequest;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class SignatureAddSource {

    private final Application application;
    private final SignatureContainerDataSource signatureContainerDataSource;
    private final SettingsDataStore settingsDataStore;

    @Inject SignatureAddSource(Application application,
                               SignatureContainerDataSource signatureContainerDataSource,
                               SettingsDataStore settingsDataStore) {
        this.application = application;
        this.signatureContainerDataSource = signatureContainerDataSource;
        this.settingsDataStore = settingsDataStore;
    }

    Observable<Result.SignatureAddResult> show(int method) {
        if (method == R.id.signatureUpdateSignatureAddMethodMobileId) {
            return Observable.just(Result.SignatureAddResult.show(method));
        } else if (method == R.id.signatureUpdateSignatureAddMethodIdCard) {
            return TokenServiceObservable.connect(application)
                    .switchMap(connectData -> {
                        if (connectData.cardPresent()) {
                            return TokenServiceObservable
                                    .read(connectData.tokenService())
                                    .toObservable()
                                    .map(data ->
                                            Result.SignatureAddResult.method(method,
                                                    IdCardResponse.data(data)))
                                    .startWith(Result.SignatureAddResult
                                            .method(method, IdCardResponse.reader()));
                        } else {
                            return Observable.just(Result.SignatureAddResult
                                    .method(method, IdCardResponse.reader()));
                        }
                    })
                    .startWith(Result.SignatureAddResult.method(method, IdCardResponse.initial()));
        } else {
            throw new IllegalArgumentException("Unknown method " + method);
        }
    }

    Observable<? extends SignatureAddResponse> sign(File containerFile,
                                                    SignatureAddRequest request) {
        if (request instanceof MobileIdRequest) {
            MobileIdRequest mobileIdRequest = (MobileIdRequest) request;
            if (mobileIdRequest.rememberMe()) {
                settingsDataStore.setPhoneNo(mobileIdRequest.phoneNo());
                settingsDataStore.setPersonalCode(mobileIdRequest.personalCode());
            }
            return Observable
                    .create(new MobileIdOnSubscribe(application, settingsDataStore, containerFile,
                            mobileIdRequest.personalCode(), mobileIdRequest.phoneNo()))
                    .switchMap(response -> {
                        String signature = response.signature();
                        if (signature != null) {
                            return signatureContainerDataSource
                                    .addSignature(containerFile, signature)
                                    .toObservable()
                                    .map(MobileIdResponse::success)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .startWith(response);
                        } else {
                            return Observable.just(response);
                        }
                    })
                    .startWith(MobileIdResponse
                            .status(GetMobileCreateSignatureStatusResponse.ProcessStatus.DEFAULT));
        } else if (request instanceof IdCardRequest) {
            IdCardRequest idCardRequest = (IdCardRequest) request;
            return TokenServiceObservable
                    .connect(application)
                    .switchMap(connectData -> {
                        if (connectData.cardPresent()) {
                            return signatureContainerDataSource
                                    .get(containerFile)
                                    .flatMap(container ->
                                            TokenServiceObservable
                                                    .sign(connectData.tokenService(), container,
                                                            settingsDataStore.getSignatureProfile(),
                                                            idCardRequest.pin2()))
                                    .map(IdCardResponse::success)
                                    .toObservable();
                        } else {
                            return Observable.empty();
                        }
                    });
        } else {
            throw new IllegalArgumentException("Unknown request " + request);
        }
    }
}
