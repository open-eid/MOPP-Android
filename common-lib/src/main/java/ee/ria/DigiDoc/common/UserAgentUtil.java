package ee.ria.DigiDoc.common;

import static android.content.Context.USB_SERVICE;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class UserAgentUtil {

    private static final String LOG_TAG = "UserAgentUtil";

    private static final List<String> deviceNameFilters = List.of("Smart", "Reader", "Card");

    private UserAgentUtil() {}

    public static String getUserAgent(Context context, boolean shouldIncludeDevices) {
        ArrayList<String> deviceProductNames = new ArrayList<>();
        StringBuilder initializingMessage = new StringBuilder();

        if (context != null) {
            for (UsbDevice device : getConnectedUsbs(context)) {
                deviceProductNames.add(device.getProductName());
            }

            initializingMessage.append("riadigidoc/").append(getAppVersion(context));
            initializingMessage.append(" (Android ").append(Build.VERSION.RELEASE).append(")");
            initializingMessage.append(" Lang: ").append(Locale.getDefault().getLanguage());
            if (shouldIncludeDevices && !deviceProductNames.isEmpty()) {
                initializingMessage.append(" Devices: ").append(TextUtils.join(", ", deviceProductNames));
            }
        }

        return initializingMessage.toString();
    }

    private static List<UsbDevice> getConnectedUsbs(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(USB_SERVICE);
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();

        Map<String, UsbDevice> smartDevices = devices.entrySet()
                .stream()
                .filter(usbDeviceEntry -> {
                    UsbDevice usbDevice = usbDeviceEntry.getValue();
                    String productName = usbDevice.getProductName();
                    String deviceName = usbDevice.getDeviceName();

                    boolean productNameExists = productName != null && deviceNameFilters.stream().anyMatch(productName::contains);
                    boolean deviceNameExists = deviceNameFilters.stream().anyMatch(deviceName::contains);

                    return productNameExists || deviceNameExists;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ArrayList<>(smartDevices.values());
    }

    private static StringBuilder getAppVersion(Context context) {
        StringBuilder versionName = new StringBuilder();
        try {
            versionName.append(getPackageInfo(context).versionName)
                    .append(".")
                    .append(getPackageInfo(context).getLongVersionCode());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, String.format("Failed getting app version from package info. %s",
                    e.getLocalizedMessage()));
        }

        return versionName;
    }

    private static PackageInfo getPackageInfo(Context context) throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
        } else {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
        }
    }

}
