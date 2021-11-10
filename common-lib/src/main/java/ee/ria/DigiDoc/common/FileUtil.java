package ee.ria.DigiDoc.common;

import android.net.Uri;
import android.webkit.URLUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class FileUtil {

    public static final String RESTRICTED_FILENAME_CHARACTERS_AS_STRING = "@%:^?[]\\'\"”’{}#&`\\\\~«»/´";
    public static final String RTL_CHARACTERS_AS_STRING = "" + '\u200E' + '\u200F' + '\u202E' + '\u202A' + '\u202B';
    public static final String RESTRICTED_FILENAME_CHARACTERS_AND_RTL_CHARACTERS_AS_STRING = RESTRICTED_FILENAME_CHARACTERS_AS_STRING + RTL_CHARACTERS_AS_STRING;

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

        StringBuilder sb = new StringBuilder(input.length());

        if (!URLUtil.isValidUrl(input) && !isRawUrl(input)) {
            for (int offset = 0; offset < input.length(); offset++) {
                char c = input.charAt(offset);

                if (RESTRICTED_FILENAME_CHARACTERS_AND_RTL_CHARACTERS_AS_STRING.indexOf(c) != -1) {
                    sb.append(replacement);
                } else {
                    sb.append(c);
                }
            }
        } else if (!isRawUrl(input)) {
            return normalizeUri(Uri.parse(input)).toString();
        }

        return !sb.toString().equals("") ? FilenameUtils.normalize(sb.toString()) : FilenameUtils.normalize(input);
    }

    public static Uri normalizeUri(Uri uriString) {
        if (uriString == null) {
            return null;
        }

        String scheme = FilenameUtils.normalize(uriString.normalizeScheme().getScheme()) + "://";
        String uriWithoutScheme = FilenameUtils.normalize(uriString.toString().replaceFirst(scheme, ""));
        return Uri.parse(scheme + uriWithoutScheme);
    }

    public static Uri normalizePath(String filePath) {
        return Uri.parse(FilenameUtils.normalize(filePath));
    }

    private static boolean isRawUrl(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }

        return url.startsWith("raw:");
    }
}
