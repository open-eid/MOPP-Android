/*
 * smart-id-lib
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

package ee.ria.DigiDoc.smartid.rest;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;

public class ContainerActions {

    private static final String SIGNATURE_PROFILE_TS = "time-stamp";

    private Container container;
    private Signature signature;

    private String containerPath;
    private String cert;

    public ContainerActions(String containerPath, String cert) {
        this.containerPath = containerPath;
        this.cert = cert;
    }

    public static String calculateSmartIdVerificationCode(String base64Hash) throws NoSuchAlgorithmException {
        byte[] hash = Base64.decode(base64Hash);
        byte[] codeDigest = MessageDigest.getInstance("SHA-256").digest(hash);
        String code = String.valueOf(ByteBuffer.wrap(codeDigest).getShort(codeDigest.length - 2) & 0xffff);
        String paddedCode = "0000" + code;
        return paddedCode.substring(code.length());
    }

    public String generateHash() throws CertificateException {
        container = Container.open(containerPath);
        signature = container.prepareWebSignature(x509Certificate(cert).getEncoded(), SIGNATURE_PROFILE_TS);
        byte[] dataToSignBytes = Base64.encode(signature.dataToSign());
        String dataToSign = new String(dataToSignBytes, StandardCharsets.UTF_8);
        return dataToSign.replaceAll("\\s+", "");
    }

    public void setSignatureValueAndValidate(String signatureValue) {
        byte[] signatureValueBytes = Base64.decode(signatureValue);
        signature.setSignatureValue(signatureValueBytes);
        signature.extendSignatureProfile(SIGNATURE_PROFILE_TS);
        signature.validate();
        container.save();
    }

    public static X509Certificate x509Certificate(String cert) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(Base64.decode(cert)));
    }
}
