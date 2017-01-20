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

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.ksoap2.transport.KeepAliveHttpsTransportSE;

import ee.ria.mopp.androidmobileid.dto.ChallengeDto;
import ee.ria.mopp.androidmobileid.dto.MobileCreateSignatureRequest;

public class DigiDocService {

    private static final String NAMESPACE = "http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl";
    private static final String MAIN_REQUEST_URL = "https://digidocservice.sk.ee/?wsdl";
    private static final String SOAP_ACTION = "http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl/MobileCreateSignature";
    private static final String MOBILE_CREATE_SIGNATURE_METHOD_NAME = "MobileCreateSignature";

    public ChallengeDto mobileCreateSignature(MobileCreateSignatureRequest request) {
        SoapObject soapRequest = new SoapObject(NAMESPACE, MOBILE_CREATE_SIGNATURE_METHOD_NAME);


        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(soapRequest);
        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.debug = true;
            ht.call(SOAP_ACTION, envelope);
            Log.d("DDS_DUMP_REQUEST: ", ht.requestDump);
            Log.d("DDS_DUMP_RESPONSE: ", ht.responseDump);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ChallengeDto challengeDto = new ChallengeDto();
        return challengeDto;
    }

    private SoapSerializationEnvelope getSoapSerializationEnvelope(SoapObject request) {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = false;
        envelope.implicitTypes = true;
        envelope.setAddAdornments(false);
        envelope.setOutputSoapObject(request);
        return envelope;
    }

    private HttpTransportSE getHttpTransportSE() {
        HttpTransportSE ht = new KeepAliveHttpsTransportSE("digidocservice.sk.ee", 443, "", 6000);
        ht.debug = true;
        ht.setXmlVersionTag("<!--?xml version=\"1.0\" encoding= \"UTF-8\" ?-->");
        return ht;
    }
}
