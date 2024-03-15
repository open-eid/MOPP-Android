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

import androidx.annotation.Nullable;

import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.StringVector;

public class ContainerWrapper {

    private static final String SIGNATURE_PROFILE_TS = "time-stamp";

    private final Container container;
    private Signature signature;

    public ContainerWrapper(String containerPath) {
        this.container = Container.open(containerPath, new DigidocContainerOpenCB(false));
    }

    public Container getContainer() {
        return container;
    }

    public String prepareSignature(String cert, @Nullable RoleData roleData) throws CertificateException {
        if (roleData != null) {
            signature = container.prepareWebSignature(CertificateUtil.x509Certificate(cert).getEncoded(), SIGNATURE_PROFILE_TS,
                    new StringVector(TextUtil.removeEmptyStrings(roleData.getRoles())), roleData.getCity(),
                    roleData.getState(), roleData.getZip(), roleData.getCountry());
        } else {
            signature = container.prepareWebSignature(CertificateUtil.x509Certificate(cert).getEncoded(), SIGNATURE_PROFILE_TS);
        }
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
        byte[] signatureValueBytes = Base64.decode(signatureValue);
        signature.setSignatureValue(signatureValueBytes);
        signature.extendSignatureProfile(SIGNATURE_PROFILE_TS);
        container.save();
    }

    private String removeWhitespaces(String text) {
        return text.replaceAll("\\s+", "");
    }

}
