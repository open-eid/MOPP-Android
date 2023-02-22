package ee.ria.DigiDoc.android.signature.data.source;

import com.google.common.collect.ImmutableList;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.signature.data.ContainerAdd;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import static com.google.common.io.Files.getNameWithoutExtension;
import static ee.ria.DigiDoc.android.Constants.SIGNATURE_CONTAINER_EXT;

import android.content.Context;

public final class FileSystemSignatureContainerDataSource implements SignatureContainerDataSource {

    private final FileSystem fileSystem;

    @Inject FileSystemSignatureContainerDataSource(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public Single<ImmutableList<File>> find() {
        return Single.fromCallable(fileSystem::findSignatureContainerFiles);
    }

    @Override
    public Single<ContainerAdd> addContainer(Context context, ImmutableList<FileStream> fileStreams,
                                             boolean forceCreate) {
        return Single.fromCallable(() -> {
            boolean isExistingContainer;
            File containerFile;
            if (!forceCreate && fileStreams.size() == 1
                    && SignedContainer.isContainer(context, fileSystem.cache(fileStreams.get(0)))) {
                FileStream fileStream = fileStreams.get(0);
                isExistingContainer = true;
                containerFile = fileSystem.addSignatureContainer(fileStream);
            } else {
                String normalizedDisplayName = FilenameUtils.getName(FileUtil.sanitizeString(FileUtil.normalizePath(
                        fileStreams.get(0).displayName()).getPath(), ""));
                String containerName = String.format(Locale.US, "%s.%s",
                        FilenameUtils.removeExtension(normalizedDisplayName),
                        SIGNATURE_CONTAINER_EXT);
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
                        .addDataFiles(cacheFileStreams(getContainerFiles(containerFile, documentStreams))));
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
                        .getDataFile(document, fileSystem.getContainerDataFilesDir(containerFile)));
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
                        .addAdEsSignature(signature.getBytes(StandardCharsets.UTF_8)));
    }

    private ImmutableList<FileStream> getContainerFiles(File containerFile, ImmutableList<FileStream> documentStreams) throws Exception {
        ImmutableList.Builder<FileStream> fileStreamList = ImmutableList.builder();
        List<String> fileNamesInContainer = getFileNamesInContainer(containerFile);
        List<String> fileNamesToAdd = getFileNamesToAddToContainer(documentStreams);
        for (int i = 0; i < fileNamesToAdd.size(); i++) {
            if (!fileNamesInContainer.contains(fileNamesToAdd.get(i))) {
                fileStreamList.add(documentStreams.get(i));
            }
        }

        if (fileStreamList.build().isEmpty()) {
            return documentStreams;
        }

        return fileStreamList.build();
    }

    private List<String> getFileNamesInContainer(File containerFile) throws Exception {
        List<String> containerFileNames = new ArrayList<>();
        ImmutableList<DataFile> dataFiles = SignedContainer.open(containerFile).dataFiles();

        for (int i = 0; i < dataFiles.size(); i++) {
            containerFileNames.add(dataFiles.get(i).name());
        }

        return containerFileNames;
    }

    private List<String> getFileNamesToAddToContainer(ImmutableList<FileStream> documentStreams) {
        List<String> documentNamesToAdd = new ArrayList<>();
        for (FileStream fileStream : documentStreams) {
            documentNamesToAdd.add(fileStream.displayName());
        }

        return documentNamesToAdd;
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
