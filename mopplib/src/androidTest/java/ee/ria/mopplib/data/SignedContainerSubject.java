package ee.ria.mopplib.data;

import android.support.annotation.RawRes;

import com.google.common.collect.ImmutableList;
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

import javax.annotation.Nullable;

import ee.ria.mopplib.Files;

import static com.google.common.truth.Truth.assertAbout;
import static ee.ria.mopplib.Files.readString;

public final class SignedContainerSubject extends Subject<SignedContainerSubject, SignedContainer> {

    public static SignedContainerSubject assertThat(File file, @RawRes int containerRes)
            throws IOException {
        return assertAbout(signedContainers())
                .that(SignedContainer.open(Files.copyRaw(file, containerRes)));
    }

    public static Subject.Factory<SignedContainerSubject, SignedContainer> signedContainers() {
        return SignedContainerSubject::new;
    }

    private SignedContainerSubject(FailureMetadata metadata, @Nullable SignedContainer actual) {
        super(metadata, actual);
    }

    public void matchesMetadata(@RawRes int metadataRes) throws IOException, JSONException {
        JSONObject metadata = containerMetadata(metadataRes);
        hasDataFiles(dataFiles(metadata.getJSONArray("dataFiles")));
        hasSignatures(signatures(metadata.getJSONArray("signatures")));
    }

    public void hasDataFiles(ImmutableList<DataFile> dataFiles) {
        Truth.assertThat(actual().dataFiles())
                .containsExactlyElementsIn(dataFiles)
                .inOrder();
    }

    public void hasSignatures(ImmutableList<Signature> signatures) {
        Truth.assertThat(actual().signatures())
                .containsExactlyElementsIn(signatures)
                .inOrder();
    }

    private static JSONObject containerMetadata(@RawRes int metadataRes) throws IOException,
            JSONException {
        return (JSONObject) new JSONTokener(readString(metadataRes)).nextValue();
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
                    signatureMetadata.getString("status")));
        }
        return builder.build();
    }
}
