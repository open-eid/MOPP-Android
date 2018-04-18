package ee.ria.mopplib.data;

import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.threeten.bp.Instant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Comparator;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signatures;
import okio.ByteString;
import timber.log.Timber;

import static com.google.common.collect.ImmutableList.sortedCopyOf;
import static com.google.common.io.Files.getFileExtension;

@AutoValue
public abstract class SignedContainer {

    private static final ImmutableSet<String> EXTENSIONS = ImmutableSet.<String>builder()
            .add("asice", "asics", "sce", "scs", "adoc", "bdoc", "ddoc", "edoc")
            .build();
    private static final ImmutableSet<String> NON_LEGACY_EXTENSIONS = ImmutableSet.<String>builder()
            .add("asice", "sce", "bdoc")
            .build();
    private static final String PDF_EXTENSION = "pdf";

    private static final String CONTAINER_MIME_TYPE = "application/octet-stream";
    private static final String DEFAULT_MIME_TYPE = "text/plain";

    private static final String SIGNATURE_PROFILE_TS = "time-stamp";
    private static final String SIGNATURE_PROFILE_TM = "time-mark";

    private static final ImmutableMap<String, String> SIGNATURE_PROFILES =
            ImmutableMap.<String, String>builder()
                    .put("asice", SIGNATURE_PROFILE_TS)
                    .put("sce", SIGNATURE_PROFILE_TS)
                    .put("bdoc", SIGNATURE_PROFILE_TM)
                    .build();

    public abstract File file();

    public final String name() {
        return file().getName();
    }

    public abstract ImmutableList<DataFile> dataFiles();

    public final boolean dataFileAddEnabled() {
        return !isLegacyContainer(file()) && signatures().size() == 0;
    }

    public final boolean dataFileRemoveEnabled() {
        return dataFileAddEnabled() && dataFiles().size() != 1;
    }

    public abstract ImmutableList<Signature> signatures();

    public final boolean signaturesValid() {
        for (Signature signature : signatures()) {
            if (!signature.status().equals(SignatureStatus.VALID)) {
                return false;
            }
        }
        return true;
    }

    public final String signatureProfile() {
        String extension = getFileExtension(file().getName());
        ImmutableList<Signature> signatures = signatures();

        if (signatures.size() == 1) {
            if (extension.equals("bdoc")) {
                return signatures.get(0).profile();
            } else {
                return SIGNATURE_PROFILE_TS;
            }
        } else if (signatures.size() > 1) {
            boolean same = true;
            String previous = null;
            for (Signature signature : signatures) {
                if (previous != null && !previous.equals(signature.profile())) {
                    same = false;
                    break;
                }
                previous = signature.profile();
            }
            if (same) {
                return previous;
            }
        }

        return SIGNATURE_PROFILES.get(extension);
    }

