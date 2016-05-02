package ee.ria.EstEIDUtility;

import java.util.ArrayList;
import java.util.Arrays;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;
import com.identive.libs.SCard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String ACTION_USB_PERMISSION = "ee.ria.EstEIDUtil.USB_PERMISSION";
	private TextView content;
	private ACS acs;
	private Identive identive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		content = (TextView) findViewById(R.id.content);
		acs = new ACS(this) {
			@Override
			protected void connected() {
				read();
			}
		};
		identive = new Identive(getApplicationContext());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_read) {
			if (acs.hasSupportedReader()) {
				acs.connect();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void read() {
		String result = "";
		try {
			send(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C });
			send(new byte[] { 0x00, (byte) 0xA4, 0x01, 0x0C, 0x02, (byte) 0xEE, (byte) 0xEE });
			send(new byte[] { 0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) 0x50, (byte) 0x44 });
			for (byte i = 1; i <= 16; ++i) {
				byte[] data = send(new byte[] { 0x00, (byte) 0xB2, i, 0x04, 0x00 });
				result += new String(data, "Windows-1252") + "\n";
			}
			acs.close();
		} catch (Exception e) {
			result += e.getMessage();
			e.printStackTrace();
		}
		content.setText(result);
	}

	private byte[] send(byte[] apdu) throws Exception {
		byte[] recv = acs.transmit(apdu);
		byte sw1 = recv[recv.length - 2];
		byte sw2 = recv[recv.length - 1];
		recv = Arrays.copyOf(recv, recv.length - 2);
		if (sw1 == 0x61) {
			recv = concat(recv, acs.transmit(new byte[] { 0x00, (byte) 0xC0, 0x00, 0x00, sw2 }));
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

	private class ACS {
		private PendingIntent permissionIntent;
		private UsbManager manager;
		private Reader acs;
		private int slot = 0;

		ACS(Context context) {
			manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			acs = new Reader(manager);

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
									int slot = 0;
									acs.open(device);
									byte[] atr = acs.power(slot, Reader.CARD_WARM_RESET);
									acs.setProtocol(slot, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
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
				if (acs.isSupported(device)) {
					return true;
				}
			}
			return false;
		}

		void connect() {
			for (final UsbDevice device: manager.getDeviceList().values()) {
				if (acs.isSupported(device)) {
					manager.requestPermission(device, permissionIntent);
				}
			}
		}

		protected void connected() {}

		void close() {
			acs.close();
		}

		byte[] transmit(byte[] apdu) throws Exception {
			byte[] recv = new byte[1024];
			int len = acs.transmit(slot, apdu, apdu.length, recv, recv.length);
			return Arrays.copyOf(recv, len);
		}
	}

	private class Identive {
		private SCard identive;

		Identive(Context context) {
			identive = new SCard();
			identive.SCardEstablishContext(getApplicationContext());
		}

		boolean hasSupportedReader() {
			try {
				ArrayList<String> deviceList = new ArrayList<String>();
				identive.USBRequestPermission(getApplicationContext());
				identive.SCardListReaders(getApplicationContext(), deviceList);
				return deviceList.size() > 0;
			} catch(Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}
}
