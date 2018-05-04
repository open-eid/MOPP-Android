package ee.ria.DigiDoc.android.model.mobileid;

import android.content.Context;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse.ProcessStatus;

public final class MobileIdStatusMessages {

    private static final ImmutableMap<ProcessStatus, Integer> MESSAGES =
            ImmutableMap.<ProcessStatus, Integer>builder()
                    .put(ProcessStatus.DEFAULT,
                            R.string.signature_update_mobile_id_status_request_sent)
                    .put(ProcessStatus.REQUEST_OK,
                            R.string.signature_update_mobile_id_status_request_ok)
                    .put(ProcessStatus.EXPIRED_TRANSACTION,
                            R.string.signature_update_mobile_id_status_expired_transaction)
                    .put(ProcessStatus.USER_CANCEL,
                            R.string.signature_update_mobile_id_status_user_cancel)
                    .put(ProcessStatus.SIGNATURE,
                            R.string.signature_update_mobile_id_status_signature)
                    .put(ProcessStatus.OUTSTANDING_TRANSACTION,
                            R.string.signature_update_mobile_id_status_outstanding_transaction)
                    .put(ProcessStatus.MID_NOT_READY,
                            R.string.signature_update_mobile_id_status_mid_not_ready)
                    .put(ProcessStatus.PHONE_ABSENT,
                            R.string.signature_update_mobile_id_status_phone_absent)
                    .put(ProcessStatus.SENDING_ERROR,
                            R.string.signature_update_mobile_id_status_sending_error)
                    .put(ProcessStatus.SIM_ERROR,
                            R.string.signature_update_mobile_id_status_sim_error)
                    .put(ProcessStatus.NOT_VALID,
                            R.string.signature_update_mobile_id_status_not_valid)
                    .put(ProcessStatus.REVOKED_CERTIFICATE,
                            R.string.signature_update_mobile_id_status_revoked_certificate)
                    .put(ProcessStatus.INTERNAL_ERROR,
                            R.string.signature_update_mobile_id_status_internal_error)
                    .build();

    public static String message(Context context, ProcessStatus status) {
        return context.getString(MESSAGES.get(status));
    }
}
