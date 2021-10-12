package ee.ria.DigiDoc.common;

import android.webkit.URLUtil;

import java.io.File;
import java.io.IOException;

public class FileUtil {

    private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_,.:;&()";
    private static final String ALLOWED_URL_CHARACTERS = ALLOWED_CHARACTERS + "!+=@?%/";

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
     * Replace invalid string characters
     *
     * @param input Input to replace invalid characters
     * @param replacement Replacement to replace invalid characters with
     * @return String with valid characters
     */
    public static String sanitizeString(String input, char replacement) {
        if (input == null) {
            return null;
        }

        String characterSet = ALLOWED_CHARACTERS;

        if (URLUtil.isValidUrl(input)) {
            characterSet = ALLOWED_URL_CHARACTERS;
        }

        StringBuilder sb = new StringBuilder(input.length());

        for (int offset = 0; offset < input.length(); offset++) {
            char c = input.charAt(offset);

            if (characterSet.indexOf(c) == -1) {
                sb.append(replacement);
            }
            else {
                // Coverity does not want to see usages of the original string
                sb.append(characterSet.charAt(characterSet.indexOf(c)));
            }
        }

        return sb.toString();
    }
}
