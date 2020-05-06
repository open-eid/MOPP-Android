package ee.ria.DigiDoc.sign;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.threeten.bp.Instant;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.sign.utils.Function;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signature.Validator;
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
        for (int count : invalidSignatureCounts().values()) {
            if (count > 0) {
                return false;
            }
        }
        return true;
    }

    public final ImmutableMap<SignatureStatus, Integer> invalidSignatureCounts() {
        Map<SignatureStatus, Integer> counts = new HashMap<>();
        counts.put(SignatureStatus.UNKNOWN, 0);
        counts.put(SignatureStatus.INVALID, 0);
        for (Signature signature : signatures()) {
            if (counts.containsKey(signature.status())) {
                counts.put(signature.status(), counts.get(signature.status()) + 1);
            }
        }
        return ImmutableMap.copyOf(counts);
    }

    public final String signatureProfile() {
        return SIGNATURE_PROFILE_TS;
    }

    public final SignedContainer addDataFiles(ImmutableList<File> dataFiles) throws IOException {
        Container container = container(file());
        for (File dataFile : dataFiles) {
            container.addDataFile(dataFile.getAbsolutePath(), mimeType(dataFile));
        }
        container.save();
        return open(file());
    }

    public final SignedContainer removeDataFile(DataFile dataFile) throws
            ContainerDataFilesEmptyException, IOException {
        Container container = container(file());
        if (container.dataFiles().size() == 1) {
            throw new ContainerDataFilesEmptyException();
        }
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFile.size(); i++) {
            if (dataFile.id().equals(dataFiles.get(i).id())) {
                container.removeDataFile(i);
                break;
            }
        }
        container.save();
        return open(file());
    }

    public final File getDataFile(DataFile dataFile, File directory) throws IOException {
        Container container = container(file());
        File file = new File(directory, dataFile.name());
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFiles.size(); i++) {
            ee.ria.libdigidocpp.DataFile containerDataFile = dataFiles.get(i);
            if (dataFile.id().equals(containerDataFile.id())) {
                containerDataFile.saveAs(file.getAbsolutePath());
                return file;
            }
        }
        throw new IllegalArgumentException("Could not find file " + dataFile.id() +
                " in container " + file());
    }

    public final String calculateDataFileDigest(DataFile dataFile, String method) throws
            IOException {
        Container container = container(file());
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFiles.size(); i++) {
            ee.ria.libdigidocpp.DataFile containerDataFile = dataFiles.get(i);
            if (dataFile.id().equals(containerDataFile.id())) {
                return Base64.encodeToString(containerDataFile.calcDigest(method), Base64.DEFAULT);
            }
        }
        throw new IllegalArgumentException("Could not find file " + dataFile.id() +
                " in container " + file());
    }

    public final SignedContainer addAdEsSignature(byte[] adEsSignature) throws
            SignaturesLockedException, IOException {
        Container container = container(file());
        try {
            container.addAdESSignature(adEsSignature);
        } catch (Exception e) {
            throw new SignaturesLockedException();
        }
        container.save();
        return open(file());
    }

    public final SignedContainer sign(ByteString certificate,
                                      Function<ByteString, ByteString> signFunction) throws
            Exception {
        Container container = container(file());
        try {
            ee.ria.libdigidocpp.Signature signature = container
                    .prepareWebSignature(certificate.toByteArray(), signatureProfile());
            ByteString signatureData = signFunction.apply(ByteString.of(signature.dataToSign()));
            signature.setSignatureValue(signatureData.toByteArray());
            signature.extendSignatureProfile(signatureProfile());
            container.save();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                throw new TooManyRequestsException();
            }

            throw e;
        }
        return open(file());
    }

    public final SignedContainer removeSignature(Signature signature) throws IOException {
        Container container = container(file());
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
     * @throws IOException When file could not be found/opened.
     */
    public static SignedContainer open(File file) throws IOException {
        Container container = container(file);

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
                if (container(file).signatures().size() > 0) {
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
        return DataFile.create(dataFile.id(), new File(dataFile.fileName()).getName(),
                dataFile.fileSize(), dataFile.mediaType());
    }

    private static Signature signature(ee.ria.libdigidocpp.Signature signature) {
        String id = signature.id();
        String name = signatureName(signature);
        Instant createdAt = Instant.parse(signature.trustedSigningTime());
        SignatureStatus status = signatureStatus(signature);
        String profile = signature.profile();
        return Signature.create(id, name, createdAt, status, profile);
    }

    private static String signatureName(ee.ria.libdigidocpp.Signature signature) {
        String commonName;
        try {
            commonName = Certificate.create(ByteString.of(signature.signingCertificateDer()))
                    .commonName();
        } catch (IOException e) {
            Timber.e(e, "Can't parse certificate to get CN");
            commonName = null;
        }
        return commonName == null ? signature.signedBy() : commonName;
    }

    private static SignatureStatus signatureStatus(
            ee.ria.libdigidocpp.Signature signature) {
        Validator validator = new Validator(signature);
        int status = validator.status().swigValue();
        validator.delete();

        if (status == Validator.Status.Valid.swigValue()) {
            return SignatureStatus.VALID;
        } else if (status == Validator.Status.Warning.swigValue()) {
            return SignatureStatus.WARNING;
        } else if (status == Validator.Status.NonQSCD.swigValue()) {
            return SignatureStatus.NON_QSCD;
        } else if (status == Validator.Status.Invalid.swigValue()) {
            return SignatureStatus.INVALID;
        } else if (status == Validator.Status.Test.swigValue()) {
            return SignatureStatus.TEST;
        } else {
            return SignatureStatus.UNKNOWN;
        }
    }

    @NonNull
    private static Container container(File file) throws IOException {
        Container container;
        try {
            container = Container.open(file.getAbsolutePath());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        return container;
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
            return o1.createdAt().compareTo(o2.createdAt());
        }
        return v1 < v2 ? -1 : 1;
    };
}
