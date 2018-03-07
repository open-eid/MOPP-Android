package ee.ria.DigiDoc.android.signature.list;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ContainersLoadAction, Result.ContainersLoadResult>
            containersLoad;

    private final ObservableTransformer<Action.NavigateUpAction, Result.VoidResult> navigateUp;

    private final ObservableTransformer<Action.NavigateToContainerUpdateAction, Result.VoidResult>
            navigateToContainerUpdate;

    private final ObservableTransformer<Action.ContainerRemoveAction, Result.ContainerRemoveResult>
            containerRemove;

    @Inject Processor(Navigator navigator,
                      SignatureContainerDataSource signatureContainerDataSource) {
        containersLoad = upstream -> upstream.switchMap(action ->
                signatureContainerDataSource.find()
                        .toObservable()
                        .map(Result.ContainersLoadResult::success)
                        .onErrorReturn(Result.ContainersLoadResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.ContainersLoadResult
                                .progress(action.indicateActivity())));

        navigateUp = upstream -> upstream
                .doOnNext(action -> navigator.execute(action.transaction()))
                .map(action -> Result.VoidResult.create());

        navigateToContainerUpdate = upstream -> upstream
                .doOnNext(action -> navigator.execute(action.transaction()))
                .map(action -> Result.VoidResult.create());

        containerRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.ContainerRemoveResult.cancel());
            } else if (action.confirmation()) {
                return Observable
                        .just(Result.ContainerRemoveResult.confirmation(action.containerFile()));
            } else {
                return signatureContainerDataSource.remove(action.containerFile())
                        .andThen(signatureContainerDataSource.find())
                        .toObservable()
                        .map(Result.ContainerRemoveResult::success)
                        .onErrorReturn(Result.ContainerRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.ContainerRemoveResult.progress());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ContainersLoadAction.class).compose(containersLoad),
                shared.ofType(Action.NavigateUpAction.class).compose(navigateUp),
                shared.ofType(Action.NavigateToContainerUpdateAction.class)
                        .compose(navigateToContainerUpdate),
                shared.ofType(Action.ContainerRemoveAction.class).compose(containerRemove)));
    }
}
