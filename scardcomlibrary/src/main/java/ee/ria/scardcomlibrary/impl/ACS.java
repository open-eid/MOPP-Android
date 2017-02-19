/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.scardcomlibrary.impl;

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

import ee.ria.scardcomlibrary.CardReader;
import ee.ria.scardcomlibrary.SmartCardCommunicationException;
import timber.log.Timber;

public class ACS extends CardReader {

    private static final String TAG = ACS.class.getName();

    public static final String TOKEN_AVAILABLE_INTENT = "TOKEN_AVAILABLE_INTENT";
    public static final String CARD_PRESENT_INTENT = "CARD_PRESENT_INTENT";
    public static final String CARD_ABSENT_INTENT = "CARD_ABSENT_INTENT";

    private PendingIntent permissionIntent;
    private UsbManager manager;
    private static Reader ctx;
    private static int slot = 0;
    private Context context;

    private static final long CARD_LISTENER_RETRY = 2500;

    public ACS(Context context) {
        this.context = context;
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        ctx = new Reader(manager);

        createUsbAttachReceiver();
        createUsbDetachReceiver();
        createUsbPermissionReceiver();
        checkAttachedDevices();
        Timber.tag(TAG);
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
                                Intent cardInsertedService = new Intent(context, CardStateService.class);
                                context.startService(cardInsertedService);
                            } catch (Exception e) {
                                Timber.e(e, "onReceive");
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
                    Intent cardAbsentIntent = new Intent(CARD_ABSENT_INTENT);
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

    @Override
    public void connect(Connected connected) {}

    @Override
    public void close() {}

    public static class CardStateService extends IntentService {

        public CardStateService() {
            super(CardStateService.class.getName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            try {
                boolean isAbsentSent = false;
                while (ctx != null && ctx.isOpened()) {
                    switch (ctx.getState(slot)) {
                        case Reader.CARD_PRESENT:
                            try {
                                ctx.power(slot, Reader.CARD_WARM_RESET);
                                ctx.setProtocol(slot, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
                            } catch (ReaderException e) {
                                Timber.e(e, "Reader Exception Occurred");
                            }

                            Intent cardInsertedIntent = new Intent(TOKEN_AVAILABLE_INTENT);
                            sendBroadcast(cardInsertedIntent);

                            isAbsentSent = false;
                            break;
                        case Reader.CARD_ABSENT:
                            if (!isAbsentSent) {
                                Intent cardAbsentIntent = new Intent(CARD_ABSENT_INTENT);
                                sendBroadcast(cardAbsentIntent);
                                isAbsentSent = true;
                            }
                            break;
                        default:
                            isAbsentSent = false;
                            break;
                    }
                    try {
                        Thread.sleep(CARD_LISTENER_RETRY);
                    } catch (InterruptedException e) {
                        Timber.e(e, "waiting for card listener retry %i millis was interrupted ", CARD_LISTENER_RETRY);
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "Exception on Handle Intent");
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
