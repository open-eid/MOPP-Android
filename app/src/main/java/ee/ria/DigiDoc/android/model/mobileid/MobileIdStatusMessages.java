package ee.ria.DigiDoc.android.model.mobileid;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.common.TextUtil;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse.ProcessStatus;
import timber.log.Timber;

public final class MobileIdStatusMessages {

    private static final ImmutableMap<ProcessStatus, Integer> MESSAGES =
            ImmutableMap.<ProcessStatus, Integer>builder()
                    .put(ProcessStatus.OK,
                            R.string.signature_update_mobile_id_status_request_ok)
                    .put(ProcessStatus.TIMEOUT,
                            R.string.signature_update_mobile_id_status_expired_transaction)
                    .put(ProcessStatus.NOT_MID_CLIENT,
                            R.string.signature_update_mobile_id_status_expired_transaction)
                    .put(ProcessStatus.USER_CANCELLED,
                            R.string.signature_update_mobile_id_status_user_cancel)
                    .put(ProcessStatus.SIGNATURE_HASH_MISMATCH,
                            R.string.signature_update_mobile_id_status_signature_hash_mismatch)
                    .put(ProcessStatus.DELIVERY_ERROR,
                            R.string.signature_update_mobile_id_status_delivery_error)
                    .put(ProcessStatus.PHONE_ABSENT,
                            R.string.signature_update_mobile_id_status_phone_absent)
                    .put(ProcessStatus.SIM_ERROR,
                            R.string.signature_update_mobile_id_status_sim_error)
                    .put(ProcessStatus.TOO_MANY_REQUESTS, R.string.signature_update_signature_error_message_too_many_requests)
                    .put(ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS, R.string.signature_update_signature_error_message_exceeded_unsuccessful_requests)
                    .put(ProcessStatus.INVALID_ACCESS_RIGHTS, R.string.signature_update_mobile_id_error_message_access_rights)
                    .put(ProcessStatus.OCSP_INVALID_TIME_SLOT, R.string.signature_update_signature_error_message_invalid_time_slot)
                    .put(ProcessStatus.CERTIFICATE_REVOKED, R.string.signature_update_signature_error_message_certificate_revoked)
                    .put(ProcessStatus.GENERAL_ERROR,
                            R.string.signature_update_mobile_id_error_general_client)
                    .put(ProcessStatus.NO_RESPONSE, R.string.no_internet_connection)
                    .put(ProcessStatus.INVALID_COUNTRY_CODE, R.string.signature_update_mobile_id_status_no_country_code)
                    .put(ProcessStatus.INVALID_SSL_HANDSHAKE, R.string.invalid_ssl_handshake)
                    .put(ProcessStatus.TECHNICAL_ERROR, R.string.signature_update_mobile_id_error_technical_error)
                    .put(ProcessStatus.INVALID_PROXY_SETTINGS, R.string.main_settings_proxy_invalid_settings)
                    .build();

    public static String message(Context context, ProcessStatus status) {
        if (status.equals(ProcessStatus.TOO_MANY_REQUESTS)) {
            Timber.log(Log.DEBUG, String.format("%s - %s", "Mobile-ID", context.getString(MESSAGES.get(status), context.getString(
                    R.string.signature_update_signature_add_method_mobile_id))));
            return context.getString(MESSAGES.get(status),
                    TextUtil.uncapitalizeString(context.getString(
                            R.string.signature_update_signature_add_method_mobile_id)));
        }
        Timber.log(Log.DEBUG, String.format("%s - %s", "Mobile-ID", context.getString(MESSAGES.get(status))));
        return context.getString(MESSAGES.get(status));
    }
}
