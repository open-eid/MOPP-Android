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

import java.util.Map;

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
                UsbDevice device = getCurrentDevice();
                Timber.log(Log.DEBUG, "Smart card device attached: %s", device);
                handleUsbDevicesPermission();
            }
        };
        BroadcastReceiver deviceDetachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = getCurrentDevice();
                Timber.log(Log.DEBUG, "Smart card device detached: %s", device);
                if (device != null && currentDevice != null &&
                        currentDevice.getDeviceId() == device.getDeviceId()) {
                    clearCurrent();
                    emitter.onNext(Optional.absent());
                }
            }
        };
        BroadcastReceiver devicePermissionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intent.setPackage(context.getPackageName());
                UsbDevice device = getDeviceGrantedPermission();
                boolean permissionGranted = device != null;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(devicePermissionReceiver,
                    new IntentFilter(ACTION_USB_DEVICE_PERMISSION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(devicePermissionReceiver,
                    new IntentFilter(ACTION_USB_DEVICE_PERMISSION));
        }

        emitter.setCancellable(() -> {
            context.unregisterReceiver(deviceAttachReceiver);
            context.unregisterReceiver(deviceDetachReceiver);
            context.unregisterReceiver(devicePermissionReceiver);
            clearCurrent();
        });

        handleUsbDevicesPermission();
    }

    private void requestPermission(Context context, UsbDevice device) {
        Intent permissionIntent = new Intent(ACTION_USB_DEVICE_PERMISSION);
        permissionIntent.setPackage(context.getPackageName());
        usbManager.requestPermission(device,
                PendingIntent
                        .getBroadcast(context, 0, permissionIntent, PendingIntent.FLAG_IMMUTABLE));

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

    private UsbDevice getCurrentDevice() {
        Map<String, UsbDevice> deviceList = usbManager.getDeviceList();

        return deviceList.values().stream()
                .filter(this::isSupportedDevice)
                .findFirst()
                .orElse(null);
    }

    private UsbDevice getDeviceGrantedPermission() {
        Map<String, UsbDevice> deviceList = usbManager.getDeviceList();

        return deviceList.values().stream()
                .filter(this::isSupportedDevice)
                .filter(usbManager::hasPermission)
                .findFirst()
                .orElse(null);
    }

    private void handleUsbDevicesPermission() {
        Map<String, UsbDevice> deviceList = usbManager.getDeviceList();

        deviceList.values().stream()
                .filter(this::isSupportedDevice)
                .findFirst()
                .ifPresent(usbDevice -> requestPermission(context, usbDevice));
    }

    private boolean isSupportedDevice(UsbDevice device) {
        return smartCardReaderManager.supports(device);
    }
}
