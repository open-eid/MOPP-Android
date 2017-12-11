package ee.ria.DigiDoc.android.signature.data.source;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.threeten.bp.Instant;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signatures;
import io.reactivex.Completable;
import io.reactivex.Single;

import static com.google.common.io.Files.getNameWithoutExtension;
import static ee.ria.DigiDoc.android.signature.data.SignatureContainer.isContainerFile;

public final class FileSystemSignatureContainerDataSource implements SignatureContainerDataSource {

    private final FileSystem fileSystem;
    private final SettingsDataStore settingsDataStore;

    @Inject FileSystemSignatureContainerDataSource(FileSystem fileSystem,
                                                   SettingsDataStore settingsDataStore) {
        this.fileSystem = fileSystem;
        this.settingsDataStore = settingsDataStore;
    }

    @Override
    public Single<File> addContainer(ImmutableList<FileStream> fileStreams) {
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

    @Override
    public Single<SignatureContainer> get(File containerFile) {
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

            ImmutableList.Builder<Signature> signatureBuilder = ImmutableList.builder();
            Signatures signatures = container.signatures();
            for (int i = 0; i < signatures.size(); i++) {
                ee.ria.libdigidocpp.Signature signature = signatures.get(i);
                String id = signature.id();
                String name = signature.signedBy();
                Instant createdAt = Instant.parse(signature.trustedSigningTime());
                boolean valid;
                try {
                    signature.validate();
                    valid = true;
                } catch (Exception e) {
                    valid = false;
                }
                signatureBuilder.add(Signature.create(id, name, createdAt, valid));
            }

            return SignatureContainer.create(containerFile.getName(), documentBuilder.build(),
                    container.signatures().size() > 0, signatureBuilder.build());
        });
    }

    @Override
    public Completable addDocuments(File containerFile, ImmutableList<FileStream> documentStreams) {
        return Completable.fromAction(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }
            for (FileStream fileStream : documentStreams) {
                File file = fileSystem.cache(fileStream);
                String mimeType = fileSystem.getMimeType(file);
                container.addDataFile(file.getAbsolutePath(), mimeType);
            }
            container.save();
        });
    }

    @Override
    public Completable removeDocuments(File containerFile, ImmutableSet<Document> documents) {
        return Completable.fromAction(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }
            for (Document document : documents) {
                DataFiles dataFiles = container.dataFiles();
                for (int i = 0; i < dataFiles.size(); i++) {
                    if (document.name().equals(dataFiles.get(i).fileName())) {
                        container.removeDataFile(i);
                        break;
                    }
                }
            }
            container.save();
        });
    }

    @Override
    public Single<File> getDocumentFile(File containerFile, Document document) {
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
