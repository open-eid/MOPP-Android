package ee.ria.token.tokenservice.reader;

import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.Arrays;

import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.reader.impl.ACS;
import ee.ria.token.tokenservice.reader.impl.Identive;
import ee.ria.token.tokenservice.reader.impl.Omnikey;
import ee.ria.token.tokenservice.util.Util;

public abstract class CardReader implements SmartCardComChannel {

	protected static final String TAG = "CardReader";
	protected static final String ACTION_USB_PERMISSION = "ee.ria.token.tokenservice.USB_PERMISSION";

	public static final String ACS = "ACS";
	public static final String Identive = "Identive";
	public static final String Omnikey = "Omnikey";

	public BroadcastReceiver receiver;
	public BroadcastReceiver usbAttachReceiver;
	public BroadcastReceiver usbDetachReceiver;

	public abstract void connect(Connected connected);
	public abstract void close();

	public static abstract class Connected {
		public abstract void connected();
	}

	public static CardReader getInstance(Context context, String provider, TokenService.SMConnected callback) {
		if (provider.equals(ACS)) {
			return new ACS(context, callback);
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
	public byte[] transmitExtended(byte[] apdu) {
		byte[] recv = transmit(apdu);
		byte sw1 = recv[recv.length - 2];
		byte sw2 = recv[recv.length - 1];
		recv = Arrays.copyOf(recv, recv.length - 2);
		if (sw1 == 0x61) {
			recv = Util.concat(recv, transmit(new byte[] { 0x00, (byte) 0xC0, 0x00, 0x00, sw2 }));
		} else if (!(sw1 == (byte) 0x90 && sw2 == (byte) 0x00)) {
			throw new ScardOperationUnsuccessfulException("SW != 9000");
		}
		return recv;
	}
}
