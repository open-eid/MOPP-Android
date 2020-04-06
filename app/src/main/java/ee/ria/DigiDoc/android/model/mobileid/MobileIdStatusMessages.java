package ee.ria.DigiDoc.android.model.mobileid;

import android.content.Context;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse.ProcessStatus;

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
                    .put(ProcessStatus.TOO_MANY_REQUESTS, R.string.signature_update_mobile_id_error_message_too_many_requests)
                    .put(ProcessStatus.GENERAL_ERROR,
                            R.string.signature_update_mobile_id_error_general_client)
                    .put(ProcessStatus.NO_RESPONSE, R.string.signature_update_mobile_id_error_no_response)
                    .put(ProcessStatus.INVALID_COUNTRY_CODE, R.string.signature_update_mobile_id_status_no_country_code)
                    .build();

    public static String message(Context context, ProcessStatus status) {
        return context.getString(MESSAGES.get(status));
    }
}
