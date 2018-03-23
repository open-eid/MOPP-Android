package ee.ria.DigiDoc.android.signature.update;

import android.app.Application;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
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

    Observable<? extends SignatureAddResponse> create(File containerFile,
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
        }
        throw new IllegalArgumentException("Unknown request " + request);
    }
}
