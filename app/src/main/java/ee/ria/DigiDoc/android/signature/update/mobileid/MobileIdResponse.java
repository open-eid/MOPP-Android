package ee.ria.DigiDoc.android.signature.update.mobileid;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse;
import ee.ria.DigiDoc.sign.SignedContainer;

@AutoValue
public abstract class MobileIdResponse implements SignatureAddResponse {

    @Nullable public abstract MobileCreateSignatureSessionStatusResponse.ProcessStatus status();

    @Nullable public abstract String challenge();

    @Nullable public abstract String signature();

    @Override
    public boolean showDialog() {
        return false;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        if (previous == null || !(previous instanceof MobileIdResponse)) {
            return this;
        }
        MobileIdResponse previousResponse = (MobileIdResponse) previous;
        MobileCreateSignatureSessionStatusResponse.ProcessStatus status =
                status() == null ? previousResponse.status() : status();
        String challenge = challenge() == null ? previousResponse.challenge() : challenge();
        String signature = signature() == null ? previousResponse.signature() : signature();
        return create(container(), active(), status, challenge, signature);
    }

    public static MobileIdResponse status(
            MobileCreateSignatureSessionStatusResponse.ProcessStatus status) {
        return create(null, true, status, null, null);
    }

    static MobileIdResponse challenge(String challenge) {
        return create(null, true, null, challenge, null);
    }

    static MobileIdResponse signature(String signature) {
        return create(null, true, MobileCreateSignatureSessionStatusResponse.ProcessStatus.OK,
                null, signature);
    }

    public static MobileIdResponse success(SignedContainer container) {
        return create(container, false, null, null, null);
    }

    private static MobileIdResponse create(
            @Nullable SignedContainer container, boolean active,
            @Nullable MobileCreateSignatureSessionStatusResponse.ProcessStatus status,
            @Nullable String challenge, @Nullable String signature) {
        return new AutoValue_MobileIdResponse(container, active, status, challenge, signature);
    }
}
