package ee.ria.DigiDoc.crypto;

import com.google.common.collect.ImmutableList;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import ee.ria.DigiDoc.core.Certificate;

import static com.google.common.io.Files.asCharSource;
import static com.google.common.truth.Truth.assertThat;
import static ee.ria.DigiDoc.crypto.CryptoContainer.createContainerFileName;
import static ee.ria.DigiDoc.crypto.CryptoContainer.isContainerFileName;
import static ee.ria.DigiDoc.crypto.CryptoContainer.open;
import static java.nio.charset.Charset.defaultCharset;
import static okio.ByteString.decodeBase64;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class CryptoContainerTest {

    @Rule public final ExpectedException exception = ExpectedException.none();
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void open_rsaRecipient() throws Exception {
        File file = resource("example1_rsa.cdoc");

        CryptoContainer container = open(file);

        assertThat(container.file())
                .isEqualTo(file);
        assertThat(container.dataFiles())
                .containsExactly(new File("example1.txt"));
        assertThat(container.recipients())
                .containsExactly(certificate("37101010021_rsa.cer"));
        assertThat(container.decrypted())
                .isFalse();
    }

    @Test
    public void open_ecRecipient() throws Exception {
        File file = resource("example1_ec.cdoc");

        CryptoContainer container = open(file);

        assertThat(container.file())
                .isEqualTo(file);
        assertThat(container.dataFiles())
                .containsExactly(new File("example1.txt"));
        assertThat(container.recipients())
                .containsExactly(certificate("37101010021_ec.cer"));
        assertThat(container.decrypted())
                .isFalse();
    }

    @Test
    public void open_fileDoesNotExist() throws Exception {
        exception.expect(CryptoException.class);

        open(new File("does-not-exist.cdoc"));
    }

    @Test
    public void open_notCdocFile() throws Exception {
        exception.expect(CryptoException.class);

        open(resource("37101010021_ec.cer"));
    }

    @Ignore // java.security.InvalidKeyException: Illegal key size or default parameters
    @Test
    public void encrypt_valid() throws Exception {
        File dataFile1 = resource("example1.txt");
        File dataFile2 = resource("example2.txt");
        Certificate rsaCertificate = certificate("37101010021_rsa.cer");
        Certificate ecCertificate = certificate("37101010021_ec.cer");
        File file = temporaryFolder.newFile("test1.cdoc");

        CryptoContainer container = CryptoContainer.encrypt(
                ImmutableList.of(dataFile1, dataFile2),
                ImmutableList.of(rsaCertificate, ecCertificate),
                file);

        assertThat(container.file())
                .isEqualTo(file);
        assertThat(container.dataFiles())
                .containsExactly(new File(dataFile1.getName()), new File(dataFile2.getName()));
        assertThat(container.recipients())
                .containsExactly(ImmutableList.of(rsaCertificate, ecCertificate));
        assertThat(container.decrypted())
                .isFalse();
    }

    @Test
    public void encrypt_dataFilesEmpty() throws Exception {
        exception.expect(DataFilesEmptyException.class);

        CryptoContainer.encrypt(
                ImmutableList.of(),
                ImmutableList.of(certificate("37101010021_rsa.cer")),
                temporaryFolder.newFile("test1.cdoc"));
    }

    @Test
    public void encrypt_recipientsEmpty() throws Exception {
        exception.expect(RecipientsEmptyException.class);

        CryptoContainer.encrypt(
                ImmutableList.of(resource("example1.txt")),
                ImmutableList.of(),
                temporaryFolder.newFile("test1.cdoc"));
    }

    @Test
    public void decrypt_rsaRecipient_pin1Invalid() throws Exception {
        DecryptToken decryptToken = mock(DecryptToken.class);
        when(decryptToken.decrypt(any(), any(), anyBoolean()))
                .thenThrow(new Pin1InvalidException());

        CryptoContainer container = CryptoContainer.open(resource("example1_ec.cdoc"));

        exception.expect(Pin1InvalidException.class);
        container.decrypt(
                decryptToken,
                certificate("37101010021_rsa.cer"),
                "1234",
                temporaryFolder.getRoot());
    }

    @Test
    public void decrypt_ecRecipient_pin1Invalid() throws Exception {
        DecryptToken decryptToken = mock(DecryptToken.class);
        when(decryptToken.decrypt(any(), any(), anyBoolean()))
                .thenThrow(new Pin1InvalidException());

        CryptoContainer container = CryptoContainer.open(resource("example1_ec.cdoc"));

        exception.expect(Pin1InvalidException.class);
        container.decrypt(
                decryptToken,
                certificate("37101010021_ec.cer"),
                "1234",
                temporaryFolder.getRoot());
    }

    @Test
    public void isContainerFileName_cdocExtension() {
        assertThat(isContainerFileName("some-file.cdoc"))
                .isTrue();
    }

    @Test
    public void isContainerFileName_cdocExtensionCaseSensitive() {
        assertThat(isContainerFileName("some-file.cDoC"))
                .isTrue();
    }

    @Test
    public void isContainerFileName_otherExtension() {
        assertThat(isContainerFileName("some-file.bdoc"))
                .isFalse();
    }

    @Test
    public void createContainerFileName_pdf() {
        assertThat(createContainerFileName("some-file.pdf"))
                .isEqualTo("some-file.cdoc");
    }

    private File resource(String name) {
        return new File(getClass().getClassLoader().getResource(name).getFile());
    }

    @SuppressWarnings("ConstantConditions")
    private Certificate certificate(String name) throws IOException {
        return Certificate
                .create(decodeBase64(asCharSource(resource(name), defaultCharset()).read()));
    }
}
