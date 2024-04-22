package ee.ria.DigiDoc.android.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RootUtil {

    public static boolean isDeviceRooted() {
        String[] rootDirs = {"/sbin", "/su/bin", "/system/bin/su"};

        for (String dir : rootDirs) {
            Path rootPath = Paths.get(dir);
            if (Files.exists(rootPath)) {
                return true;
            }
        }

        return false;
    }
}
