/*
 * app
 * Copyright 2020 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.android.signature.update.smartid;

import java.nio.charset.StandardCharsets;

import ee.ria.DigiDoc.common.MessageUtil;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.dto.request.SmartIDSignatureRequest;

final class SmartCreateSignatureRequestHelper {

    private static final int MAX_DISPLAY_MESSAGE_BYTES = 60;

    private static final String DIGEST_TYPE = "SHA256";

    private static final String RELYING_PARTY_NAME = "RIA DigiDoc";

    static SmartIDSignatureRequest create(SignedContainer container, String uuid, String proxyUrl,
                                          String skUrl, String country, String nationalIdentityNumber,
                                          String displayMessage) {
        SmartIDSignatureRequest request = new SmartIDSignatureRequest();
        request.setRelyingPartyName(RELYING_PARTY_NAME);
        request.setRelyingPartyUUID(uuid == null || uuid.isEmpty() ? "00000000-0000-0000-0000-000000000000" : uuid);
        request.setUrl(uuid == null || uuid.isEmpty() ? proxyUrl : skUrl);
        request.setCountry(country);
        request.setNationalIdentityNumber(nationalIdentityNumber);

        request.setContainerPath(container.file().getPath());
        request.setHashType(DIGEST_TYPE);
        request.setDisplayText(MessageUtil.trimDisplayMessageIfNotWithinSizeLimit(displayMessage, MAX_DISPLAY_MESSAGE_BYTES, StandardCharsets.UTF_8));
        return request;
    }
}
