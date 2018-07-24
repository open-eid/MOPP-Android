package ee.ria.DigiDoc.android.signature.data.source;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.ContainerAdd;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.Completable;
import io.reactivex.Single;

import static com.google.common.io.Files.getNameWithoutExtension;

public final class FileSystemSignatureContainerDataSource implements SignatureContainerDataSource {

    private final FileSystem fileSystem;
    private final SettingsDataStore settingsDataStore;

    @Inject FileSystemSignatureContainerDataSource(FileSystem fileSystem,
                                                   SettingsDataStore settingsDataStore) {
        this.fileSystem = fileSystem;
        this.settingsDataStore = settingsDataStore;
    }

    @Override
    public Single<ImmutableList<File>> find() {
        return Single.fromCallable(fileSystem::findSignatureContainerFiles);
    }

    @Override
    public Single<ContainerAdd> addContainer(ImmutableList<FileStream> fileStreams,
                                             boolean forceCreate) {
        return Single.fromCallable(() -> {
            boolean isExistingContainer;
            File containerFile;
            if (!forceCreate && fileStreams.size() == 1
                    && SignedContainer.isContainer(fileSystem.cache(fileStreams.get(0)))) {
                FileStream fileStream = fileStreams.get(0);
                isExistingContainer = true;
                containerFile = fileSystem.addSignatureContainer(fileStream);
            } else {
                String containerName = String.format(Locale.US, "%s.%s",
                        getNameWithoutExtension(fileStreams.get(0).displayName()),
                        settingsDataStore.getFileType());
                isExistingContainer = false;
                containerFile = fileSystem.generateSignatureContainerFile(containerName);
                SignedContainer.create(containerFile, cacheFileStreams(fileStreams));
            }
            return ContainerAdd.create(isExistingContainer, containerFile);
        });
    }

    @Override
    public Single<SignedContainer> get(File containerFile) {
        return Single.fromCallable(() -> SignedContainer.open(containerFile));
    }

    @Override
    public Completable remove(File containerFile) {
        //noinspection ResultOfMethodCallIgnored
        return Completable.fromAction(containerFile::delete);
    }

    @Override
    public Single<SignedContainer> addDocuments(File containerFile,
                                                ImmutableList<FileStream> documentStreams) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .addDataFiles(cacheFileStreams(documentStreams)));
    }

    @Override
    public Single<SignedContainer> removeDocument(File containerFile, DataFile document) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .removeDataFile(document));
    }

    @Override
    public Single<File> getDocumentFile(File containerFile, DataFile document) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .getDataFile(document, dataFileDirectory(containerFile)));
    }

    @Override
    public Single<SignedContainer> removeSignature(File containerFile, Signature signature) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .removeSignature(signature));
    }

    @Override
    public Single<SignedContainer> addSignature(File containerFile, String signature) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .addAdEsSignature(signature.getBytes()));
    }

    private ImmutableList<File> cacheFileStreams(ImmutableList<FileStream> fileStreams) throws
            IOException {
        ImmutableList.Builder<File> fileBuilder = ImmutableList.builder();
        for (FileStream fileStream : fileStreams) {
            fileBuilder.add(fileSystem.cache(fileStream));
        }
        return fileBuilder.build();
    }

    private File dataFileDirectory(File containerFile) {
        File directory;
        if (containerFile.getParentFile().equals(fileSystem.signatureContainersDir())) {
            directory = createDataFileDirectory(fileSystem.getCacheDir(), containerFile);
        } else {
            directory = createDataFileDirectory(containerFile.getParentFile(), containerFile);
        }
        return directory;
    }

    private static final String DATA_FILE_DIR = "%s-data-files";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File createDataFileDirectory(File directory, File container) {
        File dir;
        int i = 0;
        while (true) {
            StringBuilder name = new StringBuilder(
                    String.format(Locale.US, DATA_FILE_DIR, container.getName()));
            if (i > 0) {
                name.append(i);
            }
            dir = new File(directory, name.toString());
            if (dir.isDirectory() || !dir.exists()) {
                break;
            }
            i++;
        }
        dir.mkdirs();
        dir.mkdir();
        return dir;
    }
}
