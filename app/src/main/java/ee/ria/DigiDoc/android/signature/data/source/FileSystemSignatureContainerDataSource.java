package ee.ria.DigiDoc.android.signature.data.source;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.ContainerAdd;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Completable;
import io.reactivex.Single;
import timber.log.Timber;

import static com.google.common.io.Files.getNameWithoutExtension;

public final class FileSystemSignatureContainerDataSource implements SignatureContainerDataSource {

    private static final ImmutableSet<String> EXTENSIONS = ImmutableSet.<String>builder()
            .add("asice", "asics", "sce", "scs", "adoc", "bdoc", "ddoc", "edoc")
            .build();
    private static final String PDF_EXTENSION = "pdf";

    private final FileSystem fileSystem;
    private final SettingsDataStore settingsDataStore;

    @Inject FileSystemSignatureContainerDataSource(FileSystem fileSystem,
                                                   SettingsDataStore settingsDataStore) {
        this.fileSystem = fileSystem;
        this.settingsDataStore = settingsDataStore;
    }

    @Override
    public Single<ContainerAdd> addContainer(ImmutableList<FileStream> fileStreams,
                                             boolean forceCreate) {
        return Single.fromCallable(() -> {
            boolean isExistingContainer;
            File containerFile;
            if (!forceCreate && fileStreams.size() == 1 && isContainerFile(fileStreams.get(0))) {
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
    public Completable addDocuments(File containerFile, ImmutableList<FileStream> documentStreams) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .addDataFiles(cacheFileStreams(documentStreams)));
    }

    @Override
    public Completable removeDocument(File containerFile, DataFile document) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .removeDataFile(document));
    }

    @Override
    public Single<File> getDocumentFile(File containerFile, DataFile document) {
        return Single.fromCallable(() ->
                SignedContainer
                        .open(containerFile)
                        .getDataFile(document, fileSystem.getCacheDir()));
    }

    @Override
    public Completable removeSignature(File containerFile, Signature signature) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .removeSignature(signature));
    }

    @Override
    public Completable addSignature(File containerFile, String signature) {
        return Completable.fromAction(() ->
                SignedContainer
                        .open(containerFile)
                        .addAdEsSignature(signature.getBytes()));
    }

    /**
     * Check whether this is a signature container file which should be opened as such
     * or a regular file which should be added to the container.
     *
     * @param fileStream File stream containing information about the document.
     * @return True if it is a container, false otherwise.
     */
    private boolean isContainerFile(FileStream fileStream) throws IOException {
        String extension = Files.getFileExtension(fileStream.displayName()).toLowerCase();
        if (EXTENSIONS.contains(extension)) {
            return true;
        }
        if (PDF_EXTENSION.equals(extension)) {
            File containerFile = fileSystem.cache(fileStream);
            try {
                SignedContainer container = SignedContainer.open(containerFile);
                if (container.signatures().size() > 0) {
                    return true;
                }
            } catch (Exception e) {
                Timber.d(e, "Could not open PDF as signature container");
            }
        }
        return false;
    }

    private ImmutableList<File> cacheFileStreams(ImmutableList<FileStream> fileStreams) throws
            IOException {
        ImmutableList.Builder<File> fileBuilder = ImmutableList.builder();
        for (FileStream fileStream : fileStreams) {
            fileBuilder.add(fileSystem.cache(fileStream));
        }
        return fileBuilder.build();
    }
}
