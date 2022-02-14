/*
 * app
 * Copyright 2017 - 2022 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.android.model.smartid;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse.ProcessStatus;
import timber.log.Timber;

public final class SmartIdStatusMessages {

    private static final ImmutableMap<ProcessStatus, Integer> MESSAGES =
            ImmutableMap.<ProcessStatus, Integer>builder()
                    .put(ProcessStatus.OK,
                            R.string.signature_update_mobile_id_status_request_ok)
                    .put(ProcessStatus.USER_REFUSED,
                            R.string.signature_update_mobile_id_status_user_cancel)
                    .put(ProcessStatus.DOCUMENT_UNUSABLE,
                            R.string.signature_update_smart_id_status_document_unusable)
                    .put(ProcessStatus.WRONG_VC,
                            R.string.signature_update_smart_id_status_wrong_vc)
                    .put(ProcessStatus.ACCOUNT_NOT_FOUND_OR_TIMEOUT,
                            R.string.signature_update_smart_id_error_message_account_not_found_or_timeout)
                    .put(ProcessStatus.SESSION_NOT_FOUND,
                            R.string.signature_update_smart_id_error_message_session_not_found)
                    .put(ProcessStatus.MISSING_SESSIONID,
                            R.string.signature_update_mobile_id_error_general_client)
                    .put(ProcessStatus.TOO_MANY_REQUESTS,
                            R.string.signature_update_signature_error_message_too_many_requests)
                    .put(ProcessStatus.EXCEEDED_UNSUCCESSFUL_REQUESTS, R.string.signature_update_signature_error_message_exceeded_unsuccessful_requests)
                    .put(ProcessStatus.OCSP_INVALID_TIME_SLOT, R.string.signature_update_signature_error_message_invalid_time_slot)
                    .put(ProcessStatus.CERTIFICATE_REVOKED, R.string.signature_update_signature_error_message_certificate_revoked)
                    .put(ProcessStatus.NOT_QUALIFIED,
                            R.string.signature_update_smart_id_error_message_not_qualified)
                    .put(ProcessStatus.INVALID_ACCESS_RIGHTS,
                            R.string.signature_update_smart_id_error_message_access_rights)
                    .put(ProcessStatus.OLD_API,
                            R.string.signature_update_smart_id_error_message_old_api)
                    .put(ProcessStatus.UNDER_MAINTENANCE,
                            R.string.signature_update_smart_id_error_message_under_maintenance)
                    .put(ProcessStatus.GENERAL_ERROR,
                            R.string.signature_update_mobile_id_error_general_client)
                    .put(ProcessStatus.NO_RESPONSE,
                            R.string.no_internet_connection)
                    .put(ProcessStatus.INVALID_SSL_HANDSHAKE,
                            R.string.signature_update_signature_error_invalid_ssl_handshake)
                    .build();

    public static String message(Context context, ProcessStatus status) {
        Timber.log(Log.DEBUG, context.getString(MESSAGES.get(status)));
        return context.getString(MESSAGES.get(status));
    }
}
