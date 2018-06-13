package ee.ria.DigiDoc.android.signature.list;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.cryptolib.CryptoContainer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ContainersLoadAction, Result.ContainersLoadResult>
            containersLoad;

    private final ObservableTransformer<Action.NavigateUpAction, Result.VoidResult> navigateUp;

    private final ObservableTransformer<Intent.ContainerOpenIntent, Result.VoidResult>
            containerOpen;

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

        containerOpen = upstream -> upstream.switchMap(action -> {
            File containerFile = action.containerFile();
            if (CryptoContainer.isContainerFileName(containerFile.getName())) {
                navigator.execute(Transaction.push(CryptoCreateScreen.open(containerFile)));
            } else {
                navigator.execute(Transaction.push(SignatureUpdateScreen
                        .create(true, false, containerFile, false, false)));
            }
            return Observable.empty();
        });

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
                shared.ofType(Intent.ContainerOpenIntent.class).compose(containerOpen),
                shared.ofType(Action.ContainerRemoveAction.class).compose(containerRemove)));
    }
}
