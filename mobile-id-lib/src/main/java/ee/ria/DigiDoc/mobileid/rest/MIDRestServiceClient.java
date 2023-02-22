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

package ee.ria.DigiDoc.mobileid.rest;

import ee.ria.DigiDoc.mobileid.dto.request.PostMobileCreateSignatureCertificateRequest;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureCertificateResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureSessionStatusResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MIDRestServiceClient {

    String CONTENT_TYPE_HEADER = "Content-Type: application/json; charset=utf-8";
    String CONTENT_TYPE_ACCEPT = "Accept: application/json";

    @Headers({ CONTENT_TYPE_HEADER, CONTENT_TYPE_ACCEPT })
    @POST("certificate")
    Call<MobileCreateSignatureCertificateResponse> getCertificate(@Body PostMobileCreateSignatureCertificateRequest body);

    @Headers({ CONTENT_TYPE_HEADER, CONTENT_TYPE_ACCEPT })
    @POST("signature")
    Call<MobileCreateSignatureSessionResponse> getMobileCreateSession(@Body String body);

    @Headers({ CONTENT_TYPE_HEADER, CONTENT_TYPE_ACCEPT })
    @GET("signature/session/{session_id}")
    Call<MobileCreateSignatureSessionStatusResponse> getMobileCreateSignatureSessionStatus(@Path(value = "session_id", encoded = true) String sessionId, @Query("timeoutMs") String timeoutMs);
}
