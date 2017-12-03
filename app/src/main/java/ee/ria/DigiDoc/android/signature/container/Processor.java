package ee.ria.DigiDoc.android.signature.container;

import android.os.SystemClock;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.IntentUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ChooseDocumentsAction,
                    Result.ChooseDocumentsResult> chooseDocuments =
            upstream -> upstream.flatMap(action ->
                    Observable.just(Result.ChooseDocumentsResult.create()));

    private final ObservableTransformer<Action.AddDocumentsAction,
                    Result.AddDocumentsResult> addDocuments =
            upstream -> upstream.flatMap(action -> createDocuments(action.fileStreams())
                    .toObservable()
                    .map(Result.AddDocumentsResult::success)
                    .onErrorReturn(Result.AddDocumentsResult::failure)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWith(Result.AddDocumentsResult.inProgress()));

    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.merge(
                shared.ofType(Action.ChooseDocumentsAction.class).compose(chooseDocuments),
                shared.ofType(Action.AddDocumentsAction.class).compose(addDocuments)));
    }

    /**
     * This should be in data source.
     */
    private Single<ImmutableList<Document>> createDocuments(
            ImmutableList<IntentUtils.FileStream> fileStreams) {
        return Single.fromCallable(() -> {
            Timber.e("START SAVE");
            SystemClock.sleep(3000);
            ImmutableList.Builder<Document> documentBuilder = new ImmutableList.Builder<>();
            for (IntentUtils.FileStream fileStream : fileStreams) {
                documentBuilder.add(Document.create(fileStream.displayName()));
            }
            Timber.e("END SAVE");
            return documentBuilder.build();
        });
    }
}
