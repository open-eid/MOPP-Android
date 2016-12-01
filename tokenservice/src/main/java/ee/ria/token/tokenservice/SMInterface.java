package ee.ria.token.tokenservice;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.smartcardio.Card;
import android.smartcardio.CardException;
import android.smartcardio.CommandAPDU;
import android.smartcardio.ResponseAPDU;
import android.smartcardio.TerminalFactory;
import android.smartcardio.ipc.CardService;
import android.smartcardio.ipc.ICardService;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;
import com.identive.libs.SCard;
import com.identive.libs.WinDefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import ee.ria.token.tokenservice.util.Util;

public abstract class SMInterface {
	private static final String TAG = "";
	private static final String ACTION_USB_PERMISSION = "ee.ria.token.tokenservice.USB_PERMISSION";
	public static final String ACS = "ACS";
	public static final String Identive = "Identive";
	public static final String Omnikey = "Omnikey";

	BroadcastReceiver reciever;

	public abstract byte[] transmit(byte[] apdu) throws Exception;
	public abstract void connect(Connected connected);
	public abstract void close();

	public BroadcastReceiver getReciever() {
		return reciever;
	}

	public static abstract class Connected {
		public abstract void connected();
	}

	public static SMInterface getInstance(Context context, String provider) {
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

	public static boolean checkSW(byte[] resp) {
		byte sw1 = resp[resp.length - 2];
		byte sw2 = resp[resp.length - 1];
		return sw1 == (byte) 0x90 && sw2 == (byte) 0x00;
	}

	private static class ACS extends SMInterface {
		private PendingIntent permissionIntent;
		private UsbManager manager;
		private Reader ctx;
		private int slot = 0;
		private Connected connected;

		ACS(Context context) {
			manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			ctx = new Reader(manager);

			permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
			reciever = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					Log.d(TAG, "onReceive: " + action + " " + ACTION_USB_PERMISSION);
					if (ACTION_USB_PERMISSION.equals(action)) {
						synchronized (this) {
							UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
							if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
								try {
									ctx.open(device);
									Thread.sleep(200); // HACK: card is not ready for power on
									byte[] atr = ctx.power(slot, Reader.CARD_WARM_RESET);
									Log.d(TAG, "onReceive: " + atr);
									ctx.setProtocol(slot, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
									connected.connected();
								} catch (Exception e) {
									Log.e(TAG, "onReceive: ", e);
								}
							}
						}
					}
				}
			};
			context.registerReceiver(reciever, new IntentFilter(ACTION_USB_PERMISSION));
		}

		boolean hasSupportedReader() {
			for (final UsbDevice device: manager.getDeviceList().values()) {
				if (ctx.isSupported(device)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void connect(Connected connected) {
			this.connected = connected;
			for (final UsbDevice device: manager.getDeviceList().values()) {
				if (ctx.isSupported(device)) {
					manager.requestPermission(device, permissionIntent);
				}
			}
		}

		@Override
		public	void close() {
			ctx.close();
		}

		@Override
		public byte[] transmit(byte[] apdu) throws Exception {
			byte[] recv = new byte[1024];
			int len = ctx.transmit(slot, apdu, apdu.length, recv, recv.length);
			return Arrays.copyOf(recv, len);
		}
	}

	private static class Identive extends SMInterface {
		private SCard ctx;
		Context context;

		Identive(Context context) {
			this.context = context;
			ctx = new SCard();
			ctx.SCardEstablishContext(context);
		}

		@Override
		public void close() {
			ctx.SCardDisconnect(WinDefs.SCARD_LEAVE_CARD);
			ctx.SCardReleaseContext();
		}

		boolean hasSupportedReader() {
			try {
				ArrayList<String> deviceList = new ArrayList<String>();
				ctx.USBRequestPermission(context);
				ctx.SCardListReaders(context, deviceList);
				return deviceList.size() > 0;
			} catch(Exception e) {
				Log.e(TAG, "hasSupportedReader: ", e);
			}
			return false;
		}

		@Override
		public void connect(Connected connected) {
			ArrayList<String> deviceList = new ArrayList<String>();
			ctx.SCardListReaders(context, deviceList);
			ctx.SCardConnect(deviceList.get(0), WinDefs.SCARD_SHARE_EXCLUSIVE,
					(int) (WinDefs.SCARD_PROTOCOL_T0 | WinDefs.SCARD_PROTOCOL_T1));
			connected.connected();
		}

		@Override
		public
		byte[] transmit(byte[] apdu) throws Exception {
			SCard.SCardIOBuffer io = ctx.new SCardIOBuffer();
			io.setAbyInBuffer(apdu);
			io.setnBytesReturned(apdu.length);
			io.setAbyOutBuffer(new byte[0x8000]);
			io.setnOutBufferSize(0x8000);
			ctx.SCardTransmit(io);
			if (io.getnBytesReturned() == 0) {
				throw new Exception("Failed to send apdu");
			}
			String rstr = "";
			for(int k = 0; k < io.getnBytesReturned(); k++){
				int temp = io.getAbyOutBuffer()[k] & 0xFF;
				if(temp < 16){
					rstr = rstr.toUpperCase() + "0" + Integer.toHexString(io.getAbyOutBuffer()[k]) ;
				}else{
					rstr = rstr.toUpperCase() + Integer.toHexString(temp) ;
				}
			}
			return Arrays.copyOf(io.getAbyOutBuffer(), io.getnBytesReturned());
		}
	}

	private static class Omnikey extends SMInterface {
		private ICardService mService;
		private TerminalFactory mFactory;
		private Card card;

		Omnikey(Context context) {
			try {
				Intent serviceIntent = new Intent("com.theobroma.cardreadermanager.backendipc.BroadcastRecord");
				serviceIntent.setPackage("com.theobroma.cardreadermanager.backendipc");
				if (context.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
					//context.bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);
					context.startService(serviceIntent);
				}
				mService = CardService.getInstance(context.getApplicationContext());
				mFactory = mService.getTerminalFactory();
			} catch (Exception e) {
				Log.e(TAG, "Omnikey: ", e);
			}
		}

		boolean hasSupportedReader() {
			try {
				return mFactory.terminals().list().size() > 0;
			} catch (CardException e) {
				Log.e(TAG, "hasSupportedReader: ", e);
				return false;
			}
		}

		@Override
		public
		byte[] transmit(byte[] apdu) throws Exception {
			ResponseAPDU response = card.getBasicChannel().transmit(new CommandAPDU(apdu));
			return response.getBytes();
		}

		@Override
		public void connect(Connected connected) {
			try {
				card = mFactory.terminals().list().get(0).connect("T=0");
				connected.connected();
			} catch (CardException e) {
				Log.e(TAG, "connect: ", e);
			}
		}

		@Override
		public
		void close() {
			if (card != null) {
				try {
					card.disconnect(true);
				} catch (CardException e) {
					Log.e(TAG, "close: ", e);
				}
			}
			mService.releaseService();
		}
	}

	public static class NFC extends SMInterface {
		private IsoDep nfc;

		public NFC(Tag tag) {
			nfc = IsoDep.get(tag);
		}

		@Override
		public byte[] transmit(byte[] apdu) throws Exception {
			return nfc.transceive(apdu);
		}

		@Override
		public void connect(Connected connected) {
			try {
				nfc.connect();
				nfc.setTimeout(5000);
				connected.connected();
			} catch(IOException e) {
				Log.e(TAG, "connect: ", e);
			}
		}

		@Override
		public void close() {
			try {
				nfc.close();
			} catch (IOException e) {
				Log.e(TAG, "close: ", e);
			}
		}
	}
}
