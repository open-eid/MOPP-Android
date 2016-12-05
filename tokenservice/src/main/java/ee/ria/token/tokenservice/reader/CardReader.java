package ee.ria.token.tokenservice.reader;

import android.content.BroadcastReceiver;
import android.content.Context;

import com.acs.smartcard.ReaderException;

import java.util.Arrays;

import ee.ria.token.tokenservice.reader.impl.ACS;
import ee.ria.token.tokenservice.reader.impl.Identive;
import ee.ria.token.tokenservice.reader.impl.Omnikey;
import ee.ria.token.tokenservice.util.Util;

public abstract class CardReader implements ScardComChannel {
	protected static final String TAG = "SmartCard";
	protected static final String ACTION_USB_PERMISSION = "ee.ria.token.tokenservice.USB_PERMISSION";
	public static final String ACS = "ACS";
	public static final String Identive = "Identive";
	public static final String Omnikey = "Omnikey";

	protected BroadcastReceiver reciever;

	public abstract void connect(Connected connected);
	public abstract void close();

	public BroadcastReceiver getReciever() {
		return reciever;
	}

	public static abstract class Connected {
		public abstract void connected();
	}

	public static CardReader getInstance(Context context, String provider) {
		if (provider.equals(ACS)) {
			ACS acs = new ACS(context);
			if (acs.hasSupportedReader()) {
				return acs;
			}
		}

		if (provider.equals(Identive)) {
			Identive identive = new Identive(context);
			if (identive.hasSupportedReader()) {
				return identive;
			}
		}

		if (provider.equals(Omnikey)) {
			Omnikey omnikey = new Omnikey(context);
			if (omnikey.hasSupportedReader()) {
				return omnikey;
			}
		}
		return null;
	}

	@Override
	public byte[] transmitExtended(byte[] apdu) throws Exception {
		byte[] recv = transmit(apdu);
		byte sw1 = recv[recv.length - 2];
		byte sw2 = recv[recv.length - 1];
		recv = Arrays.copyOf(recv, recv.length - 2);
		if (sw1 == 0x61) {
			recv = Util.concat(recv, transmit(new byte[] { 0x00, (byte) 0xC0, 0x00, 0x00, sw2 }));
		} else if (!(sw1 == (byte) 0x90 && sw2 == (byte) 0x00)) {
			throw new ReaderException("SW != 9000");
		}
		return recv;
	}
}
