package ee.ria.DigiDoc.android.signature.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.CreateContainerAction,
                                        Result.CreateContainerResult> createContainer;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource) {
        createContainer = upstream -> upstream.flatMap(action ->
                signatureContainerDataSource
                        .addContainer(action.fileStreams(), false).toObservable()
                        .map(Result.CreateContainerResult::success)
                        .onErrorReturn(Result.CreateContainerResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.CreateContainerResult.inProgress()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.CreateContainerAction.class).compose(createContainer)));
    }
}
