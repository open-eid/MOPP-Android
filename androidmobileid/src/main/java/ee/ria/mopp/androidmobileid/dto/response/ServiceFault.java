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

package ee.ria.mopp.androidmobileid.dto.response;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import ee.ria.mopp.androidmobileid.soap.SoapFault;

public class ServiceFault {
    private String originalFaultCode;
    private String originalFaultMessage;
    private String reason;

    public static String toJson(ServiceFault fault) {
        return new Gson().toJson(fault);
    }

    public static ServiceFault fromJson(String json) {
        return new Gson().fromJson(json, ServiceFault.class);
    }

    public ServiceFault(SoapFault soapFault) {
        originalFaultCode = soapFault.getFaultstring();
        originalFaultMessage = soapFault.getMessage();
        reason = getReason(soapFault);
    }

    public ServiceFault(Exception e) {
        originalFaultCode = "";
        originalFaultMessage = e.getMessage();
        reason = reasonMap.get("exception");
    }

    public String getOriginalFaultCode() {
        return originalFaultCode;
    }

    public void setOriginalFaultCode(String originalFaultCode) {
        this.originalFaultCode = originalFaultCode;
    }

    public String getOriginalFaultMessage() {
        return originalFaultMessage;
    }

    public void setOriginalFaultMessage(String originalFaultMessage) {
        this.originalFaultMessage = originalFaultMessage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    private String getReason(SoapFault soapFault) {
        String reason = reasonMap.get(soapFault.getFaultstring());
        if (reason == null) {
            reason = reasonMap.get("unknown");
        }
        return reason;
    }

    private static final Map<String, String> reasonMap = createReasonMap();

    private static Map<String, String> createReasonMap() {
        Map<String, String> reasonMap = new HashMap<>();
        reasonMap.put("unknown", "unknown_error");
        reasonMap.put("exception", "local_service_exception");
        reasonMap.put("100", "general_client");
        reasonMap.put("101", "incorrect_input_parameters");
        reasonMap.put("102", "missing_input_parameters");
        reasonMap.put("103", "ocsp_unauthorized");
        reasonMap.put("200", "general_service");
        reasonMap.put("201", "missing_user_certificate");
        reasonMap.put("202", "certificate_validity_unknown");
        reasonMap.put("203", "session_locked");
        reasonMap.put("300", "general_user");
        reasonMap.put("301", "not_mobile_id_user");
        reasonMap.put("302", "user_certificate_revoked");
        reasonMap.put("303", "user_certificate_status_unknown");
        reasonMap.put("304", "user_certificate_suspended");
        reasonMap.put("305", "user_certificate_expired");
        reasonMap.put("413", "message_exceeds_volume_limit");
        reasonMap.put("503", "simultaneous_requests_limit_exceeded");
        return reasonMap;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceFault{");
        sb.append("originalFaultCode='").append(originalFaultCode).append('\'');
        sb.append(", originalFaultMessage='").append(originalFaultMessage).append('\'');
        sb.append(", reason='").append(reason).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
