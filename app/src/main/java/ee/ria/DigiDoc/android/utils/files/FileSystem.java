package ee.ria.DigiDoc.android.utils.files;

import android.app.Application;
import android.webkit.MimeTypeMap;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.net.MediaType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import javax.inject.Inject;

import static ee.ria.DigiDoc.android.Constants.DIR_SIGNATURE_CONTAINERS;

public final class FileSystem {

    private final File cacheDir;
    private final File signatureContainersDir;

    @Inject
    FileSystem(Application application) {
        this.cacheDir = application.getCacheDir();
        this.signatureContainersDir = new File(application.getFilesDir(), DIR_SIGNATURE_CONTAINERS);
    }

    /**
     * Add signature container to the file-system.
     *
     * Add increment number before the extension when file with the same name already exists.
     *
     * @see #generateSignatureContainerFile(String)
     *
     * @param fileStream Stream to write.
     * @return File that was eventually written into.
     * @throws IOException When file cannot be created for some reason.
     */
    public File addSignatureContainer(FileStream fileStream) throws IOException {
        File file = generateSignatureContainerFile(fileStream.displayName());
        try (
                InputStream inputStream = fileStream.source().openStream();
                OutputStream outputStream = new FileOutputStream(file)
        ) {
            ByteStreams.copy(inputStream, outputStream);
        }
        return file;
    }

    /**
     * Generate signature container file.
     *
     * Add increment number before the extension when file with the same name already exists.
     * Example "file_name (1).ext".
     *
     * @param name Name of the container file with extension.
     * @return File with absolute path pointing to generated name.
     * @throws IOException When something fails.
     */
    public File generateSignatureContainerFile(String name) throws IOException {
        File file = increaseCounterIfExists(new File(signatureContainersDir, name));
        Files.createParentDirs(file);
        return file;
    }

    /**
     * Add file stream to local cache.
     *
     * @param fileStream File stream to cache.
     * @return File with absolute path pointing to generated cache.
     * @throws IOException When something fails.
     */
    public File cache(FileStream fileStream) throws IOException {
        File file = getCacheFile(fileStream.displayName());
        try (
                InputStream inputStream = fileStream.source().openStream();
                OutputStream outputStream = new FileOutputStream(file)
        ) {
            ByteStreams.copy(inputStream, outputStream);
        }
        return file;
    }

    /**
     * Get path to a file in cache directory.
     *
     * @param name Name of the file
     * @return File with absolute path to file in cache directory.
     */
    public File getCacheFile(String name) {
        return new File(cacheDir, name);
    }

    /**
     * Get MIME type from file extension.
     *
     * @param file File to get the extension from.
     * @return MIME type of the file.
     */
    public String getMimeType(File file) {
        String mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(Files.getFileExtension(file.getName()));
        return mimeType == null ? MediaType.PLAIN_TEXT_UTF_8.toString() : mimeType;
    }

    private static File increaseCounterIfExists(File file) {
        File directory = file.getParentFile();
        String fileName = file.getName();
        String name = Files.getNameWithoutExtension(fileName);
        String ext = Files.getFileExtension(fileName);
        int i = 1;
        while (file.exists()) {
            file = new File(directory, String.format(Locale.US, "%s (%d).%s", name, i++, ext));
        }
        return file;
    }
}
