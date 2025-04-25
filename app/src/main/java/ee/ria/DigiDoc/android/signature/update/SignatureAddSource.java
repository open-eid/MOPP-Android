package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

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
import ee.ria.DigiDoc.android.signature.update.nfc.NFCOnSubscribe;
import ee.ria.DigiDoc.android.signature.update.nfc.NFCRequest;
import ee.ria.DigiDoc.android.signature.update.nfc.NFCResponse;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdOnSubscribe;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdRequest;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdResponse;
import ee.ria.DigiDoc.android.utils.LocaleService;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.idcard.CodeVerificationException;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okio.ByteString;
import timber.log.Timber;

final class SignatureAddSource {

    private final SignatureContainerDataSource signatureContainerDataSource;
    private final SettingsDataStore settingsDataStore;
    private final IdCardService idCardService;
    private final LocaleService localeService;

    private static final String EMPTY_VALUE = "";

    @Inject SignatureAddSource(SignatureContainerDataSource signatureContainerDataSource,
                               SettingsDataStore settingsDataStore, IdCardService idCardService,
                               LocaleService localeService) {
        this.signatureContainerDataSource = signatureContainerDataSource;
        this.settingsDataStore = settingsDataStore;
        this.idCardService = idCardService;
        this.localeService = localeService;
    }

    Observable<Result.SignatureAddResult> show(int method) {
        if (method == R.id.signatureUpdateSignatureAddMethodMobileId) {
            return Observable.just(Result.SignatureAddResult.show(method));
        } else if (method == R.id.signatureUpdateSignatureAddMethodSmartId) {
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
                    .startWithItem(Result.SignatureAddResult.method(method, IdCardResponse.initial()));
        } else if (method == R.id.signatureUpdateSignatureAddMethodNFC) {
            Timber.log(Log.DEBUG, "SignatureAddSource.java:83 :show");
            return Observable.just(Result.SignatureAddResult.show(method));
        } else {
            throw new IllegalArgumentException("Unknown method " + method);
        }
    }

    Observable<Result.SignatureAddResult> showRoleView(int method) {
        return Observable.just(Result.SignatureAddResult.showRoleView(method));
    }

