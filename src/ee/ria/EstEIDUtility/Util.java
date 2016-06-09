package ee.ria.EstEIDUtility;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Util {
	static byte[] concat(byte[]...arrays) {
		int size = 0;
		for (byte[] array: arrays) {
			size += array.length;
		}
		byte[] result = new byte[size];
		int pos = 0;
		for (byte[] array: arrays) {
			System.arraycopy(array, 0, result, pos, array.length);
			pos += array.length;
		}
		return result;
	}

	static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}

	static byte[] digest(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(data);
			return md.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static PublicKey getPublicKey(byte[] certBytes) throws CertificateException {
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
				new ByteArrayInputStream(certBytes));
		return certificate.getPublicKey();
	}

	public static String getCommonName(byte[] certificate, SMInterface sminterface) {
		try {
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate info = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certificate));
			info.getVersion();
			for (String x : info.getSubjectDN().getName().replace("\\,", " ").split(",")) {
				if (x.contains("CN=")) {
					return x.replace("CN=", "").trim();
				}
			}
			return info.getSubjectDN().getName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
