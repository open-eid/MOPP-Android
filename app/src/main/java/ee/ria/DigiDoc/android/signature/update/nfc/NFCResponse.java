package ee.ria.DigiDoc.android.signature.update.nfc;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardSignResponse;
import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdResponse;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;

@AutoValue
public abstract class NFCResponse implements SignatureAddResponse {

    @Nullable public abstract SessionStatusResponse.ProcessStatus status();

    @Nullable public abstract IdCardDataResponse dataResponse();

    @Nullable public abstract IdCardSignResponse signResponse();

    @Override
    public boolean active() {
        return false;
    }

    @Override
    public boolean showDialog() {
        return true;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        return this;
    }

    public static NFCResponse initial() {
        return data(IdCardDataResponse.initial());
    }

    public static NFCResponse data(IdCardDataResponse dataResponse) {
        return create(null, null, dataResponse, null);
    }

    public static NFCResponse sign(IdCardSignResponse signResponse) {
        return create(null, null, null, signResponse);
    }

    public static NFCResponse success(SignedContainer container) {
        return create(container, null, null, null);
    }

    private static NFCResponse create(@Nullable SignedContainer container,
                                      @Nullable SessionStatusResponse.ProcessStatus status,
                                         @Nullable IdCardDataResponse dataResponse,
                                         @Nullable IdCardSignResponse signResponse) {
        return new AutoValue_NFCResponse(container, status, dataResponse, signResponse);
    }

    /* fixme: copy-pasted (Lauris) */
    public static NFCResponse status(
            SessionStatusResponse.ProcessStatus status) {
        return create(null, null, null, null);
    }
}
