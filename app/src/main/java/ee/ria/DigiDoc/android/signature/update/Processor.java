package ee.ria.DigiDoc.android.signature.update;

import com.google.common.collect.ImmutableList;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.navigation.Transaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.io.Files.getFileExtension;

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

    private final ObservableTransformer<Action.SignatureListVisibilityAction,
                                        Result.SignatureListVisibilityResult>
            signatureListVisibility;

    private final ObservableTransformer<Action.SignatureRemoveSelectionAction,
                                        Result.SignatureRemoveSelectionResult>
            signatureRemoveSelection;

    private final ObservableTransformer<Action.SignatureRemoveAction,
                                        Result.SignatureRemoveResult> signatureRemove;

    private final ObservableTransformer<Action.SignatureAddAction,
                                        Result.SignatureAddResult> signatureAdd;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource,
                      SettingsDataStore settingsDataStore) {
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

        signatureListVisibility = upstream -> upstream.map(action ->
                Result.SignatureListVisibilityResult.create(action.isVisible()));

        signatureRemoveSelection = upstream -> upstream.map(action ->
                Result.SignatureRemoveSelectionResult.create(action.signature()));

        signatureRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.SignatureRemoveResult.clear());
            } else {
                return signatureContainerDataSource
                        .removeSignature(action.containerFile(), action.signature())
                        .andThen(signatureContainerDataSource.get(action.containerFile()))
                        .toObservable()
                        .map(Result.SignatureRemoveResult::success)
                        .onErrorReturn(Result.SignatureRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.SignatureRemoveResult.progress());
            }
        });

        signatureAdd = upstream -> upstream.flatMap(action -> {
            File containerFile = action.containerFile();
            if (containerFile == null) {
                return Observable.just(Result.SignatureAddResult.clear());
            } else if (action.show()) {
                if (settingsDataStore.getFileTypes()
                        .contains(getFileExtension(containerFile.getName()))) {
                    return Observable.just(Result.SignatureAddResult.show());
                } else {
                    return signatureContainerDataSource
                            .addContainer(ImmutableList.of(FileStream.create(containerFile)), true)
                            .toObservable()
                            .map(newContainerFile -> Result.SignatureAddResult.transaction(
                                    Transaction.PushScreenTransaction.create(
                                            SignatureUpdateScreen.create(newContainerFile))))
                            .onErrorReturn(Result.SignatureAddResult::failure)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(Result.SignatureAddResult.creatingContainer());
                }
            } else {
                // TODO
                return Observable.just(Result.SignatureAddResult.clear());
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
                shared.ofType(Action.RemoveDocumentsAction.class).compose(removeDocuments),
                shared.ofType(Action.SignatureListVisibilityAction.class)
                        .compose(signatureListVisibility),
                shared.ofType(Action.SignatureRemoveSelectionAction.class)
                        .compose(signatureRemoveSelection),
                shared.ofType(Action.SignatureRemoveAction.class).compose(signatureRemove),
                shared.ofType(Action.SignatureAddAction.class).compose(signatureAdd)));
    }
}
