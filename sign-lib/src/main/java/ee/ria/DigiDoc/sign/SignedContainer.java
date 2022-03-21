package ee.ria.DigiDoc.sign;

import static com.google.common.collect.ImmutableList.sortedCopyOf;
import static com.google.common.io.Files.getFileExtension;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.util.encoders.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.common.TextUtil;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import ee.ria.DigiDoc.sign.utils.Function;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signature.Validator;
import ee.ria.libdigidocpp.Signatures;
import ee.ria.libdigidocpp.StringVector;
import okio.ByteString;
import timber.log.Timber;

@AutoValue
public abstract class SignedContainer {

    private static final ImmutableSet<String> ASICS_EXTENSIONS = ImmutableSet.of("asics", "scs");

    private static final ImmutableSet<String> EXTENSIONS = ImmutableSet.<String>builder()
            .add("asice", "sce", "adoc", "bdoc", "ddoc", "edoc")
            .addAll(ASICS_EXTENSIONS)
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
        return FileUtil.sanitizeString(file().getName(), "");
    }

    public abstract ImmutableList<DataFile> dataFiles();

    public final boolean dataFileAddEnabled() {
        return !isLegacyContainer(file()) && signatures().size() == 0;
    }

    public final boolean dataFileRemoveEnabled() {
        return dataFileAddEnabled();
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

    public final SignedContainer addDataFiles(ImmutableList<File> dataFiles) throws Exception {
        Container container = container(file());
        for (File dataFile : dataFiles) {
            container.addDataFile(dataFile.getAbsolutePath(), mimeType(dataFile));
        }
        container.save();
        return open(file());
    }

    public final SignedContainer removeDataFile(DataFile dataFile) throws Exception {
        Container container = container(file());
        if (container.dataFiles().size() == 1) {
            throw new ContainerDataFilesEmptyException();
        }
        DataFiles dataFiles = container.dataFiles();
        for (int i = 0; i < dataFiles.size(); i++) {
            if (dataFile.id().equals(dataFiles.get(i).id())) {
                container.removeDataFile(i);
                break;
            }
        }
        container.save();
        return open(file());
    }

    public final File getDataFile(DataFile dataFile, File directory) throws Exception {
        Container container = container(file());
        File file = new File(directory, FileUtil.sanitizeString(dataFile.name(), ""));
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

    public final String calculateDataFileDigest(DataFile dataFile, String method) throws Exception {
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

    public final SignedContainer addAdEsSignature(byte[] adEsSignature) throws Exception {
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
                                      Function<ByteString, ByteString> signFunction,
                                      @Nullable RoleData roleData) throws Exception {
        try {
            Container container = container(file());
          
            ee.ria.libdigidocpp.Signature signature;
            if (roleData != null) {
                signature = container.prepareWebSignature(certificate.toByteArray(), signatureProfile(),
                        new StringVector(TextUtil.removeEmptyStrings(roleData.roles())), roleData.city(),
                        roleData.state(), roleData.zip(), roleData.country());
            } else {
                signature = container.prepareWebSignature(certificate.toByteArray(), signatureProfile());
            }
            if (signature != null) {
                ByteString signatureData = signFunction.apply(ByteString.of(signature.dataToSign()));
                signature.setSignatureValue(signatureData.toByteArray());
                signature.extendSignatureProfile(signatureProfile());
                container.save();
                return open(file());
            }
            throw new Exception("Empty signature value");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Too Many Requests")) {
                Timber.log(Log.ERROR, e, "Failed to sign with ID-card - Too Many Requests");
                throw new TooManyRequestsException();
            }
            if (e.getMessage() != null && e.getMessage().contains("OCSP response not in valid time slot")) {
                Timber.log(Log.ERROR, e, "Failed to sign with ID-card - OCSP response not in valid time slot");
                throw new OcspInvalidTimeSlotException();
            }
            if (e.getMessage() != null && e.getMessage().contains("Certificate status: revoked")) {
                Timber.log(Log.ERROR, e, "Failed to sign with ID-card - Certificate status: revoked");
                throw new CertificateRevokedException();
            }
            if (e.getMessage() != null && e.getMessage().contains("Failed to connect")) {
                Timber.log(Log.ERROR, e, "Failed to connect to Internet");
                throw new NoInternetConnectionException();
            }

            throw e;
        }
    }

    public final SignedContainer removeSignature(Signature signature) throws Exception {
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

    public boolean hasEmptyFiles() {
        for (DataFile dataFile : dataFiles()) {
            if (dataFile.size() == 0) {
                return true;
            }
        }

        return false;
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
    public static SignedContainer create(File file, ImmutableList<File> dataFiles) throws Exception {
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
    public static SignedContainer open(File file) throws Exception {
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
    public static boolean isContainer(Context context, File file) throws Exception {
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
                if (e instanceof NoInternetConnectionException) {
                    if (isSignedPDF(context, file)) {
                        throw e;
                    }
                }
                return false;
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

    public static String getMediaType(File file) throws Exception {
         return container(file).mediaType();
    }

    private static DataFile dataFile(ee.ria.libdigidocpp.DataFile dataFile) {
        return DataFile.create(dataFile.id(), new File(dataFile.fileName()).getName(),
                dataFile.fileSize(), dataFile.mediaType());
    }

    private static X509Certificate x509Certificate(byte[] bytes) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            Timber.log(Log.ERROR, "Can't parse certificate", e);
            return null;
        }
    }

    private static String getX509CertificateIssuer(X509Certificate x509Certificate) {
        try {
            X500Name x500name = new JcaX509CertificateHolder(x509Certificate).getIssuer();
            RDN cn = x500name.getRDNs(BCStyle.CN)[0];
            return IETFUtils.valueToString(cn.getFirst().getValue());
        } catch (CertificateEncodingException e) {
            Timber.log(Log.ERROR, "Unable to get certificate issuer", e);
            return "";
        }
    }

    private static String toHexString(byte[] bytes) {
        return TextUtils.join(" ",
                Splitter.fixedLength(2).split(Hex.toHexString(bytes))).trim();
    }

    private static Signature signature(ee.ria.libdigidocpp.Signature signature) {
        String id = signature.id();
        String name = signatureName(signature);
        Instant createdAt = Instant.parse(signature.trustedSigningTime());
        SignatureStatus status = signatureStatus(signature);
        String profile = signature.profile();

        String signersCertificateIssuer = "";
        X509Certificate signingCertificate = null;

        if (x509Certificate(signature.signingCertificateDer()) != null) {
            signersCertificateIssuer = getX509CertificateIssuer(x509Certificate(signature.signingCertificateDer()));
            signingCertificate = x509Certificate(signature.signingCertificateDer());
        }
        String signatureMethod = signature.signatureMethod();
        String signatureFormat = signature.profile();
        String signatureTimestamp = getFormattedDateTime(signature.trustedSigningTime(), false);
        String signatureTimestampUTC = getFormattedDateTime(signature.trustedSigningTime(), true);
        String hashValueOfSignature = toHexString(signature.messageImprint());

        String tsCertificateIssuer = "";
        X509Certificate tsCertificate = null;
        if (signature.TimeStampCertificateDer() != null && signature.TimeStampCertificateDer().length > 0) {
            tsCertificate = x509Certificate(signature.TimeStampCertificateDer());
            if (tsCertificate != null) {
                tsCertificateIssuer = getX509CertificateIssuer(tsCertificate);
            }
        }

        String ocspCertificateIssuer = "";
        X509Certificate ocspCertificate = null;
        if (signature.OCSPCertificateDer() != null && signature.OCSPCertificateDer().length > 0) {
            ocspCertificate = x509Certificate(signature.OCSPCertificateDer());
            if (ocspCertificate != null) {
                ocspCertificateIssuer = getX509CertificateIssuer(ocspCertificate);
            }
        }

        String ocspTime = "";
        String ocspTimeUTC = "";
        if (!signature.OCSPProducedAt().isEmpty()) {
            ocspTime = getFormattedDateTime(signature.OCSPProducedAt(), false);
        }
        if (!signature.OCSPProducedAt().isEmpty()) {
            ocspTimeUTC = getFormattedDateTime(signature.OCSPProducedAt(), true);
        }

        String signersMobileTimeUTC = getFormattedDateTime(signature.claimedSigningTime(), true);

        StringVector roles = signature.signerRoles();
        String city = signature.city();
        String state = signature.stateOrProvince();
        String country = signature.countryName();
        String zip = signature.postalCode();

        return Signature.create(id, name, createdAt, status, profile, signersCertificateIssuer,
                signingCertificate, signatureMethod, signatureFormat, signatureTimestamp,
                signatureTimestampUTC, hashValueOfSignature, tsCertificateIssuer, tsCertificate,
                ocspCertificateIssuer, ocspCertificate, ocspTime, ocspTimeUTC, signersMobileTimeUTC,
                roles, city, state, country, zip);
    }

    private static String getFormattedDateTime(String dateTimeString, boolean isUTC) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            if (isUTC) {
                return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(dateFormat.parse(dateTimeString)) + " +0000";
            }
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return new SimpleDateFormat("dd.MM.yyyy HH:mm:ssZ").format(dateFormat.parse(dateTimeString));
        } catch (ParseException | IllegalStateException e) {
            Timber.log(Log.ERROR, e, "Unable to parse date");
        }
        return "";
    }

    private static String signatureName(ee.ria.libdigidocpp.Signature signature) {
        String commonName;
        try {
            commonName = Certificate.create(ByteString.of(signature.signingCertificateDer()))
                    .friendlyName();
        } catch (IOException e) {
            Timber.log(Log.ERROR, e, "Can't parse certificate to get CN");
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
        } else {
            return SignatureStatus.UNKNOWN;
        }
    }

    @NonNull
    private static Container container(File file) throws Exception {
        Container container;
        try {
            container = Container.open(file.getAbsolutePath());
        } catch (Exception e) {
            if (e.getMessage().startsWith("Failed to connect to host")) {
                throw new NoInternetConnectionException();
            }
            throw new IOException(e.getMessage());
        }
        if (container == null) {
            throw new IOException("Container.open returned null");
        }
        return container;
    }

    /**
     * Check whether a PDF file is signed or not.
     *
     * @param file File to check.
     * @return True if it is a container, false otherwise.
     */
    private static boolean isSignedPDF(Context context, File file) {
        PDFBoxResourceLoader.init(context);
        try (PDDocument document = PDDocument.load(file)) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            for (PDSignature signature : signatures) {
                String filter = signature.getFilter();
                String subFilter = signature.getSubFilter();

                if (filter.equals("Adobe.PPKLite") ||
                        (subFilter.equals("ETSI.CAdES.detached") ||
                                subFilter.equals("adbe.pkcs7.detached"))) {
                    return true;
                }
            }
        } catch (IOException e) {
            Timber.log(Log.ERROR, e, "Unable to check if PDF is signed");
        }
        return false;
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

    /**
     * Check if file is signed PDF file.
     *
     * @param byteSource ByteSource of the file.
     * @return boolean true if file is signed PDF file. False otherwise.
     */
    public static boolean isSignedPDFFile(ByteSource byteSource, Context context, String fileName) throws IllegalStateException {
        Timber.log(Log.DEBUG, "Checking if PDF is signed");

        try (InputStream in = byteSource.openStream()) {
            final int length = (int) byteSource.size();
            byte[] bytes = new byte[length];

            int offset = 0;

            Timber.log(Log.DEBUG, "Reading PDF bytes");

            while (offset < length) {
                int read = in.read(bytes, offset, length - offset);
                if (read == -1) {
                    throw new EOFException("Unexpected end of input");
                }
                offset += read;
            }

            File pdfFilesDirectory = new File(context.getFilesDir(), "tempPdfFiles");

            FileUtils.createDirectoryIfNotExist(pdfFilesDirectory.toString());

            File file = new File(pdfFilesDirectory, String.format(Locale.US, "%s",
                    FilenameUtils.getName(FileUtil.sanitizeString(fileName, ""))));

            if (!org.apache.commons.io.FileUtils.directoryContains(pdfFilesDirectory, file)) {
                try (OutputStream outStream = new FileOutputStream(file.getCanonicalPath())) {
                    outStream.write(bytes);
                }
            }

            boolean isSignedContainer = SignedContainer.isContainer(context, file);
            FileUtils.removeFile(file.getCanonicalPath());
            FileUtils.removeFile(pdfFilesDirectory.getCanonicalPath());

            Timber.log(Log.DEBUG, String.format("Is PDF signed: %s", isSignedContainer));

            return isSignedContainer;
        } catch (Exception e) {
            Timber.log(Log.ERROR, e,
                    String.format("Unable to check if PDF file is signed. Error: %s",
                    e.getLocalizedMessage()));
            return false;
        }
    }

    public static boolean isCdoc(File file) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nodes = doc.getElementsByTagName("denc:EncryptionProperty");
            for (int i = 0; i < nodes.getLength(); i++) {
                NamedNodeMap attributes = nodes.item(i).getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    if (attributes.item(j).getNodeValue().equals("DocumentFormat")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "XML parsing failed");
            return false;
        }

        return false;
    }

    public static boolean isDdoc(File file) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nodes = doc.getElementsByTagName("SignedDoc");
            for (int i = 0; i < nodes.getLength(); i++) {
                NamedNodeMap attributes = nodes.item(i).getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    if (attributes.item(j).getNodeValue().equals("DIGIDOC-XML")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "XML parsing failed");
            return false;
        }

        return false;
    }
    public static boolean isAsicsFile(String fileName) {
        return ASICS_EXTENSIONS.contains(Files.getFileExtension(fileName).toLowerCase());
    }
}
