package ee.ria.DigiDoc.android.signature.data.source;

import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x500.style.IETFUtils;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.threeten.bp.Instant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
    public Single<File> addContainer(ImmutableList<FileStream> fileStreams, boolean forceCreate) {
        return Single.fromCallable(() -> {
            File containerFile;
            if (!forceCreate && fileStreams.size() == 1 && isContainerFile(fileStreams.get(0))) {
                FileStream fileStream = fileStreams.get(0);
                containerFile = fileSystem.addSignatureContainer(fileStream);
            } else {
                String containerName = String.format(Locale.US, "%s.%s",
                        getNameWithoutExtension(fileStreams.get(0).displayName()),
                        settingsDataStore.getFileType());
                containerFile = fileSystem.generateSignatureContainerFile(containerName);
                Container container = Container.create(containerFile.getAbsolutePath());
                if (container == null) {
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
                DataFile dataFile = dataFiles.get(i);
                documentBuilder.add(Document.create(dataFile.fileName(), dataFile.fileSize()));
            }

            ImmutableList.Builder<Signature> signatureBuilder = ImmutableList.builder();
            Signatures signatures = container.signatures();
            for (int i = 0; i < signatures.size(); i++) {
                ee.ria.libdigidocpp.Signature signature = signatures.get(i);
                String id = signature.id();
                String name = getCertificateCN(signature.signingCertificateDer());
                if (name == null) {
                    name = signature.signedBy();
                }
                Instant createdAt = Instant.parse(signature.trustedSigningTime());
                boolean valid;
                try {
                    signature.validate();
                    valid = true;
                } catch (Exception e) {
                    Timber.d(e, "Signature validation failed");
                    valid = false;
                }
                signatureBuilder.add(Signature.create(id, name, createdAt, valid));
            }

            return SignatureContainer.create(containerFile.getName(), documentBuilder.build(),
                    signatureBuilder.build());
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
    public Completable removeDocument(File containerFile, Document document) {
        return Completable.fromAction(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }
            DataFiles dataFiles = container.dataFiles();
            for (int i = 0; i < dataFiles.size(); i++) {
                if (document.name().equals(dataFiles.get(i).fileName())) {
                    container.removeDataFile(i);
                    break;
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

    @Override
    public Completable removeSignature(File containerFile, Signature signature) {
        return Completable.fromAction(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }
            Signatures signatures = container.signatures();
            for (int i = 0; i < signatures.size(); i++) {
                if (signature.id().equals(signatures.get(i).id())) {
                    container.removeSignature(i);
                    break;
                }
            }
            container.save();
        });
    }

    @Override
    public Completable addSignature(File containerFile, String signature) {
        return Completable.fromAction(() -> {
            Container container = Container.open(containerFile.getAbsolutePath());
            if (container == null) {
                throw new IOException("Could not open signature container " + containerFile);
            }
            container.addAdESSignature(signature.getBytes(StandardCharsets.UTF_8));
            container.save();
        });
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
                Container container = Container.open(containerFile.getAbsolutePath());
                if (container != null && container.signatures().size() > 0) {
                    return true;
                }
            } catch (Exception e) {
                Timber.d(e, "Could not open PDF as signature container");
            }
        }
        return false;
    }

    @Nullable private static String getCertificateCN(byte[] signingCertificateDer) {
        if (signingCertificateDer == null || signingCertificateDer.length == 0) {
            return null;
        }

        X509Certificate certificate = null;
        try {
            certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(signingCertificateDer));
        } catch (CertificateException e) {
            Timber.e(e, "Error generating certificate");
        }
        if (certificate == null) {
            return null;
        }

        X500Name x500name = null;
        try {
            x500name = new JcaX509CertificateHolder(certificate).getSubject();
        } catch (CertificateEncodingException e) {
            Timber.e(e, "Error getting value by ASN1 Object identifier");
        }
        if (x500name == null) {
            return null;
        }

        RDN[] rdNs = x500name.getRDNs(ASN1ObjectIdentifier.getInstance(BCStyle.CN));
        if (rdNs.length == 0) {
            return null;
        }

        return IETFUtils.valueToString(rdNs[0].getFirst().getValue());
    }
}
