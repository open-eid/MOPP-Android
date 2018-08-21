package ee.ria.DigiDoc.android.auth;


import android.app.Application;
import android.content.ContentResolver;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.crypto.RecipientRepository;
import ee.ria.DigiDoc.idcard.Token;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Processor implements ObservableTransformer<Intent, Result> {

    private final ObservableTransformer<Intent.InitialIntent, Result.AuthInitResult> initial;
    private final ObservableTransformer<Intent.AuthIntent, Result.AuthResult> auth;

    @Inject
    Processor(Navigator navigator, RecipientRepository recipientRepository,
              ContentResolver contentResolver, FileSystem fileSystem,
              Application application, IdCardService idCardService) {

        initial = upstream -> upstream.switchMap(intent -> {
            if (intent.visible()) {
                return idCardService.data()
                        .map(Result.AuthInitResult::show)
                        .startWith(Result.AuthInitResult.show(IdCardDataResponse.initial()));
            } else {
                return Observable.just(Result.AuthInitResult.hide());
            }
        });


        auth = upstream -> upstream.switchMap(intent -> {
            AuthRequest request = intent.request();
            if (request != null) {
                Token token = request.token();
                 return idCardService.signForAutentication(token, request.hash(), request.pin1())
                         .flatMapObservable(signature ->   Observable
                                 .timer(3, TimeUnit.SECONDS)
                                 .map(ignored -> Result.AuthResult.success(signature))
                                 .startWith(Result.AuthResult.successMessage(signature)))
                                 .onErrorReturn(throwable -> {
                                     IdCardDataResponse idCardDataResponse = null;
                                     if (throwable instanceof Pin1InvalidException) {
                                         try {
                                             idCardDataResponse = IdCardDataResponse
                                                     .success(IdCardService.data(token), token);
                                         } catch (Exception ignored) {}

                                     }
                                     return Result.AuthResult.failure(throwable, idCardDataResponse);
                                 })
                                 .subscribeOn(Schedulers.io())
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .startWith(Result.AuthResult.activity());

            } else {
                return Observable.just(Result.AuthResult.clear(), Result.AuthResult.idle());
            }
        });
    }
    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.AuthIntent.class).compose(auth)));
    }
}
