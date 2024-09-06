package ee.ria.DigiDoc.android.signature.update.nfc;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;

@AutoValue
public abstract class NFCResponse implements SignatureAddResponse {

    @Nullable public abstract SessionStatusResponse.ProcessStatus status();
    @Nullable public abstract String message();

    @Override
    public boolean active() {
        return false;
    }

    @Override
    public boolean showDialog() {
        return false;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        return this;
    }

    public static NFCResponse initial() {
        return create(null, null, null);
    }

    public static NFCResponse success(SignedContainer container) {
        return create(container, null, null);
    }

    private static NFCResponse create(@Nullable SignedContainer container,
                                      @Nullable SessionStatusResponse.ProcessStatus status,
                                         @Nullable String message) {
        return new AutoValue_NFCResponse(container, status, message);
    }

    public static NFCResponse createWithStatus(SessionStatusResponse.ProcessStatus status, String message) {
        return create(null, status, message);
    }
}
