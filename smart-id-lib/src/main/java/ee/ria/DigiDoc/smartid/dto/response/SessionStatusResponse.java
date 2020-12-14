/*
 * smart-id-lib
 * Copyright 2020 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.smartid.dto.response;

import lombok.Data;

@Data
public class SessionStatusResponse {

    private ProcessState state;
    private Status result;
    private SmartSignatureResponse signature;
    private SmartCertificateResponse cert;

    public enum ProcessState {
        RUNNING,
        COMPLETE
    }

    @Data
    public static class Status {

        private ProcessStatus endResult;
        private String documentNumber;
    }

    @Data
    public static class SmartSignatureResponse {

        private String value;
        private String algorithm;
    }

    @Data
    public static class SmartCertificateResponse {

        private String value;
        private String assuranceLevel;
        private String certificateLevel;
    }

    public enum ProcessStatus {
        OK,
        TIMEOUT,
        USER_REFUSED,
        DOCUMENT_UNUSABLE,
        WRONG_VC,

        ACCOUNT_NOT_FOUND,
        SESSION_NOT_FOUND,
        MISSING_SESSIONID,
        TOO_MANY_REQUESTS,
        NOT_QUALIFIED,
        INVALID_ACCESS_RIGHTS,
        OLD_API,
        UNDER_MAINTENANCE,
        GENERAL_ERROR,
        NO_RESPONSE,
        INVALID_SSL_HANDSHAKE
    }
}

