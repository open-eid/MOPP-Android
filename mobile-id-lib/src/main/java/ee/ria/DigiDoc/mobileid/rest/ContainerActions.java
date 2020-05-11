/*
 * Copyright 2020 Riigi Infosüsteemide Amet
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

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
import timber.log.Timber;

public class ContainerActions {

    private static final String PEM_BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END_CERT = "-----END CERTIFICATE-----";

    private static final String SIGNATURE_PROFILE_TS = "time-stamp";

    private Container container;
    private String signatureId;

    private String containerPath;
    private String cert;

    public ContainerActions(String containerPath, String cert) {
        this.containerPath = containerPath;
        this.cert = cert;
    }

    public Container getContainer() {
        return container;
    }

    public static String calculateMobileIdVerificationCode(byte[] hash) {
        return String.format(Locale.ROOT, "%04d", hash != null ? ((0xFC & hash[0]) << 5) | (hash[hash.length - 1] & 0x7F) : 0);
    }

    private static String removeWhitespace(String text) {
        return text.replaceAll("\\s+", "");
    }

    public String generateHash() throws CertificateException {

        String certificateStringPEM = PEM_BEGIN_CERT + "\n" + cert + "\n" + PEM_END_CERT;
        X509Certificate x509 = x509Certificate(certificateStringPEM);

        container = Container.open(containerPath);
        signatureId = prepareSignature(container, x509, SIGNATURE_PROFILE_TS).id();

        container.save();

        byte[] dataToSignBytes = Base64.encode(getDataToSign());
        String dataToSign = new String(dataToSignBytes, StandardCharsets.UTF_8);

        return removeWhitespace(dataToSign);

    }

    public boolean validateSignature(String signatureValue) {
        try {
            Signature currentSignature = getSignatureFromContainer();

            byte[] signatureValueBytes = Base64.decode(signatureValue);

            currentSignature.setSignatureValue(signatureValueBytes);
            currentSignature.extendSignatureProfile(SIGNATURE_PROFILE_TS);
            currentSignature.validate();
            container.save();

            return true;
        } catch (Exception e) {
            Timber.e(e, "Exception when validating signature");
            return false;
        }
    }

    public void removeSignatureFromContainer() {
        if (container != null && signatureId != null) {
            for (int i = 0; i < container.signatures().size(); i++) {
                Signature signature = container.signatures().get(i);

                try {
                    if (signature.id().equals(signatureId)) {
                        container.removeSignature(i);
                        container.save();
                        break;
                    }
                } catch (Exception e) {
                    Timber.e(e, "Unable to remove signature from container");
                    throw new IllegalArgumentException("Could not remove signature from container");
                }
            }
        }
    }

    public byte[] getDataToSign() {
        return getSignatureFromContainer().dataToSign();
    }

    private Signature prepareSignature(Container container, X509Certificate certificate, String signatureProfile) throws CertificateEncodingException {
        try {
            return container.prepareWebSignature(certificate.getEncoded(), signatureProfile);
        } catch (CertificateEncodingException e) {
            Timber.e(e, "Unable to prepare signature. Certificate encoding failed");
            throw new CertificateEncodingException();
        }
    }

    private Signature getSignatureFromContainer() {
        Signature currentSignature = null;

        for (int i = 0; i < container.signatures().size(); i++) {
            if (container.signatures().get(i).id().equals(signatureId)) {
                currentSignature = container.signatures().get(i);
            }
        }

        if (currentSignature == null) {
            Timber.d("Unable to find signature from container");
            throw new IllegalArgumentException("Could not find signature with an ID of " + signatureId);
        }

        return currentSignature;
    }

    public static X509Certificate x509Certificate(String cert) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8)));
    }

}
