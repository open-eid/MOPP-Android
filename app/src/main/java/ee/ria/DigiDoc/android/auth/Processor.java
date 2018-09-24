package ee.ria.DigiDoc.android.auth;


import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.auth.AuthService;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.idcard.Token;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Processor implements ObservableTransformer<Intent, Result> {

    private final ObservableTransformer<Intent.InitialIntent, Result.AuthInitResult> initial;
    private final ObservableTransformer<Intent.AuthenticationIntent, Result.AuthenticationResult> authInitial;
    private final ObservableTransformer<Intent.AuthActionIntent, Result.AuthActionResult> auth;

    @Inject
    Processor(AuthService authService, IdCardService idCardService) {

        initial = upstream -> upstream.switchMap(intent -> {
            if (intent.visible()) {
                return idCardService.data()
                        .map(Result.AuthInitResult::show)
                        .startWith(Result.AuthInitResult.show(IdCardDataResponse.initial()));
            } else {
                return Observable.just(Result.AuthInitResult.hide());
            }
        });

        authInitial = upstream -> upstream.switchMap(intent -> {
            if (intent.visible()) {
                return idCardService.data()
                        .map(Result.AuthenticationResult::show)
                        .startWith(Result.AuthenticationResult.show(IdCardDataResponse.initial()));
            } else {
                return Observable.just(Result.AuthenticationResult.hide());
            }
        });

        auth = upstream -> upstream.switchMap(intent -> {
            AuthRequest request = intent.request();
            if (request != null) {
                Token token = request.token();
                return idCardService.signForAutentication(token, request.hash(), request.pin1())
                        .flatMapObservable(signature -> Observable
                                .timer(3, TimeUnit.SECONDS)
                                .map(ignored -> {
                                    authService.sendAuthResponse(signature, request.sessionId(), request.certificate().data().base64(), request.hash(), request.keystore(), request.properties());
                                    return Result.AuthActionResult.success(signature);
                                })
                                .startWith(Result.AuthActionResult.successMessage(signature)))
                        .onErrorReturn(throwable -> {
                            IdCardDataResponse idCardDataResponse = null;
                            if (throwable instanceof Pin1InvalidException) {
                                try {
                                    idCardDataResponse = IdCardDataResponse
                                            .success(IdCardService.data(token), token);
                                } catch (Exception ignored) {

                                }

                            }
                            return Result.AuthActionResult.failure(throwable, idCardDataResponse);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.AuthActionResult.activity());
            } else {
                return Observable.just(Result.AuthActionResult.clear(), Result.AuthActionResult.idle());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.AuthenticationIntent.class).compose(authInitial),
                shared.ofType(Intent.AuthActionIntent.class).compose(auth)));
    }
}
