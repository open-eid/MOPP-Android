package ee.ria.DigiDoc.android.utils.files;

import android.app.Application;
import android.webkit.MimeTypeMap;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
        FileOutputStream outputStream = new FileOutputStream(file);
        ByteStreams.copy(fileStream.inputStream(), outputStream);
        fileStream.inputStream().close();
        outputStream.close();
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
        File file = new File(cacheDir, fileStream.displayName());
        FileOutputStream outputStream = new FileOutputStream(file);
        ByteStreams.copy(fileStream.inputStream(), outputStream);
        fileStream.inputStream().close();
        outputStream.close();
        return file;
    }

    /**
     * Get MIME type from file extension.
     *
     * @param file File to get the extension from.
     * @return MIME type of the file.
     */
    public String getMimeType(File file) {
        return MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(Files.getFileExtension(file.getName()));
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
