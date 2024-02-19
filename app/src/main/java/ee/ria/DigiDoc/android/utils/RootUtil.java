package ee.ria.DigiDoc.android.utils;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class RootUtil {

    public static boolean isDeviceRooted() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            try (DataOutputStream dos = new DataOutputStream(process.getOutputStream())) {
                dos.writeBytes("exit\n");
                dos.flush();
            }
            int exitValue = process.waitFor();
            boolean isRooted = exitValue == 0;
            Timber.log(Log.DEBUG, isRooted ? "Device is rooted" : "Device is not rooted");
            return isRooted;
        } catch (IOException | InterruptedException e) {
            Timber.log(Log.DEBUG, e, "Device is not rooted");
            return false;
        }
    }
}
