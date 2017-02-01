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

package ee.ria.EstEIDUtility.mid;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

import ee.ria.EstEIDUtility.R;

public class MobileSignFaultMessageSource {

    private static Map<String, Integer> messageMap = createMessageMap();

    private static Map<String, Integer> createMessageMap() {
        Map<String, Integer> messageMap = new HashMap<>();
        messageMap.put("unknown_error", R.string.unknown_error);
        messageMap.put("local_service_exception" , R.string.local_service_exception);
        messageMap.put("general_client", R.string.general_client);
        messageMap.put("incorrect_input_parameters", R.string.incorrect_input_parameters);
        messageMap.put("missing_input_parameters", R.string.missing_input_parameters);
        messageMap.put("ocsp_unauthorized", R.string.ocsp_unauthorized);
        messageMap.put("general_service", R.string.general_service);
        messageMap.put("missing_user_certificate", R.string.missing_user_certificate);
        messageMap.put("certificate_validity_unknown", R.string.certificate_validity_unknown);
        messageMap.put("session_locked", R.string.session_locked);
        messageMap.put("general_user", R.string.general_user);
        messageMap.put("not_mobile_id_user" ,R.string.not_mobile_id_user);
        messageMap.put("user_certificate_revoked", R.string.user_certificate_revoked);
        messageMap.put("user_certificate_status_unknown", R.string.user_certificate_status_unknown);
        messageMap.put("user_certificate_suspended", R.string.user_certificate_suspended);
        messageMap.put("user_certificate_expired", R.string.user_certificate_expired);
        messageMap.put("message_exceeds_volume_limit", R.string.message_exceeds_volume_limit);
        messageMap.put("simultaneous_requests_limit_exceeded", R.string.simultaneous_requests_limit_exceeded);
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
