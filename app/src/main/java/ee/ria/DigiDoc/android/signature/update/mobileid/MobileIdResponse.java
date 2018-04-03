package ee.ria.DigiDoc.android.signature.update.mobileid;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopplib.data.SignedContainer;

@AutoValue
public abstract class MobileIdResponse implements SignatureAddResponse {

    @Nullable public abstract GetMobileCreateSignatureStatusResponse.ProcessStatus status();

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
        GetMobileCreateSignatureStatusResponse.ProcessStatus status =
                status() == null ? previousResponse.status() : status();
        String challenge = challenge() == null ? previousResponse.challenge() : challenge();
        String signature = signature() == null ? previousResponse.signature() : signature();
        return create(container(), active(), status, challenge, signature);
    }

    public static MobileIdResponse status(
            GetMobileCreateSignatureStatusResponse.ProcessStatus status) {
        return create(null, true, status, null, null);
    }

    static MobileIdResponse challenge(String challenge) {
        return create(null, true, null, challenge, null);
    }

    static MobileIdResponse signature(String signature) {
        return create(null, true, GetMobileCreateSignatureStatusResponse.ProcessStatus.SIGNATURE,
                null, signature);
    }

    public static MobileIdResponse success(SignedContainer container) {
        return create(container, false, null, null, null);
    }

    private static MobileIdResponse create(
            @Nullable SignedContainer container, boolean active,
            @Nullable GetMobileCreateSignatureStatusResponse.ProcessStatus status,
            @Nullable String challenge, @Nullable String signature) {
        return new AutoValue_MobileIdResponse(container, active, status, challenge, signature);
    }
}
