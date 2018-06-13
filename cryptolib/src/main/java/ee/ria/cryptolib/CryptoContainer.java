package ee.ria.cryptolib;

import android.support.annotation.WorkerThread;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.openeid.cdoc4j.Recipient;
import org.openeid.cdoc4j.xml.XMLDocumentBuilder;
import org.openeid.cdoc4j.xml.XmlEncParser;
import org.openeid.cdoc4j.xml.XmlEncParserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import ee.ria.DigiDoc.Certificate;
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
        try (InputStream inputStream = new FileInputStream(file)) {
            ImmutableList.Builder<File> dataFilesBuilder = ImmutableList.builder();
            ImmutableList.Builder<Certificate> recipientsBuilder = ImmutableList.builder();

            Document document = XMLDocumentBuilder.buildDocument(inputStream);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xPath
                    .compile("/EncryptedData/EncryptionProperties/EncryptionProperty" +
                            "[@Name='orig_file']");
            NodeList nodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                dataFilesBuilder.add(new File(node.getTextContent().split("\\|")[0]));
            }

            XmlEncParser parser = XmlEncParserFactory.getXmlEncParser(document);
            for (Recipient recipient : parser.getRecipients()) {
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
