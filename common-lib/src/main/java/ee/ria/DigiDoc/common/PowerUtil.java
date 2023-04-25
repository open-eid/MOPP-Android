package ee.ria.DigiDoc.common;

import android.content.Context;
import android.os.PowerManager;

public class PowerUtil {

    public static boolean isPowerSavingMode(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }
}
