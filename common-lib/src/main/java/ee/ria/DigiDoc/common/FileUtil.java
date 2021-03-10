package ee.ria.DigiDoc.common;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class FileUtil {

    /**
     * Check if file path is in cache directory
     *
     * @param file File to check
     * @return Boolean indicating if file is in the cache directory.
     */
    public static File getFileInDirectory(File file, File directory) throws IOException {
        if (file.getCanonicalPath().startsWith(directory.getCanonicalPath())) {
            return file;
        }

        throw new IOException("Invalid file path");
    }

    /**
     * Replace invalid filename characters
     *
     * @param name Filename to replace invalid characters
     * @return String with replaced valid characters
     */
    public static String getValidFilename(String name) {
        String allowedSymbols = ".,-_";
        String validName = name.replaceAll("\\s", "_");
        for (char c = 0; c < validName.length(); c++) {
            if ((!Character.isLetterOrDigit(validName.charAt(c)) &&
                    !allowedSymbols.contains(Character.toString(validName.charAt(c))) &&
                    !(validName.charAt(c) >= 0x2190 && (validName.charAt(c) >= 0xD83D && validName.charAt(c) <= 0xDEFF)))) {
                validName = validName.replace(validName.charAt(c), '_');
            }
        }
        return validName;
    }
}
