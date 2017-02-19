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

package ee.ria.tokenlibrary.util;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import timber.log.Timber;

public class Util {

    private static final String TAG = Util.class.getName();

    public static byte[] concat(byte[]... arrays) {
        int size = 0;
        for (byte[] array : arrays) {
            size += array.length;
        }
        byte[] result = new byte[size];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    public static String toHex(byte[] data) {
        return Hex.toHexString(data);
    }

    public static byte[] fromHex(String hexData) {
        return Hex.decode(hexData);
    }

    private static X509Certificate getX509Certificate(byte[] certificate) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(certificate));
    }

    public static String getCommonName(byte[] certificate) {
        try {
            X509Certificate cert = getX509Certificate(certificate);
            cert.getVersion();
            for (String x : cert.getSubjectDN().getName().replace("\\,", " ").split(",")) {
                if (x.contains("CN=")) {
                    return x.replace("CN=", "").trim();
                }
            }
            return cert.getSubjectDN().getName();
        } catch (Exception e) {
            Timber.e(e, "Error parsing CN from certificate");
        }
        return "";
    }
}
