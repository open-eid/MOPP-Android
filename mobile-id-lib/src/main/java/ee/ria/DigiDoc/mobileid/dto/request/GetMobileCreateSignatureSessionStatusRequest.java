/*
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

package ee.ria.DigiDoc.mobileid.dto.request;

public class GetMobileCreateSignatureSessionStatusRequest {

    private String sessionId;
    private String timeoutMs;

    public GetMobileCreateSignatureSessionStatusRequest(String sessionId) {
        this.sessionId = sessionId;
        this.timeoutMs = "1000";
    }

    public GetMobileCreateSignatureSessionStatusRequest(String sessionId, String timeoutMs) {
        this.sessionId = sessionId;
        this.timeoutMs = timeoutMs;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(String timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String toString() {
        return "GetMobileCreateSignatureSessionStatusRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", timeoutMs='" + timeoutMs + '\'' +
                '}';
    }
}
