/*
 * smart-id-lib
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

package ee.ria.DigiDoc.smartid.rest;

import ee.ria.DigiDoc.smartid.dto.request.PostCertificateRequest;
import ee.ria.DigiDoc.smartid.dto.request.PostCreateSignatureRequest;
import ee.ria.DigiDoc.smartid.dto.response.SessionResponse;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SIDRestServiceClient {

    String CONTENT_TYPE_HEADER = "Content-Type: application/json";
    String CONTENT_TYPE_ACCEPT = "Accept: application/json";

    @Headers({ CONTENT_TYPE_HEADER, CONTENT_TYPE_ACCEPT })
    @POST("certificatechoice/pno/{country}/{nationalIdentityNumber}")
    Call<SessionResponse> getCertificate(
            @Path(value = "country", encoded = true) String country,
            @Path(value = "nationalIdentityNumber", encoded = true) String nationalIdentityNumber,
            @Body PostCertificateRequest body);

    @Headers({ CONTENT_TYPE_HEADER, CONTENT_TYPE_ACCEPT })
    @POST("signature/document/{documentnumber}")
    Call<SessionResponse> getCreateSignature(
            @Path(value = "documentnumber", encoded = true) String documentnumber,
            @Body PostCreateSignatureRequest body);

    @Headers({ CONTENT_TYPE_HEADER, CONTENT_TYPE_ACCEPT })
    @GET("session/{session_id}")
    Call<SessionStatusResponse> getSessionStatus(
            @Path(value = "session_id", encoded = true) String sessionId,
            @Query("timeoutMs") long timeoutMs);
}
