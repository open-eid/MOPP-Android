package ee.ria.EstEIDUtility;

import java.io.ByteArrayOutputStream;

import android.util.SparseArray;

public class EstEIDUtil {
	public enum CertType {
		CertAuth((byte)0xAA),
		CertSign((byte)0xDD);
		private byte value;
		private CertType(byte value) {
			this.value = value;
		}
	}
	public enum PinType {
		PIN1((byte)0x01),
		PIN2((byte)0x02),
		PUK((byte)0x00);
		private byte value;
		private PinType(byte value) {
			this.value = value;
		}
	}

	public static byte[] readCert(CertType certType, SMInterface sminterface) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		sminterface.transmitExtended(new byte[] {0x00, (byte) 0xA4, 0x00, 0x0C, 0x00});
		sminterface.transmitExtended(new byte[] {0x00, (byte) 0xA4, 0x01, 0x04, 0x02, (byte) 0xEE, (byte) 0xEE });
		sminterface.transmitExtended(new byte[] {0x00, (byte) 0xA4, 0x02, 0x04, 0x02, certType.value, (byte) 0xCE });

		for (byte i = 0; i <= 5; ++i) {
			byte[] data = sminterface.transmitExtended(new byte[] { 0x00, (byte)0xB0, i, 0x00, 0x00 });
			byteStream.write(data);
		}
		//byteStream.write(sminterface.transmit(new byte[]{0x00, (byte) 0xB0, 0x00, 0x00, 0x00, 0x06, 0x00}));
		return byteStream.toByteArray();
	}

	public static SparseArray<String> readPersonalFile(SMInterface sminterface) throws Exception {
		sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x00 });
		sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE });
		sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44 });
		SparseArray<String> result = new SparseArray<String>();
		for (byte i = 1; i <= 16; ++i) {
			byte[] data = sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xB2, i, 0x04, 0x00 });
			result.put(i, new String(data, "Windows-1252"));
		}
		return result;
	}

	public static byte[] sign(byte[] data, PinType pinType, SMInterface sminterface) throws Exception {
		sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x00 });
		sminterface.transmitExtended(new byte[] { 0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE });
		sminterface.transmitExtended(new byte[] { 0x00, 0x22, (byte)0xF3, 0x01, 0x00 });
		sminterface.transmitExtended(new byte[] { 0x00, 0x22, 0x41, (byte) 0xB8, 0x02, (byte) 0x83, 0x00 } );
		switch (pinType) {
		case PIN1:
			return sminterface.transmitExtended(Util.concat(new byte[] { 0x00, (byte) 0x88, 0x00, 0x00, (byte) data.length}, data));
		case PIN2:
			return sminterface.transmitExtended(Util.concat(new byte[] { 0x00, 0x2A, (byte)0x9E, (byte)0x9A, (byte) data.length}, data) );
		default:
			throw new Exception("Unsuported");
		}
	}

	public static boolean login(PinType pinType, byte[] pin, SMInterface sminterface) throws Exception {
		byte[] recv = sminterface.transmit(Util.concat(new byte[] { 0x00, 0x20, 0x00, pinType.value, (byte) pin.length}, pin ));
		return SMInterface.checkSW(recv);
	}

	public static boolean changePin (byte[] currentPin, byte[] newPin, PinType pinType, SMInterface sminterface) throws Exception {
		byte[] recv = sminterface.transmit(Util.concat(new byte[] {0x00 ,0x24, 0x00, pinType.value, (byte) (currentPin.length + newPin.length)}, currentPin, newPin));
		return SMInterface.checkSW(recv);
	}

	public static boolean unblockPin (byte[] currentPuk, PinType pinType, SMInterface sminterface) throws Exception {
		if (!login(PinType.PUK, currentPuk, sminterface))
			return false;
		byte[] recv = sminterface.transmitExtended(new byte[] {0x00, 0x2C, 0x03, pinType.value, 0x00});
		return SMInterface.checkSW(recv);
	}
}
