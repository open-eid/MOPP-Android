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

package ee.ria.DigiDoc.mobileid.soap;

import ee.ria.DigiDoc.mobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.DigiDoc.mobileid.dto.response.MobileCreateSignatureResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface DigidocServiceClient {

    String CONTENT_TYPE_HEADER = "Content-Type: text/xml;charset=UTF-8";
    String ENCODING_HEADER = "Accept-Encoding: gzip,deflate";

    @Headers({CONTENT_TYPE_HEADER, ENCODING_HEADER})
    @POST("/")
    Call<MobileCreateSignatureResponse> mobileCreateSignature(@Body RequestEnvelope body);

    @Headers({CONTENT_TYPE_HEADER, ENCODING_HEADER})
    @POST("/")
    Call<GetMobileCreateSignatureStatusResponse> getMobileCreateSignatureStatus(@Body RequestEnvelope body);
}
