package ee.ria.DigiDoc.smartcardreader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
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
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Timber.d("Smart card device attached: %s", device);
                if (smartCardReaderManager.supports(device)) {
                    requestPermission(device);
                }
            }
        };
        BroadcastReceiver deviceDetachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Timber.d("Smart card device detached: %s", device);
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
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Timber.d("Smart card device permission: granted: %s; device: %s", permissionGranted,
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
        usbManager.requestPermission(device,
                PendingIntent
                        .getBroadcast(context, 0, new Intent(ACTION_USB_DEVICE_PERMISSION), 0));
    }

    private void clearCurrent() {
        currentDevice = null;
        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (Exception e) {
                Timber.e(e, "Closing current reader %s", currentReader);
            }
            currentReader = null;
        }
    }
}
