package ee.ria.token.tokenservice.reader.impl;

import android.app.IntentService;
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
import java.util.HashMap;

import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.reader.CardReader;
import ee.ria.token.tokenservice.reader.SmartCardCommunicationException;

public class ACS extends CardReader {

    private static final String TAG = ACS.class.getName();

    private PendingIntent permissionIntent;
    private UsbManager manager;
    private static Reader ctx;
    private static int slot = 0;
    private static Connected connected;
    private Context context;

    private static final long CARD_LISTENER_RETRY = 2500;

    public ACS(Context context, TokenService.SMConnected connected) {
        this.context = context;
        this.connected = connected;
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        ctx = new Reader(manager);

        createUsbAttachReceiver();
        createUsbDetachReceiver();
        createUsbPermissionReceiver();
        checkAttachedDevices();
    }

    private void createUsbPermissionReceiver() {
        permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_CANCEL_CURRENT);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                            try {
                                ctx.open(device);
                                Intent cardInsertedService = new Intent(context, CardInsertedService.class);
                                context.startService(cardInsertedService);
                            } catch (Exception e) {
                                Log.e(TAG, "onReceive: ", e);
                            }
                        }
                    }
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(ACTION_USB_PERMISSION));
    }

    private void createUsbDetachReceiver() {
        usbDetachReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    ctx.close();
                    ctx = null;
                    Intent cardAbsentIntent = new Intent(TokenService.CARD_ABSENT_INTENT);
                    ACS.this.context.sendBroadcast(cardAbsentIntent);
                }
            }
        };
        context.registerReceiver(usbDetachReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
    }

    private void createUsbAttachReceiver() {
        usbAttachReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    ctx = new Reader(manager);
                    askPermission();
                }
            }
        };
        context.registerReceiver(usbAttachReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
    }

    private void checkAttachedDevices() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (deviceList == null) {
            return;
        }
        for (UsbDevice device : deviceList.values()) {
            if (device != null) {
                askPermission();
            }
        }
    }

    public static class CardInsertedService extends IntentService {

        public CardInsertedService() {
            super(CardInsertedService.class.getName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            try {
                while (ctx != null && ctx.isOpened()) {
                    switch (ctx.getState(slot)) {
                        case Reader.CARD_PRESENT:
                            try {
                                ctx.power(slot, Reader.CARD_WARM_RESET);
                                ctx.setProtocol(slot, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
                            } catch (ReaderException e) {
                                Log.e(TAG, "ReaderException: ", e);
                            }
                            connected.connected();
                            Intent cardInsertedIntent = new Intent(TokenService.CARD_PRESENT_INTENT);
                            sendBroadcast(cardInsertedIntent);
                            break;
                        case Reader.CARD_ABSENT:
                            Intent cardAbsentIntent = new Intent(TokenService.CARD_ABSENT_INTENT);
                            sendBroadcast(cardAbsentIntent);
                            break;
                    }
                    try {
                        Thread.sleep(CARD_LISTENER_RETRY);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "onHandleIntent: ", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "onHandleIntent: ", e);
                //TODO: unplug and plugin message
            }
        }

    }

    private void askPermission() {
        if (!hasSupportedReader()) {
            return;
        }
        for (final UsbDevice device : manager.getDeviceList().values()) {
            if (ctx.isSupported(device)) {
                manager.requestPermission(device, permissionIntent);
            }
        }
    }

    private boolean hasSupportedReader() {
        for (final UsbDevice device : manager.getDeviceList().values()) {
            if (ctx.isSupported(device)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void connect(Connected connected) {
    }

    @Override
    public void close() {
        ctx.close();
    }

    @Override
    public boolean isSecureChannel() {
        return true;
    }

    @Override
    public byte[] transmit(byte[] apdu) {
        byte[] recv = new byte[1024];
        int len;
        try {
            len = ctx.transmit(slot, apdu, apdu.length, recv, recv.length);
        } catch (ReaderException e) {
            throw new SmartCardCommunicationException(e);
        }
        return Arrays.copyOf(recv, len);
    }
}