    Observable<? extends SignatureAddResponse> sign(File containerFile,
                                                    SignatureAddRequest request,
                                                    Navigator navigator,
                                                    @Nullable RoleData roleData,
                                                    boolean isSivaConfirmed) {
        if (request instanceof MobileIdRequest mobileIdRequest) {
            if (mobileIdRequest.rememberMe()) {
                settingsDataStore.setPhoneNo(mobileIdRequest.phoneNo());
                settingsDataStore.setPersonalCode(mobileIdRequest.personalCode());
            } else {
                settingsDataStore.setPhoneNo(EMPTY_VALUE);
                settingsDataStore.setPersonalCode(EMPTY_VALUE);
            }
            return signatureContainerDataSource
                    .get(containerFile, isSivaConfirmed)
                    .flatMapObservable(container ->
                            Observable.create(new MobileIdOnSubscribe(navigator, container,
                                            localeService.applicationLocale(),
                                    settingsDataStore.getUuid(), mobileIdRequest.personalCode(),
                                    mobileIdRequest.phoneNo(), settingsDataStore.getProxySetting(),
                                    settingsDataStore.getManualProxySettings(navigator.activity()), roleData)))
                    .switchMap(response -> {
                        String signature = response.signature();
                        Button mobileIdCancelButton = navigator.activity().findViewById(R.id.signatureUpdateMobileIdCancelButton);
                        if (mobileIdCancelButton != null && signature != null) {
                            mobileIdCancelButton.setVisibility(View.GONE);
                            return signatureContainerDataSource
                                    .addSignature(containerFile, signature)
                                    .toObservable()
                                    .map(MobileIdResponse::success)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .startWithItem(response);
                        } else {
                            return Observable.just(response);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(MobileIdResponse
                            .status(MobileCreateSignatureSessionStatusResponse.ProcessStatus.OK))
                    .onErrorResumeNext(Observable::error);
        } else if (request instanceof SmartIdRequest smartIdRequest) {
            if (smartIdRequest.rememberMe()) {
                settingsDataStore.setCountry(smartIdRequest.country());
                settingsDataStore.setSidPersonalCode(smartIdRequest.personalCode());
            } else {
                settingsDataStore.setCountry(EMPTY_VALUE);
                settingsDataStore.setSidPersonalCode(EMPTY_VALUE);
            }
            return signatureContainerDataSource
                    .get(containerFile, isSivaConfirmed)
                    .flatMapObservable(container ->
                            Observable.create(new SmartIdOnSubscribe(navigator, container,
                                    localeService.applicationLocale(), settingsDataStore.getUuid(),
                                    smartIdRequest.personalCode(), smartIdRequest.country(),
                                    settingsDataStore.getProxySetting(),
                                    settingsDataStore.getManualProxySettings(navigator.activity()), roleData)))
                    .switchMap(response -> {
                        SessionStatusResponse.ProcessStatus processStatus = response.status();
                        Button smartIdCancelButton = navigator.activity().findViewById(R.id.signatureUpdateSmartIdCancelButton);
                        if (smartIdCancelButton != null && SessionStatusResponse.ProcessStatus.OK.equals(processStatus)) {
                            smartIdCancelButton.setVisibility(View.GONE);
                        }
                        return Observable.just(response);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(SmartIdResponse
                            .status(SessionStatusResponse.ProcessStatus.OK))
                    .onErrorResumeNext(Observable::error);
        } else if (request instanceof NFCRequest nfcRequest) {
            settingsDataStore.setCan(nfcRequest.can());
            Single<SignedContainer> s = signatureContainerDataSource.get(containerFile, isSivaConfirmed);
            Observable<NFCResponse> obs = s.flatMapObservable(container -> {
                NFCOnSubscribe nfcsub = new NFCOnSubscribe(navigator, container, nfcRequest.can(), nfcRequest.pin2(), roleData);
                return Observable.create(nfcsub);
                });
            return obs.switchMap(Observable::just)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(NFCResponse.createWithStatus(SessionStatusResponse.ProcessStatus.OK, null))
                    .onErrorResumeNext(Observable::error);
        } else if (request instanceof IdCardRequest idCardRequest) {
            return signatureContainerDataSource
                    .get(containerFile, isSivaConfirmed)
                    .flatMap(container ->
                            idCardService.data()
                                    .filter(dataResponse -> dataResponse.token() != null)
                                    .switchMapSingle(dataResponse ->
                                            idCardService.sign(navigator.activity(), dataResponse.token(), container,
                                                    idCardRequest.pin2(), roleData)).firstOrError())
                    .map(IdCardResponse::success)
                    .toObservable()
                    .onErrorResumeNext(error -> {
                        if (error instanceof CodeVerificationException) {
                            return idCardService.data()
                                    .filter(dataResponse -> dataResponse.data() != null)
                                    .switchMap(dataResponse -> {
                                        IdCardData data = dataResponse.data();
                                        if (data != null && data.pin2RetryCount() > 0) {
                                            return Observable.just(
                                                    IdCardResponse.sign(IdCardSignResponse.clear(
                                                            error, data, idCardRequest.token())),
                                                    IdCardResponse.sign(IdCardSignResponse.failure(
                                                            error, data, idCardRequest.token())));
                                        }
                                        return Observable.error(error);
                                    });
                        }

                        return Observable.error(error);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(IdCardResponse.sign(IdCardSignResponse.activity()));
        } else {
            throw new IllegalArgumentException("Unknown request " + request);
        }
    }

    public Single<SignedContainer> sign(Context context, String signatureValue, byte[] dataToSign,
                                        SignedContainer container,
                                        @Nullable RoleData roleData) {
        return Single
                .fromCallable(() -> container.sign(context, ByteString.of(dataToSign),
                        signData -> ByteString.encodeUtf8(signatureValue), roleData, false, false))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
