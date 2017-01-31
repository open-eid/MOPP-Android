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

package ee.ria.mopp.androidmobileid.soap;


import android.util.Log;

import java.io.IOException;

import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.dto.response.MobileCreateSignatureResponse;
import retrofit2.Call;
import retrofit2.Response;

public class DigiDocService {

    private static final String TAG = "DigiDocService";

    private static final String NAMESPACE = "http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl";
    private static final String MAIN_REQUEST_URL = "https://digidocservice.sk.ee/?wsdl";
    private static final String SOAP_ACTION = "http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl/MobileCreateSignature";
    private static final String MOBILE_CREATE_SIGNATURE_METHOD_NAME = "MobileCreateSignature";

    public MobileCreateSignatureResponse mobileCreateSignature(MobileCreateSignatureRequest request) {
        RequestEnvelope envelope = new RequestEnvelope(new RequestBody(request));



        DigidocServiceClient ddsClient = ServiceGenerator.createService(DigidocServiceClient.class);


        Call<MobileCreateSignatureResponse> call = ddsClient.mobileCreateSignature(envelope);
        try {
            Response<MobileCreateSignatureResponse> response = call.execute();
            if (response.isSuccessful()) {
                Log.i(TAG, "Call successful");
                return response.body();
            }
            else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
