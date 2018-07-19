package ee.ria.cryptolib;

import android.support.annotation.WorkerThread;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.openeid.cdoc4j.CDOCParser;
import org.openeid.cdoc4j.Recipient;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

import ee.ria.DigiDoc.core.Certificate;
import okio.ByteString;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;

@AutoValue
public abstract class CryptoContainer {

    private static final String EXTENSION = "cdoc";

    public abstract File file();

    public abstract ImmutableList<File> dataFiles();

    public abstract ImmutableList<Certificate> recipients();

    @WorkerThread
    public static CryptoContainer open(File file) throws InvalidCryptoContainerException {
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

            return new AutoValue_CryptoContainer(file, dataFilesBuilder.build(),
                    recipientsBuilder.build());
        } catch (Exception e) {
            throw new InvalidCryptoContainerException(e);
        }
    }

    public static boolean isContainerFileName(String fileName) {
        return getFileExtension(fileName).toLowerCase(Locale.US).equals(EXTENSION);
    }

    public static String createContainerFileName(String fileName) {
        return getNameWithoutExtension(fileName) + "." + EXTENSION;
    }
}