    public final SignedContainer addDataFiles(ImmutableList<File> dataFiles) throws IOException {
        Container container;
        try {
            container = Container.open(file().getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        for (File dataFile : dataFiles) {
            container.addDataFile(dataFile.getAbsolutePath(), mimeType(dataFile));
        }
        container.save();
        return open(file());
    }

    public final SignedContainer removeDataFile(DataFile dataFile) throws
            ContainerDataFilesEmptyException, IOException {
        Container container;
        try {
            container = Container.open(file().getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        if (container.dataFiles().size() == 1) {
            throw new ContainerDataFilesEmptyException();
        }
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFile.size(); i++) {
            if (dataFile.name().equals(dataFiles.get(i).fileName())) {
                container.removeDataFile(i);
                break;
            }
        }
        container.save();
        return open(file());
    }

    public final File getDataFile(DataFile dataFile, File directory) throws IOException {
        Container container;
        try {
            container = Container.open(file().getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        File file = new File(directory, dataFile.name());
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFiles.size(); i++) {
            ee.ria.libdigidocpp.DataFile containerDataFile = dataFiles.get(i);
            if (dataFile.name().equals(containerDataFile.fileName())) {
                containerDataFile.saveAs(file.getAbsolutePath());
                return file;
            }
        }
        throw new IllegalArgumentException("Could not find file " + dataFile.name() +
                " in container " + file());
    }

    public final SignedContainer addAdEsSignature(byte[] adEsSignature) throws
            SignaturesLockedException, IOException {
        Container container;
        try {
            container = Container.open(file().getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        try {
            container.addAdESSignature(adEsSignature);
        } catch (Exception e) {
            throw new SignaturesLockedException();
        }
        container.save();
        return open(file());
    }

    @SuppressWarnings("Guava")
    public final SignedContainer sign(ByteString certificate,
                                      Function<ByteString, ByteString> signFunction) throws
            IOException {
        Container container;
        try {
            container = Container.open(file().getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        ee.ria.libdigidocpp.Signature signature = container
                .prepareWebSignature(certificate.toByteArray(), signatureProfile());
        ByteString signatureData = signFunction.apply(ByteString.of(signature.dataToSign()));
        signature.setSignatureValue(signatureData.toByteArray());
        signature.extendSignatureProfile(signatureProfile());
        container.save();
        return open(file());
    }

    public final SignedContainer removeSignature(Signature signature) throws
            SignaturesLockedException, IOException {
        Container container;
        try {
            container = Container.open(file().getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        Signatures signatures = container.signatures();
        for (int i = 0; i < signatures.size(); i++) {
            if (signature.id().equals(signatures.get(i).id())) {
                container.removeSignature(i);
                break;
            }
        }
        container.save();
        return open(file());
    }

    /**
     * Create a new signed container with given data files.
     *
     * @param file Path to the created container.
     * @param dataFiles List of paths to data files.
     * @return New signed container with given data files and no signatures.
     * @throws IOException When given paths are inaccessible.
     * @throws ContainerDataFilesEmptyException When no data files are given.
     */
    public static SignedContainer create(File file, ImmutableList<File> dataFiles) throws
            IOException, ContainerDataFilesEmptyException {
        if (dataFiles == null || dataFiles.size() == 0) {
            throw new ContainerDataFilesEmptyException();
        }
        Container container;
        try {
            container = Container.create(file.getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        for (File dataFile : dataFiles) {
            container.addDataFile(dataFile.getAbsolutePath(), mimeType(dataFile));
        }
        container.save();
        return open(file);
    }

    /**
     * Open a signed container from {@link File}.
     *
     * @param file Path to existing container.
     * @return Signed container with data files and signatures.
     * @throws FileNotFoundException When file could not be found/opened.
     */
    public static SignedContainer open(File file) throws FileNotFoundException {
        Container container;
        try {
            container = Container.open(file.getAbsolutePath());
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
        if (container == null) {
            throw new FileNotFoundException("Container.open returned null");
        }

        ImmutableList.Builder<DataFile> dataFileBuilder = ImmutableList.builder();
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFiles.size(); i++) {
            dataFileBuilder.add(dataFile(dataFiles.get(i)));
        }

        ImmutableList.Builder<Signature> signatureBuilder = ImmutableList.builder();
        Signatures signatures = container.signatures();
        for (int i = 0; i < signatures.size(); i++) {
            signatureBuilder.add(signature(signatures.get(i)));
        }

        return new AutoValue_SignedContainer(file, dataFileBuilder.build(),
                sortedCopyOf(SIGNATURE_COMPARATOR, signatureBuilder.build()));
    }

    /**
     * Check whether this is a signature container file which should be opened as such
     * or a regular file which should be added to the container.
     *
     * @param file File to check.
     * @return True if it is a container, false otherwise.
     */
    public static boolean isContainer(File file) {
        String extension = getFileExtension(file.getName()).toLowerCase();
        if (EXTENSIONS.contains(extension)) {
            return true;
        }
        if (PDF_EXTENSION.equals(extension)) {
            try {
                Container container = Container.open(file.getAbsolutePath());
                if (container == null) {
                    Timber.d("Could not open PDF as signature container %s", file);
                    return false;
                }
                if (container.signatures().size() > 0) {
                    return true;
                }
            } catch (Exception e) {
                Timber.d("Could not open PDF as signature container %s", file);
            }
        }
        return false;
    }

    /**
     * Check whether this is a legacy container which needs to be wrapped in a valid container
     * type before adding signature.
     *
     * @param file File to check.
     * @return True if it is a legacy container, false otherwise.
     */
    public static boolean isLegacyContainer(File file) {
        String extension = getFileExtension(file.getName()).toLowerCase();
        return !NON_LEGACY_EXTENSIONS.contains(extension);
    }

    private static DataFile dataFile(ee.ria.libdigidocpp.DataFile dataFile) {
        return DataFile.create(dataFile.id(), dataFile.fileName(), dataFile.fileSize());
    }

    private static Signature signature(ee.ria.libdigidocpp.Signature signature) {
        String id = signature.id();
        String name = signatureName(signature);
        Instant createdAt = Instant.parse(signature.trustedSigningTime());
        @SignatureStatus String status;
        try {
            signature.validate();
            status = SignatureStatus.VALID;
        } catch (Exception e) {
            Timber.d(e, "Validation failed for signature {id: %s, name: %s, createdAt: %s}",
                    id, name, createdAt);
            status = SignatureStatus.INVALID;
        }
        String profile = signature.profile();
        return Signature.create(id, name, createdAt, status, profile);
    }

    private static String signatureName(ee.ria.libdigidocpp.Signature signature) {
        String certificateCn = certificateCN(signature.signingCertificateDer());
        return certificateCn == null ? signature.signedBy() : certificateCn;
    }

    @Nullable
    private static String certificateCN(byte[] signingCertificateDer) {
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

        return rdNs[0].getFirst().getValue().toString();
    }

    /**
     * Get MIME type from file extension.
     *
     * @param file File to get the extension from.
     * @return MIME type of the file.
     */
    public static String mimeType(File file) {
        String extension = getFileExtension(file.getName()).toLowerCase();
        if (EXTENSIONS.contains(extension)) {
            return CONTAINER_MIME_TYPE;
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }

    private static final Comparator<Signature> SIGNATURE_COMPARATOR = (o1, o2) -> {
        int v1 = SignatureStatus.ORDER.get(o1.status());
        int v2 = SignatureStatus.ORDER.get(o2.status());
        if (v1 == v2) {
            return 0;
        }
        return v1 < v2 ? -1 : 1;
    };
}
