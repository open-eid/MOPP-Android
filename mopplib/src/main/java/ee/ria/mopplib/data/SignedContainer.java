package ee.ria.mopplib.data;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x500.style.IETFUtils;
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

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFiles;
import ee.ria.libdigidocpp.Signatures;
import timber.log.Timber;

@AutoValue
public abstract class SignedContainer {

    public abstract ImmutableList<DataFile> dataFiles();

    public abstract ImmutableList<Signature> signatures();

    public final SignedContainer addDataFiles(ImmutableList<File> dataFiles) {
        return null;
    }

    public final SignedContainer removeDataFile(DataFile dataFile) throws
            ContainerDataFilesEmptyException {
        return null;
    }

    public final File getDataFile(DataFile dataFile, File directory) {
        return null;
    }

    public final SignedContainer addAdEsSignature(byte[] adEsSignature) throws
            SignaturesLockedException {
        return null;
    }

    public final SignedContainer removeSignature(Signature signature) throws
            SignaturesLockedException {
        return null;
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
        return null;
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

        return new AutoValue_SignedContainer(dataFileBuilder.build(), signatureBuilder.build());
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
            status = SignatureStatus.INVALID;
        }
        return Signature.create(id, name, createdAt, status);
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

        return IETFUtils.valueToString(rdNs[0].getFirst().getValue());
    }
}
