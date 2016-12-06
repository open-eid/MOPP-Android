package ee.ria.token.tokenservice.reader.impl;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

import java.util.Arrays;

import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.reader.SmartCardCommunicationException;

public class ACS extends CardReader {

    private static final String TAG = "ACS";

    private PendingIntent permissionIntent;
    private UsbManager manager;
    private Reader ctx;
    private int slot = 0;
    private Connected connected;

    public ACS(Context context) {
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

    public boolean hasSupportedReader() {
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
    public boolean isSecureChannel() {
        return true;
    }

    @Override
    public byte[] transmit(byte[] apdu) {
        byte[] recv = new byte[1024];
        int len = 0;
        try {
            len = ctx.transmit(slot, apdu, apdu.length, recv, recv.length);
        } catch (ReaderException e) {
            throw new SmartCardCommunicationException(e);
        }
        return Arrays.copyOf(recv, len);
    }
}
