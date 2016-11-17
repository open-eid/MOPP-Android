package ee.ria.EstEIDUtility.util;


import java.text.DecimalFormat;

public class FileUtils {

    public static String getKilobytes(long length) {
        double kilobytes = (length / 1024);
        return new DecimalFormat("##.##").format(kilobytes);
    }

}
