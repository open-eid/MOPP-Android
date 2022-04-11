package ee.ria.DigiDoc.android.model.mobileid;

import static ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType.NOT_ACTIVE;
import static ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType.NOT_FOUND;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse.ProcessStatus;

/**
 * Exception thrown by Mobile-ID service that contains message suitable for showing to the user.
 */
public final class MobileIdMessageException extends Exception {

    @Nullable private final String detailMessage;

    private static final ImmutableMap<MobileCertificateResultType, Integer> FAULT_REASON_MESSAGES =
            ImmutableMap.<MobileCertificateResultType, Integer>builder()
                    .put(NOT_FOUND, R.string.signature_update_mobile_id_error_not_mobile_id_user)
                    .put(NOT_ACTIVE, R.string.signature_update_mobile_id_error_not_mobile_id_user)
                    .build();

    public static MobileIdMessageException create(Context context, ProcessStatus status, @Nullable String errorMessage) {
        return new MobileIdMessageException(MobileIdStatusMessages.message(context, status), errorMessage);
    }

    public static MobileIdMessageException create(Context context, MobileCertificateResultType resultType, @Nullable String errorMessage) {
        return new MobileIdMessageException(
                context.getString(FAULT_REASON_MESSAGES.get(resultType)), errorMessage);
    }

    private MobileIdMessageException(String message) {
        super(message);
        detailMessage = null;
    }

    private MobileIdMessageException(String message, @Nullable String detailMessage) {
        super(message);
        this.detailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
