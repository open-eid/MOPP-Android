package ee.ria.DigiDoc.android.signature.create;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.libdigidocpp.Container;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.io.Files.getNameWithoutExtension;
import static ee.ria.DigiDoc.android.signature.data.SignatureContainer.isContainerFile;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ChooseFilesAction,
                                        Result.ChooseFilesResult> chooseFiles =
            upstream -> upstream.flatMap(action ->
                    Observable.just(Result.ChooseFilesResult.create()));

    private final ObservableTransformer<Action.CreateContainerAction,
                                        Result.CreateContainerResult> createContainer =
            upstream -> upstream.flatMap(action -> createSignatureContainer(action.fileStreams())
                    .toObservable()
                    .map(Result.CreateContainerResult::success)
                    .onErrorReturn(Result.CreateContainerResult::failure)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWith(Result.CreateContainerResult.inProgress()));

    private final FileSystem fileSystem;
    private final SettingsDataStore settingsDataStore;

    @Inject
    Processor(FileSystem fileSystem, SettingsDataStore settingsDataStore) {
        this.fileSystem = fileSystem;
        this.settingsDataStore = settingsDataStore;
    }

    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.merge(
                shared.ofType(Action.ChooseFilesAction.class).compose(chooseFiles),
                shared.ofType(Action.CreateContainerAction.class).compose(createContainer)));
    }

    private Single<File> createSignatureContainer(ImmutableList<FileStream> fileStreams) {
        return Single.fromCallable(() -> {
            File containerFile;
            if (fileStreams.size() == 1 && isContainerFile(fileStreams.get(0))) {
                FileStream fileStream = fileStreams.get(0);
                containerFile = fileSystem.addSignatureContainer(fileStream);
            } else {
                String containerName = String.format(Locale.US, "%s.%s",
                        getNameWithoutExtension(fileStreams.get(0).displayName()),
                        settingsDataStore.getFileType());
                containerFile = fileSystem.generateSignatureContainerFile(containerName);
                Container container = Container.create(containerFile.getAbsolutePath());
                if (container == null) {
                    for (FileStream fileStream : fileStreams) {
                        fileStream.inputStream().close();
                    }
                    throw new IOException("Could not create container file " + containerFile);
                }
                for (FileStream fileStream : fileStreams) {
                    File file = fileSystem.cache(fileStream);
                    String mimeType = fileSystem.getMimeType(file);
                    container.addDataFile(file.getAbsolutePath(), mimeType);
                }
                container.save();
            }
            return containerFile;
        });
    }
}
