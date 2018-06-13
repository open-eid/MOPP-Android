package ee.ria.cryptolib;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import ee.ria.DigiDoc.Certificate;

import static com.google.common.io.Files.asCharSource;
import static com.google.common.truth.Truth.assertThat;
import static ee.ria.cryptolib.CryptoContainer.createContainerFileName;
import static ee.ria.cryptolib.CryptoContainer.isContainerFileName;
import static ee.ria.cryptolib.CryptoContainer.open;
import static java.nio.charset.Charset.defaultCharset;
import static okio.ByteString.decodeBase64;

@SuppressWarnings("ConstantConditions")
public final class CryptoContainerTest {

    @Rule public final ExpectedException exception = ExpectedException.none();

    @Test
    public void open_validContainer() throws Exception {
        File containerFile = new File(
                getClass().getClassLoader().getResource("example1.cdoc").getFile());
        File certificateFile = new File(
                getClass().getClassLoader().getResource("example1.cert").getFile());

        CryptoContainer container = open(containerFile);

        File dataFile = new File("example1.txt");
        Certificate recipient = Certificate
                .create(decodeBase64(asCharSource(certificateFile, defaultCharset()).read()));

        assertThat(container.file())
                .isEqualTo(containerFile);
        assertThat(container.dataFiles())
                .containsExactly(dataFile);
        assertThat(container.recipients())
                .containsExactly(recipient);
    }

    @Test
    public void open_fileDoesNotExist() throws Exception {
        exception.expect(InvalidCryptoContainerException.class);
        open(new File("does-not-exist.cdoc"));
    }

    @Test
    public void open_notCdocFile() throws Exception {
        exception.expect(InvalidCryptoContainerException.class);
        open(new File(getClass().getClassLoader().getResource("example1.cert").getFile()));
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
}
