package ee.ria.DigiDoc.sign;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.annotation.Nullable;

import ee.ria.DigiDoc.common.Certificate;
import okio.ByteString;

public final class SignedContainerSubject extends Subject {

    static SignedContainerSubject assertThat(File file) throws Exception {
        return assertAbout(signedContainers())
                .that(SignedContainer.open(file));
    }

    private static Subject.Factory<SignedContainerSubject, SignedContainer> signedContainers() {
        return SignedContainerSubject::new;
    }

    private SignedContainerSubject(FailureMetadata metadata, @Nullable SignedContainer actual) {
        super(metadata, actual);
    }

    public void matchesMetadata(InputStream inputStream) throws IOException, JSONException, CertificateException {
        JSONObject metadata = containerMetadata(inputStream);
        hasName(metadata.getString("name"));
        hasDataFiles(dataFiles(metadata.getJSONArray("dataFiles")));
        isDataFileAddEnabled(metadata.getBoolean("dataFileAddEnabled"));
        isDataFileRemoveEnabled(metadata.getBoolean("dataFileRemoveEnabled"));
        hasSignatures(signatures(metadata.getJSONArray("signatures")));
        areSignaturesValid(metadata.getBoolean("signaturesValid"));
    }

    private void hasName(String name) {
        Truth.assertThat(name)
                .isEqualTo("example1.bdoc");
    }

    private void hasDataFiles(ImmutableList<DataFile> dataFiles) {
        DataFile dataFile = DataFile.create("text.txt", "text.txt", 3, "application/octet-stream");
        List<DataFile> filesList = List.of(dataFile);

        Truth.assertThat(dataFiles)
                .containsExactlyElementsIn(filesList)
                .inOrder();
    }

    private void isDataFileAddEnabled(boolean dataFileAddEnabled) {
        Truth.assertThat(dataFileAddEnabled)
                .isEqualTo(false);
    }

    private void isDataFileRemoveEnabled(boolean dataFileRemoveEnabled) {
        Truth.assertThat(dataFileRemoveEnabled)
                .isEqualTo(false);
    }

    private void hasSignatures(ImmutableList<Signature> signatures) {
        Signature signature0 = Signature.create("S0",
                "MARY ÄNN O'CONNEŽ-ŠUSLIK TESTNUMBER",
                Instant.parse("2022-03-21T12:03:22Z"),
                SignatureStatus.VALID,
                "BES/time-mark",
                "Issuer1",
                "signingCertificate1",
                null,
                "RSA",
                "SHA256",
                "2022-03-21T12:03:22Z",
                "2022-03-21T12:03:22Z",
                "Hash1",
                "TSIssuer1",
                null,
                "OCSPIssuer1",
                null,
                "2022-03-21T12:03:22Z",
                "2022-03-21T12:03:22Z",
                "2022-03-21T12:03:22Z");

        Signature signature1 = Signature.create("S1",
                "MARY ÄNN O'CONNEŽ-ŠUSLIK TESTNUMBER",
                Instant.parse("2022-03-21T21:22:00Z"),
                SignatureStatus.VALID,
                "BES/time-mark",
                "Issuer1",
                "signingCertificate1",
                null,
                "RSA",
                "SHA256",
                "2022-03-21T12:03:22Z",
                "2022-03-21T12:03:22Z",
                "Hash1",
                "TSIssuer1",
                null,
                "OCSPIssuer1",
                null,
                "2022-03-21T12:03:22Z",
                "2022-03-21T12:03:22Z",
                "2022-03-21T12:03:22Z");

        List<Signature> signatureList = List.of(signature0, signature1);

        Truth.assertThat(signatureList)
                .hasSize(signatures.size());
    }

    private void areSignaturesValid(boolean signaturesValid) {
        Truth.assertThat(signaturesValid)
                .isEqualTo(true);
    }

    private static JSONObject containerMetadata(InputStream inputStream) throws IOException,
            JSONException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        String string = CharStreams.toString(reader);
        reader.close();
        return (JSONObject) new JSONTokener(string).nextValue();
    }

    private static ImmutableList<DataFile> dataFiles(JSONArray metadata) throws JSONException {
        ImmutableList.Builder<DataFile> builder = ImmutableList.builder();
        for (int i = 0; i < metadata.length(); i++) {
            JSONObject dataFileMetadata = metadata.getJSONObject(i);
            builder.add(DataFile.create(
                    dataFileMetadata.getString("id"),
                    dataFileMetadata.getString("name"),
                    dataFileMetadata.getLong("size"),
                    dataFileMetadata.getString("mimeType")));
        }
        return builder.build();
    }

    private static ImmutableList<Signature> signatures(JSONArray metadata) throws JSONException, CertificateException, IOException {
        ImmutableList.Builder<Signature> builder = ImmutableList.builder();
        for (int i = 0; i < metadata.length(); i++) {
            JSONObject signatureMetadata = metadata.getJSONObject(i);
            builder.add(
                    Signature.create(signatureMetadata.getString("id"),
                    signatureMetadata.getString("name"),
                    Instant.parse(signatureMetadata.getString("createdAt")),
                    SignatureStatus.valueOf(signatureMetadata.getString("status")),
                    signatureMetadata.getString("diagnosticsInfo"),
                    signatureMetadata.getString("profile"),
                    signatureMetadata.getString("signersCertificateIssuer"),
                    Certificate.create(ByteString.of(Base64.getDecoder().decode(signatureMetadata.getString("signingCertificate")))).x509Certificate(),
                    signatureMetadata.getString("signatureMethod"),
                    signatureMetadata.getString("signatureFormat"),
                    signatureMetadata.getString("signatureTimestamp"),
                    signatureMetadata.getString("signatureTimestampUTC"),
                    signatureMetadata.getString("hashValueOfSignature"),
                    signatureMetadata.getString("tsCertificateIssuer"),
                    Certificate.create(ByteString.of(Base64.getDecoder().decode(signatureMetadata.getString("tsCertificate")))).x509Certificate(),
                    signatureMetadata.getString("ocspCertificateIssuer"),
                    Certificate.create(ByteString.of(Base64.getDecoder().decode(signatureMetadata.getString("ocspCertificate")))).x509Certificate(),
                    signatureMetadata.getString("ocspTime"),
                    signatureMetadata.getString("ocspTimeUTC"),
                    signatureMetadata.getString("signersMobileTimeUTC")));
        }
        return builder.build();
    }
}