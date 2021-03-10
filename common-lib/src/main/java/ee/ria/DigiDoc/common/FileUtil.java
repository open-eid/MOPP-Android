package ee.ria.DigiDoc.common;

import java.io.File;
import java.io.IOException;

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
        StringBuilder allowedFileName = new StringBuilder();
        for (char c = 0; c < name.length(); c++) {
            if (Character.isLetterOrDigit(name.charAt(c)) ||
                    allowedSymbols.contains(Character.toString(name.charAt(c))) ||
                    (name.charAt(c) >= 0x2190 && (name.charAt(c) >= 0xD83D && name.charAt(c) <= 0xDEFF))) {
                allowedFileName.append(name.charAt(c));
            } else if (Character.isWhitespace(name.charAt(c))) {
                allowedFileName.append("_");
            }
        }
        return allowedFileName.toString();
    }
}
