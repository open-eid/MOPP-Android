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

package ee.ria.mopp.androidmobileid.dto;

import com.google.gson.Gson;

public class CreateSignatureStatusDto {

    private String sessCode;
    private ProcessStatus status;
    private String signature;

    public static String toJson(CreateSignatureStatusDto status) {
        return new Gson().toJson(status);
    }

    public static CreateSignatureStatusDto fromJson(String json) {
        return new Gson().fromJson(json, CreateSignatureStatusDto.class);
    }

    public String getSessCode() {
        return sessCode;
    }

    public void setSessCode(String sessCode) {
        this.sessCode = sessCode;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CreateSignatureStatusDto{");
        sb.append("sessCode='").append(sessCode).append('\'');
        sb.append(", status=").append(status);
        sb.append(", signature='").append(signature).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public enum ProcessStatus {
        REQUEST_OK,
        EXPIRED_TRANSACTION,
        USER_CANCEL,
        SIGNATURE,
        OUTSTANDING_TRANSACTION,
        MID_NOT_READY,
        PHONE_ABSENT,
        SENDING_ERROR,
        SIM_ERROR,
        NOT_VALID,
        REVOKED_CERTIFICATE,
        INTERNAL_ERROR
    }

}
