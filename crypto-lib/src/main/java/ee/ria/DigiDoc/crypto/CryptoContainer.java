package ee.ria.DigiDoc.crypto;

import android.support.annotation.WorkerThread;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.openeid.cdoc4j.CDOCBuilder;
import org.openeid.cdoc4j.CDOCDecrypter;
import org.openeid.cdoc4j.CDOCParser;
import org.openeid.cdoc4j.Recipient;
import org.openeid.cdoc4j.exception.DataFileMissingException;
import org.openeid.cdoc4j.exception.RecipientMissingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import ee.ria.DigiDoc.core.Certificate;
import okio.ByteString;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;

/**
 * Handles working with CDOC containers.
 *
 * @see #open(File) Parse existing containers.
 * @see #encrypt(ImmutableList, ImmutableList, File) Create new containers.
 * @see #decrypt(DecryptToken, Certificate, String, File) Decrypt existing containers.
 */
@AutoValue
public abstract class CryptoContainer {

    private static final String EXTENSION = "cdoc";

    /**
     * Location of the container.
     */
    public abstract File file();

    /**
     * List of data files.
     *
     * When {@link #decrypted()} is true then it's an absolute path to the file,
     * otherwise only data file name is present.
     */
    public abstract ImmutableList<File> dataFiles();

    /**
     * List of recipients.
     */
    public abstract ImmutableList<Certificate> recipients();

    /**
     * Whether this container is decrypted.
     *
     * When decrypted the data files are with absolute paths.
     */
    public abstract boolean decrypted();

    /**
     * Decrypt the container.
     *
     * @param decryptToken Token used to decrypt.
     * @param authCertificate Authentication certificate.
     * @param pin1 PIN1 for the authentication certificate.
     * @param dataFilesDirectory Directory where the data files are saved.
     * @return Decrypted container.
     * @throws Pin1InvalidException When PIN1 is incorrect.
     * @throws CryptoException When decryption fails.
     */
    public CryptoContainer decrypt(DecryptToken decryptToken, Certificate authCertificate,
                                   String pin1, File dataFilesDirectory) throws CryptoException {
        if (decrypted()) {
            return this;
        }
        try {
            List<File> files = new CDOCDecrypter()
                    .withToken(
                            new PKCS11Token(decryptToken, authCertificate.x509Certificate(), pin1))
                    .withCDOC(file())
                    .decrypt(dataFilesDirectory);
            return create(file(), ImmutableList.copyOf(files), recipients(), true);
        } catch (Pin1InvalidException.DecryptPin1InvalidException e) {
            throw new Pin1InvalidException();
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }

    /**
     * Open an existing CDOC file.
     *
     * @param file Path to the file.
     * @return Container object.
     * @throws CryptoException When opening or parsing the container file fails.
     */
    @WorkerThread
    public static CryptoContainer open(File file) throws CryptoException {
        try (
                InputStream dataFilesStream = new FileInputStream(file);
                InputStream recipientsStream = new FileInputStream(file)
        ) {
            ImmutableList.Builder<File> dataFilesBuilder = ImmutableList.builder();
            ImmutableList.Builder<Certificate> recipientsBuilder = ImmutableList.builder();

            for (String dataFileName : CDOCParser.getDataFileNames(dataFilesStream)) {
                dataFilesBuilder.add(new File(dataFileName));
            }

            for (Recipient recipient : CDOCParser.getRecipients(recipientsStream)) {
                recipientsBuilder.add(Certificate
                        .create(ByteString.of(recipient.getCertificate().getEncoded())));
            }

            return create(file, dataFilesBuilder.build(), recipientsBuilder.build(), false);
        } catch (Exception e) {
            throw new CryptoException("Can't open crypto container", e);
        }
    }

    /**
     * Create a new crypto container with provided data files and recipients.
     *
     * @param dataFiles Data files.
     * @param recipients Recipients.
     * @param file Path to the created crypto container file.
     * @return Container object.
     * @throws DataFilesEmptyException When no data files provided.
     * @throws RecipientsEmptyException When no recipients provided.
     * @throws CryptoException When something failed with encryption.
     */
    @WorkerThread
    public static CryptoContainer encrypt(ImmutableList<File> dataFiles,
                                          ImmutableList<Certificate> recipients, File file)
            throws CryptoException {
        try {
            CDOCBuilder builder = CDOCBuilder.defaultVersion();
            for (File dataFile : dataFiles) {
                builder.withDataFile(dataFile);
            }
            for (Certificate recipient : recipients) {
                builder.withRecipient(recipient.x509Certificate());
            }
            builder.buildToFile(file);
            return open(file);
        } catch (DataFileMissingException e) {
            throw new DataFilesEmptyException();
        } catch (RecipientMissingException e) {
            throw new RecipientsEmptyException();
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    /**
     * Checks whether the name is a CDOC container file name by extension.
     *
     * @param fileName File name to check.
     * @return True if valid container file name.
     */
    public static boolean isContainerFileName(String fileName) {
        return getFileExtension(fileName).toLowerCase(Locale.US).equals(EXTENSION);
    }

    /**
     * Create a CDOC container file name based on some other file name.
     *
     * @param fileName Some other file name.
     * @return Valid container file name.
     */
    public static String createContainerFileName(String fileName) {
        return getNameWithoutExtension(fileName) + "." + EXTENSION;
    }

    private static CryptoContainer create(File file, ImmutableList<File> dataFiles,
                                          ImmutableList<Certificate> recipients,
                                          boolean decrypted) {
        return new AutoValue_CryptoContainer(file, dataFiles, recipients, decrypted);
    }
}
