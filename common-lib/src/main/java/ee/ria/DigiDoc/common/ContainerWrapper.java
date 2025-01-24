/*
 * Copyright 2017 - 2024 Riigi Infosüsteemi Amet
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
package ee.ria.DigiDoc.common;

import android.content.Context;

import androidx.annotation.Nullable;

import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.ExternalSigner;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.StringVector;

public class ContainerWrapper {

    private static final String SIGNATURE_PROFILE_TS = "time-stamp";

    private final Context context;
    private final Container container;
    private Signature signature;
    private ExternalSigner signer;

    public ContainerWrapper(Context context, String containerPath) {
        this.context = context;
        this.container = Container.open(containerPath, new DigidocContainerOpenCB(false));
    }

    public Container getContainer() {
        return container;
    }

    public String prepareSignature(String cert, @Nullable RoleData roleData) throws CertificateException {
        signer = new ExternalSigner(CertificateUtil.x509Certificate(cert).getEncoded());
        signer.setProfile(SIGNATURE_PROFILE_TS);
        signer.setUserAgent(UserAgentUtil.getUserAgent(context, false));
        if (roleData != null) {
            signer.setSignerRoles(new StringVector(TextUtil.removeEmptyStrings(roleData.getRoles())));
            signer.setSignatureProductionPlace(roleData.getCity(), roleData.getState(),
                    roleData.getZip(), roleData.getCountry());
        }

        signature = container.prepareSignature(signer);

        if (signature != null) {
            byte[] dataToSignBytes = Base64.encode(signature.dataToSign());
            String dataToSign = new String(dataToSignBytes, StandardCharsets.UTF_8);
            return removeWhitespaces(dataToSign);
        }

        return null;
    }

    public void finalizeSignature(String signatureValue) {
        if (signature == null) {
            throw new IllegalStateException("Cannot finalize uninitialized signature");
        }

        if (signer == null) {
            throw new IllegalStateException("Cannot finalize uninitialized signer");
        }

        byte[] signatureValueBytes = Base64.decode(signatureValue);
        signature.setSignatureValue(signatureValueBytes);
        signature.extendSignatureProfile(signer);
        container.save();
    }

    private String removeWhitespaces(String text) {
        return text.replaceAll("\\s+", "");
    }

}
