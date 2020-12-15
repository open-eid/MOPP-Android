/*
 * Copyright 2020 Riigi Infosüsteemide Amet
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

package ee.ria.DigiDoc.mobileid.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MobileCreateSignatureSessionStatusResponse {

    @SerializedName("state")
    private ProcessState state;
    @SerializedName("result")
    private ProcessStatus result;
    @SerializedName("signature")
    private MobileSignatureResponse signature;
    @SerializedName("cert")
    private String cert;
    @SerializedName("time")
    private String time;
    @SerializedName("traceId")
    private String traceId;
    @SerializedName("error")
    private String error;

    public static String toJson(MobileCreateSignatureSessionStatusResponse status) {
        return new Gson().toJson(status);
    }

    public static MobileCreateSignatureSessionStatusResponse fromJson(String json) {
        return new Gson().fromJson(json, MobileCreateSignatureSessionStatusResponse.class);
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public ProcessStatus getResult() {
        return result;
    }

    public void setResult(ProcessStatus result) {
        this.result = result;
    }

    public MobileSignatureResponse getSignature() {
        return signature;
    }

    public void setSignature(MobileSignatureResponse signature) {
        this.signature = signature;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "MobileCreateSignatureSessionStatusResponse{" +
                "state=" + state +
                ", result=" + result +
                ", signature=" + signature +
                ", cert='" + cert + '\'' +
                ", time='" + time + '\'' +
                ", traceId='" + traceId + '\'' +
                ", error='" + error + '\'' +
                '}';
    }

    public enum ProcessState {
        RUNNING,
        COMPLETE
    }

    public enum ProcessStatus {
        OK,
        TIMEOUT,
        NOT_MID_CLIENT,
        USER_CANCELLED,
        SIGNATURE_HASH_MISMATCH,
        PHONE_ABSENT,
        DELIVERY_ERROR,
        SIM_ERROR,

        TOO_MANY_REQUESTS,
        OCSP_INVALID_TIME_SLOT,
        GENERAL_ERROR,
        NO_RESPONSE,
        INVALID_COUNTRY_CODE,
        INVALID_SSL_HANDSHAKE
    }
}

