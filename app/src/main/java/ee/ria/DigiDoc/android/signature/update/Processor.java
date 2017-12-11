package ee.ria.DigiDoc.android.signature.update;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadContainerAction,
                                        Result.LoadContainerResult> loadContainer;

    private final ObservableTransformer<Action.AddDocumentsAction,
                                        Result.AddDocumentsResult> addDocuments;

    private final ObservableTransformer<Action.OpenDocumentAction,
                                        Result.OpenDocumentResult> openDocument;

    private final ObservableTransformer<Action.DocumentsSelectionAction,
                                        Result.DocumentsSelectionResult> documentsSelection;

    private final ObservableTransformer<Action.RemoveDocumentsAction,
                                        Result.RemoveDocumentsResult> removeDocuments;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource) {
        loadContainer = upstream -> upstream.flatMap(action ->
                signatureContainerDataSource.get(action.containerFile())
                        .toObservable()
                        .map(Result.LoadContainerResult::success)
                        .onErrorReturn(AutoValue_Result_LoadContainerResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.LoadContainerResult.progress()));

        addDocuments = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.AddDocumentsResult.clear());
            } else if (action.fileStreams() == null) {
                return Observable.just(Result.AddDocumentsResult.picking());
            } else {
                return signatureContainerDataSource
                        .addDocuments(action.containerFile(), action.fileStreams())
                        .andThen(signatureContainerDataSource.get(action.containerFile()))
                        .toObservable()
                        .map(Result.AddDocumentsResult::success)
                        .onErrorReturn(Result.AddDocumentsResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.AddDocumentsResult.adding());
            }
        });

        openDocument = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.OpenDocumentResult.clear());
            } else {
                return signatureContainerDataSource
                        .getDocumentFile(action.containerFile(), action.document())
                        .toObservable()
                        .map(Result.OpenDocumentResult::success)
                        .onErrorReturn(Result.OpenDocumentResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.OpenDocumentResult.opening());
            }
        });

        documentsSelection = upstream -> upstream.map(action ->
                Result.DocumentsSelectionResult.create(action.documents()));

        removeDocuments = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.RemoveDocumentsResult.clear());
            } else {
                return signatureContainerDataSource
                        .removeDocuments(action.containerFile(), action.documents())
                        .andThen(signatureContainerDataSource.get(action.containerFile()))
                        .toObservable()
                        .map(Result.RemoveDocumentsResult::success)
                        .onErrorReturn(Result.RemoveDocumentsResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.RemoveDocumentsResult.progress());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.LoadContainerAction.class).compose(loadContainer),
                shared.ofType(Action.AddDocumentsAction.class).compose(addDocuments),
                shared.ofType(Action.OpenDocumentAction.class).compose(openDocument),
                shared.ofType(Action.DocumentsSelectionAction.class).compose(documentsSelection),
                shared.ofType(Action.RemoveDocumentsAction.class).compose(removeDocuments)));
    }
}
