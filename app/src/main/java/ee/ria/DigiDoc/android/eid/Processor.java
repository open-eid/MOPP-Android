package ee.ria.DigiDoc.android.eid;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadAction, Result.LoadResult> load;

    @Inject Processor(IdCardService idCardService) {
        load = upstream -> upstream.switchMap(action ->
                idCardService.data().map(Result.LoadResult::create));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.LoadAction.class).compose(load)));
    }
}
