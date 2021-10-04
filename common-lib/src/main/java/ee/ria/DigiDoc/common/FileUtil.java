package ee.ria.DigiDoc.common;

import android.os.Build;
import android.webkit.URLUtil;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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

        String allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_,.:;&()";
        String allowedUrlCharacters = allowedCharacters + "!+=@?%/";

        String characterSet = allowedCharacters;

        if (URLUtil.isValidUrl(input)) {
            characterSet = allowedUrlCharacters;
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
