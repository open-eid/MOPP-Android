package ee.ria.token.tokenservice;

import android.util.SparseArray;

public interface Token {

	byte[] sign(PinType type, String pin, byte[] data) throws Exception;
	SparseArray<String> readPersonalFile() throws Exception;
	boolean changePin(PinType pinType, byte[] currentPin, byte[] newPin) throws Exception;
	boolean unblockPin(PinType pinType, byte[] puk) throws Exception;
	byte[] readCert(CertType type) throws Exception;

	enum CertType {
		CertAuth((byte)0xAA),
		CertSign((byte)0xDD);
		public byte value;
		CertType(byte value) {
			this.value = value;
		}
	}

	enum PinType {
		PIN1((byte)0x01),
		PIN2((byte)0x02),
		PUK((byte)0x00);
		public byte value;
		PinType(byte value) {
			this.value = value;
		}
	}

}
