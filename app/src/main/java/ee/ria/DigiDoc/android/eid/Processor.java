package ee.ria.DigiDoc.android.eid;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadAction, Result.LoadResult> load;

    private final ObservableTransformer<Action.CertificatesTitleClickAction,
                                        Result.CertificatesTitleClickResult> certificatesTitleClick;

    private final ObservableTransformer<Intent.CodeUpdateIntent, Result.CodeUpdateResult>
            codeUpdate;

    @Inject Processor(IdCardService idCardService, Navigator navigator) {
        load = upstream -> upstream.switchMap(action -> {
            Observable<Result.LoadResult> resultObservable = idCardService.data()
                    .map(idCardDataResponse -> {
                        if (idCardDataResponse.error() != null) {
                            return Result.LoadResult.failure(idCardDataResponse.error());
                        } else {
                            return Result.LoadResult.success(idCardDataResponse);
                        }
                    })
                    .onErrorReturn(Result.LoadResult::failure);
            if (action.clear()) {
                return resultObservable
                        .startWith(Result.LoadResult.clear());
            }
            return resultObservable;
        });

        certificatesTitleClick = upstream -> upstream.map(action ->
                Result.CertificatesTitleClickResult.create(action.expand()));

        codeUpdate = upstream -> upstream
                .map(action -> Result.CodeUpdateResult.create(action.action()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.LoadAction.class).compose(load),
                shared.ofType(Action.CertificatesTitleClickAction.class)
                        .compose(certificatesTitleClick),
                shared.ofType(Intent.CodeUpdateIntent.class).compose(codeUpdate)));
    }
}
