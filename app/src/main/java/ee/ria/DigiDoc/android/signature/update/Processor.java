package ee.ria.DigiDoc.android.signature.update;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.LoadContainerAction,
                                        Result.LoadContainerResult> loadContainer =
            upstream -> upstream.flatMap(action -> loadContainer(action.containerFile())
                    .toObservable()
                    .map(Result.LoadContainerResult::success)
                    .onErrorReturn(AutoValue_Result_LoadContainerResult::failure)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWith(Result.LoadContainerResult.progress()));

    private final ObservableTransformer<Action.AddDocumentsAction,
                                        Result.AddDocumentsResult> addDocuments =
            upstream -> upstream.flatMap(action -> {
                if (action.containerFile() == null) {
                    return Observable.just(Result.AddDocumentsResult.clear());
                } else if (action.fileStreams() == null) {
                    return Observable.just(Result.AddDocumentsResult.picking());
                } else {
                    return addDocuments(action.containerFile(), action.fileStreams())
                            .toObservable()
                            .map(Result.AddDocumentsResult::success)
                            .onErrorReturn(Result.AddDocumentsResult::failure)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(Result.AddDocumentsResult.adding());
                }
            });

    private final ObservableTransformer<Action.OpenDocumentAction,
                                        Result.OpenDocumentResult> openDocument =
            upstream -> upstream.flatMap(action -> {
                if (action.containerFile() == null) {
                    return Observable.just(Result.OpenDocumentResult.clear());
                } else {
                    return loadDocument(action.containerFile(), action.document())
                            .toObservable()
                            .map(Result.OpenDocumentResult::success)
                            .onErrorReturn(Result.OpenDocumentResult::failure)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(Result.OpenDocumentResult.opening());
                }
            });

    private final ObservableTransformer<Action.DocumentsSelectionAction,
                                        Result.DocumentsSelectionResult> documentsSelection =
            upstream -> upstream.map(action ->
                    Result.DocumentsSelectionResult.create(action.documents()));

    private final FileSystem fileSystem;

    @Inject
    Processor(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.merge(
                shared.ofType(Action.LoadContainerAction.class).compose(loadContainer),
                shared.ofType(Action.AddDocumentsAction.class).compose(addDocuments),
                shared.ofType(Action.OpenDocumentAction.class).compose(openDocument),
                shared.ofType(Action.DocumentsSelectionAction.class).compose(documentsSelection)));
    }

    private Single<SignatureContainer> loadContainer(File containerFile) {
        return Single.fromCallable(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }

            ImmutableList.Builder<Document> documentBuilder = ImmutableList.builder();
            DataFiles dataFiles = container.dataFiles();
            for (int i = 0; i < dataFiles.size(); i++) {
                documentBuilder.add(Document.create(dataFiles.get(i).fileName()));
            }

            return SignatureContainer.create(containerFile.getName(), documentBuilder.build(),
                    container.signatures().size() > 0);
        });
    }

    private Single<SignatureContainer> addDocuments(final File containerFile,
                                                    ImmutableList<FileStream> fileStreams) {
        return Completable.fromAction(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }
            for (FileStream fileStream : fileStreams) {
                File file = fileSystem.cache(fileStream);
                String mimeType = fileSystem.getMimeType(file);
                container.addDataFile(file.getAbsolutePath(), mimeType);
            }
            container.save();
        }).andThen(loadContainer(containerFile));
    }

    private Single<File> loadDocument(File containerFile, Document document) {
        return Single.fromCallable(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }

            DataFiles dataFiles = container.dataFiles();
            for (int i = 0; i < dataFiles.size(); i++) {
                DataFile dataFile = dataFiles.get(i);
                if (document.name().equals(dataFile.fileName())) {
                    File file = fileSystem.getCacheFile(document.name());
                    dataFile.saveAs(file.getAbsolutePath());
                    return file;
                }
            }

            throw new IllegalArgumentException("Could not find file " + document.name() +
                    " in container " + containerFile);
        });
    }
}
