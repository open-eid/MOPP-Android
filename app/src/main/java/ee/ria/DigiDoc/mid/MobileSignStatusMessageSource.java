/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.mid;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

import ee.ria.DigiDoc.R;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse.ProcessStatus;

public class MobileSignStatusMessageSource {

    private static Map<ProcessStatus, Integer> messageMap = createMessageMap();

    private static Map<ProcessStatus, Integer> createMessageMap() {
        Map<ProcessStatus, Integer> messageMap = new HashMap<>();
        messageMap.put(ProcessStatus.DEFAULT, R.string.signature_update_mobile_id_status_request_sent);
        messageMap.put(ProcessStatus.REQUEST_OK, R.string.signature_update_mobile_id_status_request_ok);
        messageMap.put(ProcessStatus.EXPIRED_TRANSACTION, R.string.signature_update_mobile_id_status_expired_transaction);
        messageMap.put(ProcessStatus.USER_CANCEL, R.string.signature_update_mobile_id_status_user_cancel);
        messageMap.put(ProcessStatus.SIGNATURE, R.string.signature_update_mobile_id_status_signature);
        messageMap.put(ProcessStatus.OUTSTANDING_TRANSACTION, R.string.signature_update_mobile_id_status_outstanding_transaction);
        messageMap.put(ProcessStatus.MID_NOT_READY, R.string.signature_update_mobile_id_status_mid_not_ready);
        messageMap.put(ProcessStatus.PHONE_ABSENT, R.string.signature_update_mobile_id_status_phone_absent);
        messageMap.put(ProcessStatus.SENDING_ERROR, R.string.signature_update_mobile_id_status_sending_error);
        messageMap.put(ProcessStatus.SIM_ERROR, R.string.signature_update_mobile_id_status_sim_error);
        messageMap.put(ProcessStatus.NOT_VALID, R.string.signature_update_mobile_id_status_not_valid);
        messageMap.put(ProcessStatus.REVOKED_CERTIFICATE, R.string.signature_update_mobile_id_status_revoked_certificate);
        messageMap.put(ProcessStatus.INTERNAL_ERROR, R.string.signature_update_mobile_id_status_internal_error);
        return messageMap;
    }

    private Resources resources;

    public MobileSignStatusMessageSource(Resources resources) {
        this.resources = resources;
    }

    public String getMessage(ProcessStatus processStatus) {
        return resources.getString(messageMap.get(processStatus));
    }

    public String getInitialStatusMessage() {
        return getMessage(ProcessStatus.DEFAULT);
    }
}
