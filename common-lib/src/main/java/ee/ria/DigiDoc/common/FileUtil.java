package ee.ria.DigiDoc.common;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.URLUtil;

import com.google.common.io.ByteSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    public static final String RESTRICTED_FILENAME_CHARACTERS_AS_STRING = "@%:^?[]\\'\"”’{}#&`\\\\~«»/´";
    public static final String RTL_CHARACTERS_AS_STRING = "" + '\u200E' + '\u200F' + '\u202E' + '\u202A' + '\u202B';
    public static final String RESTRICTED_FILENAME_CHARACTERS_AND_RTL_CHARACTERS_AS_STRING = RESTRICTED_FILENAME_CHARACTERS_AS_STRING + RTL_CHARACTERS_AS_STRING;
    public static final String DEFAULT_FILENAME = "newFile";
    private static final String ALLOWED_URL_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_,.:/%;+=@?&!()";

    /**
     * Check if file path is in cache directory
     *
     * @param file File to check
     * @return Boolean indicating if file is in the cache directory.
     */
    public static File getFileInDirectory(File file, File directory) throws IOException {
        if (!file.toPath().normalize().startsWith(directory.toPath())) {
            throw new IOException("Invalid path: " + file.getCanonicalPath());
        }

        return file;
    }

    /**
     * Get Smart-ID V2 file name
     *
     * @param file File to get name from
     * @return String with updated file name
     */
    public static String getSignDocumentFileName(File file) {
        String fullFileName = file.getName();
        String fileName = FilenameUtils.getBaseName(fullFileName);
        String fileExtension = FilenameUtils.getExtension(fullFileName);

        if (fileName.length() <= 6) {
            return fileName + "." + fileExtension;
        }

        return StringUtils.left(fileName, 3) +
                "..." +
                StringUtils.right(fileName, 3) +
                "." +
                fileExtension;
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

        String trimmed = input.trim();
        if (trimmed.startsWith(".")) {
            trimmed = DEFAULT_FILENAME + trimmed;
        }

        StringBuilder sb = new StringBuilder(trimmed.length());

        if (!URLUtil.isValidUrl(trimmed) && !isRawUrl(trimmed)) {
            for (int offset = 0; offset < trimmed.length(); offset++) {
                char c = trimmed.charAt(offset);

                if (RESTRICTED_FILENAME_CHARACTERS_AND_RTL_CHARACTERS_AS_STRING.indexOf(c) != -1) {
                    sb.append(replacement);
                } else {
                    sb.append(c);
                }
            }
        } else if (!isRawUrl(trimmed)) {
            return normalizeUri(Uri.parse(trimmed)).toString();
        }

        return !sb.toString().isEmpty() ?
                FilenameUtils.getName(FilenameUtils.normalize(sb.toString())) :
                FilenameUtils.normalize(trimmed);
    }

    public static Uri normalizeUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        return Uri.parse(normalizeText(uri.toString()));
    }

    public static String normalizeText(String text) {
        StringBuilder sb = new StringBuilder(text.length());

        for (int offset = 0; offset < text.length(); offset++) {
            int i = ALLOWED_URL_CHARACTERS.indexOf(text.charAt(offset));

            if (i == -1) {
                sb.append("");
            }
            else {
                // Coverity does not want to see usages of the original string
                sb.append(ALLOWED_URL_CHARACTERS.charAt(i));
            }
        }

        return sb.toString();
    }

    public static String normalizeString(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD);
    }

    public static Uri normalizePath(String filePath) {
        return Uri.parse(FilenameUtils.normalize(filePath));
    }

    public static boolean isPDF(File file) {
        try (ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_ONLY)) {
            // Try to render as PDF. Throws exception if not a PDF file.
            new PdfRenderer(parcelFileDescriptor);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Path renameFile(Path path, String fileNameWithExtension) {
        try {
            Files.deleteIfExists(path.resolveSibling(fileNameWithExtension));
            Path newFilePath = path.resolveSibling(fileNameWithExtension);
            Files.move(path, newFilePath);
            return newFilePath;
        } catch (IOException e) {
            return path;
        }
    }

    public static boolean logsExist(File logsDirectory) {
        if (logsDirectory.exists()) {
            File[] files = logsDirectory.listFiles();
            return files != null && files.length > 0;
        }
        return false;
    }

    public static File combineLogFiles(File logsDirectory, String diagnosticsLogsFileName) throws IOException {
        if (logsExist(logsDirectory)) {
            File[] files = logsDirectory.listFiles() != null ? logsDirectory.listFiles() : new File[]{};
            File combinedLogFile = new File(logsDirectory + File.separator + diagnosticsLogsFileName);
            if (combinedLogFile.exists()) {
                Files.delete(combinedLogFile.toPath());
            }
            if (files != null) {
                for (File file : files) {
                    String header = "\n\n" + "===== File: " + file.getName() + " =====" + "\n\n";
                    String fileString = header + FileUtils.readFileToString(file, Charset.defaultCharset());
                    FileUtils.write(combinedLogFile, fileString, Charset.defaultCharset(), true);
                }
            }
            return combinedLogFile;
        }
        throw new FileNotFoundException("Could not combine log files. Cannot find logs.");
    }

    public static File getLogsDirectory(Context context) {
        return new File(context.getFilesDir() + "/logs");
    }

    public static File getCertFile(Context context, String certName, String certFolder) {
        File savedCertFolder = new File(context.getFilesDir(), certFolder);

        File[] files = savedCertFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(certName)) {
                    return file;
                }
            }
        }
        return null;
    }

    public static boolean isCades(Context context, ByteSource byteSource, String fileName) {
        try {
            File tempContainerFilesDirectory = new File(context.getFilesDir(), "tempContainerFiles");
            createDirectoryIfNotExist(tempContainerFilesDirectory.toString());
            File containerFile = getFile(byteSource,
                    new File(tempContainerFilesDirectory, fileName).getPath());
            return isFileInZip(containerFile.getPath(), "p7s");
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isFileInZip(String zipFilePath, String fileNameToFind) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains(fileNameToFind)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static File getFile(ByteSource byteSource, String filePath) throws IOException {
        byte[] bytes = byteSource.read();

        File file = new File(filePath);
        com.google.common.io.Files.write(bytes, file);

        return file;
    }

    public static void createDirectoryIfNotExist(String directory) {
        File destinationDirectory = new File(directory);
        if (!destinationDirectory.exists()) {
            boolean isDirsCreated = destinationDirectory.mkdirs();
            if (isDirsCreated) {
                logMessage(Level.INFO, "Directories created for " + directory);
            }
        }
    }

    public static void logMessage(Level level, String message) {
        if (BuildConfig.DEBUG && logger.isLoggable(level)) {
            logger.log(level, message);
        }
    }

    private static boolean isRawUrl(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }

        return url.startsWith("raw:");
    }
}
