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

public class MobileSignFaultMessageSource {

    private static Map<String, Integer> messageMap = createMessageMap();

    private static Map<String, Integer> createMessageMap() {
        Map<String, Integer> messageMap = new HashMap<>();
        messageMap.put("unknown_error", R.string.signature_update_mobile_id_error_unknown);
        messageMap.put("local_service_exception" , R.string.signature_update_mobile_id_error_local_service_exception);
        messageMap.put("general_client", R.string.signature_update_mobile_id_error_general_client);
        messageMap.put("incorrect_input_parameters", R.string.signature_update_mobile_id_error_incorrect_input_parameters);
        messageMap.put("missing_input_parameters", R.string.signature_update_mobile_id_error_missing_input_parameters);
        messageMap.put("ocsp_unauthorized", R.string.signature_update_mobile_id_error_ocsp_unauthorized);
        messageMap.put("general_service", R.string.signature_update_mobile_id_error_general_service);
        messageMap.put("missing_user_certificate", R.string.signature_update_mobile_id_error_missing_user_certificate);
        messageMap.put("certificate_validity_unknown", R.string.signature_update_mobile_id_error_certificate_validity_unknown);
        messageMap.put("session_locked", R.string.signature_update_mobile_id_error_session_locked);
        messageMap.put("general_user", R.string.signature_update_mobile_id_error_general_user);
        messageMap.put("not_mobile_id_user" ,R.string.signature_update_mobile_id_error_not_mobile_id_user);
        messageMap.put("user_certificate_revoked", R.string.signature_update_mobile_id_error_user_certificate_revoked);
        messageMap.put("user_certificate_status_unknown", R.string.signature_update_mobile_id_error_user_certificate_status_unknown);
        messageMap.put("user_certificate_suspended", R.string.signature_update_mobile_id_error_user_certificate_suspended);
        messageMap.put("user_certificate_expired", R.string.signature_update_mobile_id_error_user_certificate_expired);
        messageMap.put("status_user_cancel", R.string.signature_update_mobile_id_status_user_cancel);
        messageMap.put("message_exceeds_volume_limit", R.string.signature_update_mobile_id_error_message_exceeds_volume_limit);
        messageMap.put("simultaneous_requests_limit_exceeded", R.string.signature_update_mobile_id_error_simultaneous_requests_limit_exceeded);
        return messageMap;
    }

    private Resources resources;

    public MobileSignFaultMessageSource(Resources resources) {
        this.resources = resources;
    }

    public String getMessage(String faultCode) {
        return resources.getString(messageMap.get(faultCode));
    }
}
