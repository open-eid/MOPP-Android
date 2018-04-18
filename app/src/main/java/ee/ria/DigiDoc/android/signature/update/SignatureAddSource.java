package ee.ria.DigiDoc.android.signature.update;

import android.app.Application;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.model.idcard.IdCardSignResponse;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardRequest;
import ee.ria.DigiDoc.android.signature.update.idcard.IdCardResponse;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdOnSubscribe;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdRequest;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.tokenlibrary.exception.PinVerificationException;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class SignatureAddSource {

    private final Application application;
    private final SignatureContainerDataSource signatureContainerDataSource;
    private final SettingsDataStore settingsDataStore;
    private final IdCardService idCardService;

    @Inject SignatureAddSource(Application application,
                               SignatureContainerDataSource signatureContainerDataSource,
                               SettingsDataStore settingsDataStore, IdCardService idCardService) {
        this.application = application;
        this.signatureContainerDataSource = signatureContainerDataSource;
        this.settingsDataStore = settingsDataStore;
        this.idCardService = idCardService;
    }

    Observable<Result.SignatureAddResult> show(int method) {
        if (method == R.id.signatureUpdateSignatureAddMethodMobileId) {
            return Observable.just(Result.SignatureAddResult.show(method));
        } else if (method == R.id.signatureUpdateSignatureAddMethodIdCard) {
            return idCardService.data()
                    .map(dataResponse -> {
                        if (dataResponse.error() == null) {
                            return Result.SignatureAddResult
                                    .method(method, IdCardResponse.data(dataResponse));
                        } else {
                            return Result.SignatureAddResult.failure(dataResponse.error());
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
            return signatureContainerDataSource
                    .get(containerFile)
                    .flatMapObservable(container ->
                            Observable.create(new MobileIdOnSubscribe(application, container,
                                    mobileIdRequest.personalCode(), mobileIdRequest.phoneNo())))
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
            return signatureContainerDataSource
                    .get(containerFile)
                    .flatMap(container ->
                            idCardService
                                    .sign(idCardRequest.token(), container, idCardRequest.pin2()))
                    .toObservable()
                    .map(IdCardResponse::success)
                    .onErrorResumeNext(error -> {
                        if (error instanceof PinVerificationException) {
                            IdCardData data = IdCardService.data(idCardRequest.token());
                            if (data.signCertificate().pinRetryCount() > 0) {
                                return Observable.just(IdCardResponse.sign(IdCardSignResponse
                                        .failure(error, data, idCardRequest.token())));
                            }
                        }
                        return Observable.error(error);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWith(IdCardResponse.sign(IdCardSignResponse.activity()));
        } else {
            throw new IllegalArgumentException("Unknown request " + request);
        }
    }
}
