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

package ee.ria.EstEIDUtility.certificate;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.IETFUtils;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;

import timber.log.Timber;

public class X509Cert {

    private static final String TAG = X509Cert.class.getName();

    private X509Certificate certificate;

    public X509Cert(byte[] signingCertificateDer) {
        certificate = getSignatureCertificate(signingCertificateDer);
        Timber.tag(TAG);
    }

    public boolean isValid() {
        try {
            certificate.checkValidity(new Date());
        } catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        }
        return true;
    }

    public String getValueByObjectIdentifier(ASN1ObjectIdentifier identifier) {
        X500Name x500name = null;
        try {
            x500name = new JcaX509CertificateHolder(certificate).getSubject();
        } catch (CertificateEncodingException e) {
            Timber.e(e, "Error getting value by ASN1 Object identifier");
        }
        if (x500name == null) {
            return null;
        }
        RDN[] rdNs = x500name.getRDNs(identifier);
        if (rdNs.length == 0) {
            return null;
        }
        RDN c = rdNs[0];
        return IETFUtils.valueToString(c.getFirst().getValue());
    }


    private X509Certificate getSignatureCertificate(byte[] signingCertificateDer) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(signingCertificateDer));
        } catch (CertificateException e) {
            Timber.e(e, "Error creating certificate object from byte array");
        }
        return null;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}
