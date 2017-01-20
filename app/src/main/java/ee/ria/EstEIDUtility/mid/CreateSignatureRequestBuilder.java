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

package ee.ria.EstEIDUtility.mid;

import android.util.Base64;

import java.util.ArrayList;
import java.util.List;

import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.Signature;
import ee.ria.mopp.androidmobileid.dto.DataFileDto;
import ee.ria.mopp.androidmobileid.dto.MobileCreateSignatureRequest;

public class CreateSignatureRequestBuilder {

    private static final String FORMAT = "BDOC";
    private static final String VERSION = "2.1";
    private static final String MESSAGING_MODE = "asynchClientServer";
    private static final String SERVICE_NAME = "DigiDoc3";
    private static final int ASYNC_CONFIGURATION = 0;
    private static final String DIGEST_TYPE = "sha256";
    private static final String DIGEST_METHOD = "http://www.w3.org/2001/04/xmlenc#sha256";

    private String phoneNr;
    private String idCode;
    private String message;
    private String language;
    private SigningProfile signingProfile;
    private ContainerFacade container;
    private MobileCreateSignatureRequest request;

    private CreateSignatureRequestBuilder() {}

    public MobileCreateSignatureRequest build() {
        request = new MobileCreateSignatureRequest();
        buildConstantParameters();
        buildPersonalInfo();
        buildContainerParameters();
        return request;
    }

    private void buildPersonalInfo() {
        request.setIdCode(idCode);
        request.setPhoneNr(phoneNr);
        request.setLanguage(language == null ? "EST" : language);
        request.setMessageToDisplay(message == null ? "Sign " + container.getName() : message);
    }

    private void buildConstantParameters() {
        request.setFormat(FORMAT);
        request.setVersion(VERSION);
        request.setMessagingMode(MESSAGING_MODE);
        request.setServiceName(SERVICE_NAME);
        request.setAsyncConfiguration(ASYNC_CONFIGURATION);
    }

    private void buildContainerParameters() {
        request.setSigningProfile(getSigningProfile());
        request.setSignatureId(getNextSignatureId());
        buildDataFiles();
    }

    private void buildDataFiles() {
        List<DataFile> dataFiles = container.getDataFiles();
        List<DataFileDto> dataFileDtos = new ArrayList<>();
        for (DataFile df : dataFiles) {
            dataFileDtos.add(createDataFileDto(df));
        }
        request.setDatafiles(dataFileDtos);
    }

    private DataFileDto createDataFileDto(DataFile df) {
        DataFileDto dto = new DataFileDto();
        dto.setId(df.id());
        dto.setMimeType(df.mediaType());
        dto.setDigestType(DIGEST_TYPE);
        dto.setDigestValue(Base64.encodeToString(df.calcDigest(DIGEST_METHOD), Base64.DEFAULT));
        return dto;
    }

    private String getNextSignatureId() {
        List<Signature> signatures = container.getSignatures();
        List<String> existingIds = new ArrayList<>();
        for (Signature s : signatures) {
            existingIds.add(s.id().toUpperCase());
        }
        int id = 0;
        while (existingIds.contains("S" + id)) ++id;
        return "S" + id;
    }

    private String getSigningProfile() {
        List<Signature> signatures = container.getSignatures();
        if (signatures.isEmpty()) {
            return signingProfile.name();
        } else {
            return parseLibdigidocProfile(signatures.get(0).profile());
        }
    }

    private String parseLibdigidocProfile(String profile) {
        if ("time-stamp".equals(profile)) {
            return SigningProfile.LT.name();
        } else {
            return SigningProfile.LT_TM.name();
        }
    }

    public static CreateSignatureRequestBuilder aCreateSignatureRequest() {
        return new CreateSignatureRequestBuilder();
    }

    public CreateSignatureRequestBuilder withSingingProfile(SigningProfile signingProfile) {
        this.signingProfile = signingProfile;
        return this;
    }


    public CreateSignatureRequestBuilder withPhoneNr(String phoneNr) {
        this.phoneNr = phoneNr;
        return this;
    }

    public CreateSignatureRequestBuilder withIdCode(String idCode) {
        this.idCode = idCode;
        return this;
    }

    public CreateSignatureRequestBuilder withContainer(ContainerFacade container) {
        this.container = container;
        return this;
    }

    public CreateSignatureRequestBuilder withMessageToDisplay(String message) {
        this.message = message;
        return this;
    }

    public enum SigningProfile {
        LT,
        LT_TM
    }
}
