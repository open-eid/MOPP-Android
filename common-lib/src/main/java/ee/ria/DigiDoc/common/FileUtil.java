package ee.ria.DigiDoc.common;

import android.webkit.URLUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
    public static String sanitizeString(String input, String replacement) {
        if (input == null) {
            return null;
        }

        String restrictedCharacters = "@%:^?[]\\'\"”’{}#&`\\\\~«»/´";

        ArrayList<String> rtlChars = new ArrayList<>(
                Arrays.asList("\u200E", "\u200F", "\u202E", "\u202A", "\u202B"));

        StringBuilder sb = new StringBuilder(input.length());

        if (!URLUtil.isValidUrl(input) && !isRawUrl(input)) {
            for (int offset = 0; offset < input.length(); offset++) {
                char c = input.charAt(offset);

                if (rtlChars.contains(Character.toString(c)) || restrictedCharacters.indexOf(c) != -1) {
                    sb.append(replacement);
                } else {
                    sb.append(c);
                }
            }
        }

        return !sb.toString().equals("") ? sb.toString() : input;
    }

    private static boolean isRawUrl(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }

        return url.startsWith("raw:");
    }
}