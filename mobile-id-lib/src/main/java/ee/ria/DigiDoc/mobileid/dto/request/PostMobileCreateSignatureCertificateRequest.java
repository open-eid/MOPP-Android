/*
 * Copyright 2021 Riigi Infosüsteemi Amet
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

import com.google.gson.Gson;

public class PostMobileCreateSignatureCertificateRequest {

    private String relyingPartyName;
    private String relyingPartyUUID;
    private String phoneNumber;
    private String nationalIdentityNumber;

    public static String toJson(PostMobileCreateSignatureCertificateRequest request) {
        return new Gson().toJson(request);
    }

    public static PostMobileCreateSignatureCertificateRequest fromJson(String json) {
        return new Gson().fromJson(json, PostMobileCreateSignatureCertificateRequest.class);
    }

    public String getRelyingPartyName() {
        return relyingPartyName;
    }

    public void setRelyingPartyName(String relyingPartyName) {
        this.relyingPartyName = relyingPartyName;
    }

    public String getRelyingPartyUUID() {
        return relyingPartyUUID;
    }

    public void setRelyingPartyUUID(String relyingPartyUUID) {
        this.relyingPartyUUID = relyingPartyUUID;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNationalIdentityNumber() {
        return nationalIdentityNumber;
    }

    public void setNationalIdentityNumber(String nationalIdentityNumber) {
        this.nationalIdentityNumber = nationalIdentityNumber;
    }

    @Override
    public String toString() {
        return "GetMobileCertificateRequest{" +
                "relyingPartyName='" + relyingPartyName + '\'' +
                ", relyingPartyUUID='" + relyingPartyUUID + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", nationalIdentityNumber='" + nationalIdentityNumber + '\'' +
                '}';
    }
}
