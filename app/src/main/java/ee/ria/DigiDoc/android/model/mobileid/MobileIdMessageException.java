package ee.ria.DigiDoc.android.model.mobileid;

import android.content.Context;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse.ProcessStatus;

/**
 * Exception thrown by Mobile-ID service that contains message suitable for showing to the user.
 */
public final class MobileIdMessageException extends Exception {

    private static final ImmutableMap<String, Integer> FAULT_REASON_MESSAGES =
            ImmutableMap.<String, Integer>builder()
                    .put("unknown_error", R.string.signature_update_mobile_id_error_unknown)
                    .put("local_service_exception",
                            R.string.signature_update_mobile_id_error_local_service_exception)
                    .put("general_client", R.string.signature_update_mobile_id_error_general_client)
                    .put("incorrect_input_parameters",
                            R.string.signature_update_mobile_id_error_incorrect_input_parameters)
                    .put("missing_input_parameters",
                            R.string.signature_update_mobile_id_error_missing_input_parameters)
                    .put("ocsp_unauthorized",
                            R.string.signature_update_mobile_id_error_ocsp_unauthorized)
                    .put("general_service",
                            R.string.signature_update_mobile_id_error_general_service)
                    .put("missing_user_certificate",
                            R.string.signature_update_mobile_id_error_missing_user_certificate)
                    .put("certificate_validity_unknown",
                            R.string.signature_update_mobile_id_error_certificate_validity_unknown)
                    .put("session_locked", R.string.signature_update_mobile_id_error_session_locked)
                    .put("general_user", R.string.signature_update_mobile_id_error_general_user)
                    .put("not_mobile_id_user",
                            R.string.signature_update_mobile_id_error_not_mobile_id_user)
                    .put("user_certificate_revoked",
                            R.string.signature_update_mobile_id_error_user_certificate_revoked)
                    .put("user_certificate_status_unknown", R.string
                            .signature_update_mobile_id_error_user_certificate_status_unknown)
                    .put("user_certificate_suspended",
                            R.string.signature_update_mobile_id_error_user_certificate_suspended)
                    .put("user_certificate_expired",
                            R.string.signature_update_mobile_id_error_user_certificate_expired)
                    .put("status_user_cancel",
                            R.string.signature_update_mobile_id_status_user_cancel)
                    .put("message_exceeds_volume_limit",
                            R.string.signature_update_mobile_id_error_message_exceeds_volume_limit)
                    .put("simultaneous_requests_limit_exceeded", R.string
                            .signature_update_mobile_id_error_simultaneous_requests_limit_exceeded)
                    .build();

    public static MobileIdMessageException create(Context context, ProcessStatus status) {
        return new MobileIdMessageException(MobileIdStatusMessages.message(context, status));
    }

    public static MobileIdMessageException create(Context context, String faultReason) {
        return new MobileIdMessageException(
                context.getString(FAULT_REASON_MESSAGES.get(faultReason)));
    }

    private MobileIdMessageException(String message) {
        super(message);
    }
}
