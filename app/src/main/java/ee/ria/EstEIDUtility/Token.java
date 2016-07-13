package ee.ria.EstEIDUtility;

import android.util.SparseArray;

public abstract class Token {
	public static CertListener certListener;
	public static SignListener signListener;
	public static PersonalFileListener personalFileListener;

	public enum CertType {
		CertAuth((byte)0xAA),
		CertSign((byte)0xDD);
		byte value;
		private CertType(byte value) {
			this.value = value;
		}
	}

	public enum PinType {
		PIN1((byte)0x01),
		PIN2((byte)0x02),
		PUK((byte)0x00);
		byte value;
		private PinType(byte value) {
			this.value = value;
		}
	}

	public static interface CertListener {
		public abstract void onCertificateResponse(CertType type, byte[] cert);
		public abstract void onCertificateError(String msg);
	}

	public void setCertListener(CertListener listener) {
		Token.certListener = listener;
	}

	public CertListener getCertListener() {
		return certListener;
	}

	public static interface SignListener {
		public abstract void onSignResponse(byte[] signature);
		public abstract void onSignError(String msg);
	}

	public void setSignListener(SignListener listener) {
		Token.signListener = listener;
	}

	public SignListener getSignListener() {
		return signListener;
	}

	public static interface PersonalFileListener {
		public abstract void onPersonalFileResponse(SparseArray<String> result);
		public abstract void onPersonalFileError(String msg);
	}

	public void setPersonalFileListener(PersonalFileListener listener) {
		Token.personalFileListener = listener;
	}

	public PersonalFileListener getPersonalFileListener () {
		return personalFileListener;
	}

	public abstract void readCert(CertType certType);
	public abstract void sign(PinType type, byte[] data);
	public abstract void readPersonalFile();
}
