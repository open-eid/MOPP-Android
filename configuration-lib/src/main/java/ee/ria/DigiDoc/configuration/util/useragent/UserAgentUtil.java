package ee.ria.DigiDoc.configuration.util.useragent;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static android.content.Context.USB_SERVICE;

public final class UserAgentUtil {

    private final Context context;

    public UserAgentUtil(Context context) {
        this.context = context;
    }

    private static List<UsbDevice> getConnectedUsbs(Context context) {
        UsbManager usbManager = (UsbManager)context.getSystemService(USB_SERVICE);
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        Object[] devicesArray = devices.values().toArray();
        List<UsbDevice> usbDevices = new ArrayList<>();

        for (Object device : devicesArray) {
            usbDevices.add((UsbDevice) device);
        }

        return usbDevices;
    }

    private static StringBuilder getAppVersion(Context context) {
        StringBuilder versionName = new StringBuilder();
        try {
            versionName.append(context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName)
                    .append(".")
                    .append(context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "Getting package info");
        }

        return versionName;
    }

    public String getUserAgent() {

        ArrayList<String> devices = new ArrayList<>();
        StringBuilder initializingMessage = new StringBuilder();

        if (context != null) {
            for (UsbDevice device : getConnectedUsbs(context)) {
                devices.add(device.getProductName());
            }

            initializingMessage.append("libdigidoc/").append(getAppVersion(context));
            initializingMessage.append(" (Android ").append(Build.VERSION.RELEASE).append(")");
            initializingMessage.append(" Lang: ").append(Locale.getDefault().getLanguage());
            if (devices.size() > 0) {
                initializingMessage.append(" Devices: ").append(TextUtils.join(", ", devices));
            }
        }

        return initializingMessage.toString();
    }

}
