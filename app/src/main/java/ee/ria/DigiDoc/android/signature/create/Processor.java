package ee.ria.DigiDoc.android.signature.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ChooseFilesAction,
                                        Result.ChooseFilesResult> chooseFiles;

    private final ObservableTransformer<Action.CreateContainerAction,
                                        Result.CreateContainerResult> createContainer;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource) {
        chooseFiles = upstream -> upstream.flatMap(action ->
                Observable.just(Result.ChooseFilesResult.create()));

        createContainer = upstream -> upstream.flatMap(action ->
                signatureContainerDataSource
                        .addContainer(action.fileStreams(), false).toObservable()
                        .map(Result.CreateContainerResult::success)
                        .onErrorReturn(Result.CreateContainerResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.CreateContainerResult.inProgress()));
    }

    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.merge(
                shared.ofType(Action.ChooseFilesAction.class).compose(chooseFiles),
                shared.ofType(Action.CreateContainerAction.class).compose(createContainer)));
    }
}
