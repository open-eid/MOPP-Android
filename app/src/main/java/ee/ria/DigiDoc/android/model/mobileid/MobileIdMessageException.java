package ee.ria.DigiDoc.android.model.mobileid;

import android.content.Context;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse.ProcessStatus;

import static ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType.NOT_ACTIVE;
import static ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType.NOT_FOUND;

/**
 * Exception thrown by Mobile-ID service that contains message suitable for showing to the user.
 */
public final class MobileIdMessageException extends Exception {

    private static final ImmutableMap<MobileCertificateResultType, Integer> FAULT_REASON_MESSAGES =
            ImmutableMap.<MobileCertificateResultType, Integer>builder()
                    .put(NOT_FOUND, R.string.signature_update_mobile_id_error_not_mobile_id_user)
                    .put(NOT_ACTIVE, R.string.signature_update_mobile_id_error_not_mobile_id_user)
                    .build();

    public static MobileIdMessageException create(Context context, ProcessStatus status) {
        return new MobileIdMessageException(MobileIdStatusMessages.message(context, status));
    }

    public static MobileIdMessageException create(Context context, MobileCertificateResultType resultType) {
        return new MobileIdMessageException(
                context.getString(FAULT_REASON_MESSAGES.get(resultType)));
    }

    private MobileIdMessageException(String message) {
        super(message);
    }
}
