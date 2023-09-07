package ee.ria.DigiDoc.smartcardreader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.common.base.Optional;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import timber.log.Timber;

final class SmartCardReaderOnSubscribe implements ObservableOnSubscribe<Optional<SmartCardReader>> {

    private static final String ACTION_USB_DEVICE_PERMISSION = BuildConfig.LIBRARY_PACKAGE_NAME +
            ".USB_DEVICE_PERMISSION";

    private final Context context;
    private final UsbManager usbManager;
    private final SmartCardReaderManager smartCardReaderManager;

    @Nullable private UsbDevice currentDevice;
    @Nullable private SmartCardReader currentReader;

    SmartCardReaderOnSubscribe(Context context, SmartCardReaderManager smartCardReaderManager) {
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.smartCardReaderManager = smartCardReaderManager;
    }

    @Override
    public void subscribe(ObservableEmitter<Optional<SmartCardReader>> emitter) {
        BroadcastReceiver deviceAttachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = getUsbDevice(intent);
                Timber.log(Log.DEBUG, "Smart card device attached: %s", device);
                if (smartCardReaderManager.supports(device)) {
                    requestPermission(device);
                }
            }
        };
        BroadcastReceiver deviceDetachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = getUsbDevice(intent);
                Timber.log(Log.DEBUG, "Smart card device detached: %s", device);
                if (currentDevice != null && currentDevice.getDeviceId() == device.getDeviceId()) {
                    clearCurrent();
                    emitter.onNext(Optional.absent());
                }
            }
        };
        BroadcastReceiver devicePermissionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean permissionGranted = intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED, false);
                UsbDevice device = getUsbDevice(intent);
                Timber.log(Log.DEBUG, "Smart card device permission: granted: %s; device: %s", permissionGranted,
                        device);
                if (permissionGranted && smartCardReaderManager.supports(device)) {
                    clearCurrent();
                    currentDevice = device;
                    currentReader = smartCardReaderManager.reader(device);
                    emitter.onNext(Optional.fromNullable(currentReader));
                }
            }
        };

        context.registerReceiver(deviceAttachReceiver,
                new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        context.registerReceiver(deviceDetachReceiver,
                new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        context.registerReceiver(devicePermissionReceiver,
                new IntentFilter(ACTION_USB_DEVICE_PERMISSION));

        emitter.setCancellable(() -> {
            context.unregisterReceiver(deviceAttachReceiver);
            context.unregisterReceiver(deviceDetachReceiver);
            context.unregisterReceiver(devicePermissionReceiver);
            clearCurrent();
        });

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (smartCardReaderManager.supports(device)) {
                requestPermission(device);
                break;
            }
        }
    }

    private void requestPermission(UsbDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            usbManager.requestPermission(device,
                    PendingIntent
                            .getBroadcast(context, 0, new Intent(ACTION_USB_DEVICE_PERMISSION), PendingIntent.FLAG_MUTABLE));
        } else {
            usbManager.requestPermission(device,
                    PendingIntent
                            .getBroadcast(context, 0, new Intent(ACTION_USB_DEVICE_PERMISSION), 0));
        }
    }

    private void clearCurrent() {
        currentDevice = null;
        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (Exception e) {
                Timber.log(Log.ERROR, e, "Closing current reader %s", currentReader);
            }
            currentReader = null;
        }
    }

    private UsbDevice getUsbDevice(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
        } else {
            return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        }
    }
}
