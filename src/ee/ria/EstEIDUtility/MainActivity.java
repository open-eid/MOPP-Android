package ee.ria.EstEIDUtility;

import java.util.ArrayList;
import java.util.Arrays;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;
import com.identive.libs.SCard;
import com.identive.libs.WinDefs;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.smartcardio.Card;
import android.smartcardio.CardException;
import android.smartcardio.CommandAPDU;
import android.smartcardio.ResponseAPDU;
import android.smartcardio.TerminalFactory;
import android.smartcardio.ipc.CardService;
import android.smartcardio.ipc.ICardService;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String ACTION_USB_PERMISSION = "ee.ria.EstEIDUtil.USB_PERMISSION";
	private TextView content;
	private SMInterface sminterface = null;
	Identive identive;
	ACS acs;
	Omnikey omnikey;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		content = (TextView) findViewById(R.id.content);
		identive = new Identive(getApplicationContext()) {
			@Override
			protected void connected() {
				read();
			}
		};
		acs = new ACS(this) {
			@Override
			protected void connected() {
				read();
			}
		};
		omnikey = new Omnikey(this) {
			@Override
			protected void connected() {
				read();
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		acs.close();
		identive.close();
		omnikey.close();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_read) {
			sminterface = null;
			if (identive.hasSupportedReader()) {
				sminterface = identive;
				identive.connect();
			}
			if (acs.hasSupportedReader()) {
				sminterface = acs;
				acs.connect();
			}
			if (omnikey.hasSupportedReader()) {
				sminterface = omnikey;
				omnikey.connect();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void read() {
		String result = "";
		try {
			send(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x00 });
			send(new byte[] { 0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE });
			send(new byte[] { 0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44 });
			for (byte i = 1; i <= 16; ++i) {
				byte[] data = send(new byte[] { 0x00, (byte) 0xB2, i, 0x04, 0x00 });
				result += new String(data, "Windows-1252") + "\n";
			}
			sminterface.close();
		} catch (Exception e) {
			result += e.getMessage();
			e.printStackTrace();
		}
		content.setText(result);
	}

	private byte[] send(byte[] apdu) throws Exception {
		byte[] recv = sminterface.transmit(apdu);
		byte sw1 = recv[recv.length - 2];
		byte sw2 = recv[recv.length - 1];
		recv = Arrays.copyOf(recv, recv.length - 2);
		if (sw1 == 0x61) {
			recv = concat(recv, sminterface.transmit(new byte[] { 0x00, (byte) 0xC0, 0x00, 0x00, sw2 }));
		} else if (sw1 != (byte) 0x90 && sw2 != (byte) 0x00) {
			throw new ReaderException("SW != 9000");
		}
		return recv;
	}

	private static byte[] concat(byte[]...arrays) {
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

	private static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}

	private abstract class SMInterface {
		abstract byte[] transmit(byte[] apdu) throws Exception;
		abstract void connect();
		abstract void connected();
		abstract void close();
	}

	private class ACS extends SMInterface {
		private PendingIntent permissionIntent;
		private UsbManager manager;
		private Reader ctx;
		private int slot = 0;

		ACS(Context context) {
			manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			ctx = new Reader(manager);

			permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					if (ACTION_USB_PERMISSION.equals(action)) {
						synchronized (this) {
							UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
							if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
								try {
									ctx.open(device);
									byte[] atr = ctx.power(slot, Reader.CARD_WARM_RESET);
									ctx.setProtocol(slot, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
									connected();
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}, new IntentFilter(ACTION_USB_PERMISSION));
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
		void connect() {
			for (final UsbDevice device: manager.getDeviceList().values()) {
				if (ctx.isSupported(device)) {
					manager.requestPermission(device, permissionIntent);
				}
			}
		}

		@Override
		void close() {
			ctx.close();
		}

		@Override
		byte[] transmit(byte[] apdu) throws Exception {
			byte[] recv = new byte[1024];
			int len = ctx.transmit(slot, apdu, apdu.length, recv, recv.length);
			return Arrays.copyOf(recv, len);
		}

		@Override
		protected void connected() {}
	}

	private class Identive extends SMInterface {
		private SCard ctx;

		Identive(Context context) {
			ctx = new SCard();
			ctx.SCardEstablishContext(getApplicationContext());
		}

		@Override
		public void close() {
			ctx.SCardDisconnect(WinDefs.SCARD_LEAVE_CARD);
			ctx.SCardReleaseContext();
		}

		boolean hasSupportedReader() {
			try {
				ArrayList<String> deviceList = new ArrayList<String>();
				ctx.USBRequestPermission(getApplicationContext());
				ctx.SCardListReaders(getApplicationContext(), deviceList);
				return deviceList.size() > 0;
			} catch(Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		void connect() {
			ArrayList<String> deviceList = new ArrayList<String>();
			ctx.SCardListReaders(getApplicationContext(), deviceList);
			ctx.SCardConnect(deviceList.get(0), WinDefs.SCARD_SHARE_EXCLUSIVE,
					(int) (WinDefs.SCARD_PROTOCOL_T0 | WinDefs.SCARD_PROTOCOL_T1));
			connected();
		}

		@Override
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

		@Override
		protected void connected() {}
	}

	private class Omnikey extends SMInterface {
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
				e.printStackTrace();
			}
		}

		boolean hasSupportedReader() {
			try {
				return mFactory.terminals().list().size() > 0;
			} catch (CardException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		byte[] transmit(byte[] apdu) throws Exception {
			ResponseAPDU response = card.getBasicChannel().transmit(new CommandAPDU(apdu));
			return response.getBytes();
		}

		@Override
		void connect() {
			try {
				card = mFactory.terminals().list().get(0).connect("T=0");
				connected();
			} catch (CardException e) {
				e.printStackTrace();
			}
		}

		@Override
		void connected() {}

		@Override
		void close() {
			if (card != null) {
				try {
					card.disconnect(true);
				} catch (CardException e) {
					e.printStackTrace();
				}
			}
			mService.releaseService();
		}
	}
}
