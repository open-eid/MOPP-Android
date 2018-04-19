package ee.ria.mopplib;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Nullable;

import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;

import static com.google.common.truth.Truth.assertAbout;

public final class SignedContainerSubject extends Subject<SignedContainerSubject, SignedContainer> {

    public static SignedContainerSubject assertThat(File file)
            throws IOException {
        return assertAbout(signedContainers())
                .that(SignedContainer.open(file));
    }

    private static Subject.Factory<SignedContainerSubject, SignedContainer> signedContainers() {
        return SignedContainerSubject::new;
    }

    private SignedContainerSubject(FailureMetadata metadata, @Nullable SignedContainer actual) {
        super(metadata, actual);
    }

    public void matchesMetadata(InputStream inputStream) throws IOException, JSONException {
        JSONObject metadata = containerMetadata(inputStream);
        hasName(metadata.getString("name"));
        hasDataFiles(dataFiles(metadata.getJSONArray("dataFiles")));
        isDataFileAddEnabled(metadata.getBoolean("dataFileAddEnabled"));
        isDataFileRemoveEnabled(metadata.getBoolean("dataFileRemoveEnabled"));
        hasSignatures(signatures(metadata.getJSONArray("signatures")));
        areSignaturesValid(metadata.getBoolean("signaturesValid"));
    }

    private void hasName(String name) {
        Truth.assertThat(actual().name())
                .isEqualTo(name);
    }

    private void hasDataFiles(ImmutableList<DataFile> dataFiles) {
        Truth.assertThat(actual().dataFiles())
                .containsExactlyElementsIn(dataFiles)
                .inOrder();
    }

    private void isDataFileAddEnabled(boolean dataFileAddEnabled) {
        Truth.assertThat(actual().dataFileAddEnabled())
                .isEqualTo(dataFileAddEnabled);
    }

    private void isDataFileRemoveEnabled(boolean dataFileRemoveEnabled) {
        Truth.assertThat(actual().dataFileRemoveEnabled())
                .isEqualTo(dataFileRemoveEnabled);
    }

    private void hasSignatures(ImmutableList<Signature> signatures) {
        Truth.assertThat(actual().signatures())
                .containsExactlyElementsIn(signatures)
                .inOrder();
    }

    private void areSignaturesValid(boolean signaturesValid) {
        Truth.assertThat(actual().signaturesValid())
                .isEqualTo(signaturesValid);
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
                    dataFileMetadata.getLong("size")));
        }
        return builder.build();
    }

    private static ImmutableList<Signature> signatures(JSONArray metadata) throws JSONException {
        ImmutableList.Builder<Signature> builder = ImmutableList.builder();
        for (int i = 0; i < metadata.length(); i++) {
            JSONObject signatureMetadata = metadata.getJSONObject(i);
            builder.add(Signature.create(
                    signatureMetadata.getString("id"),
                    signatureMetadata.getString("name"),
                    Instant.parse(signatureMetadata.getString("createdAt")),
                    signatureMetadata.getString("status"),
                    signatureMetadata.getString("profile")));
        }
        return builder.build();
    }
}
