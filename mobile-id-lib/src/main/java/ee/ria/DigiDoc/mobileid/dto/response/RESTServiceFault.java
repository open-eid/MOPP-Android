/*
 * Copyright 2017 - 2023 Riigi Infosüsteemi Amet
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

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;

import ee.ria.DigiDoc.common.DetailMessageSource;
import ee.ria.DigiDoc.mobileid.dto.MobileCertificateResultType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RESTServiceFault implements DetailMessageSource {

    private int httpStatus;
    private MobileCreateSignatureSessionStatusResponse.ProcessState state;
    private MobileCreateSignatureSessionStatusResponse.ProcessStatus status;
    private MobileCertificateResultType result;
    private String time;
    private String traceId;
    private String error;

    @Nullable private String detailMessage;

    public RESTServiceFault() {
    }

    public RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus status) {
        this.status = status;
    }

    public RESTServiceFault(MobileCreateSignatureSessionStatusResponse.ProcessStatus status, @Nullable String detailMessage) {
        this.status = status;
        this.detailMessage = detailMessage;
    }

    public RESTServiceFault(int httpStatus, MobileCreateSignatureSessionStatusResponse.ProcessState state, MobileCreateSignatureSessionStatusResponse.ProcessStatus status, String time, String traceId, String error) {
        this.httpStatus = httpStatus;
        this.state = state;
        this.status = status;
        this.time = time;
        this.traceId = traceId;
        this.error = error;
    }

    public RESTServiceFault(int httpStatus, MobileCertificateResultType result, String time, String traceId, String error) {
        this.httpStatus = httpStatus;
        this.result = result;
        this.time = time;
        this.traceId = traceId;
        this.error = error;
    }

    public static String toJson(RESTServiceFault fault) {
        return new Gson().toJson(fault);
    }

    public static RESTServiceFault fromJson(String json) {
        return new Gson().fromJson(json, RESTServiceFault.class);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public MobileCreateSignatureSessionStatusResponse.ProcessState getState() {
        return state;
    }

    public void setState(MobileCreateSignatureSessionStatusResponse.ProcessState state) {
        this.state = state;
    }

    public MobileCreateSignatureSessionStatusResponse.ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(MobileCreateSignatureSessionStatusResponse.ProcessStatus status) {
        this.status = status;
    }

    public MobileCertificateResultType getResult() {
        return result;
    }

    public void setResult(MobileCertificateResultType result) {
        this.result = result;
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
    @Nullable public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public String toString() {
        return "RESTServiceFault{" +
                "httpStatus=" + httpStatus +
                ", state=" + state +
                ", status=" + status +
                ", result=" + result +
                ", time='" + time + '\'' +
                ", traceId='" + traceId + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
