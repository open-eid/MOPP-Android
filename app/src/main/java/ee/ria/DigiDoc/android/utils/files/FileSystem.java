package ee.ria.DigiDoc.android.utils.files;

import android.app.Application;
import android.os.Environment;
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

    @Inject
    FileSystem(Application application) {
        this.cacheDir = application.getCacheDir();
    }

    /**
     * Add a file to the file-system.
     *
     * If the file already exists, add a counter and the end of the filename.
     *
     * @param fileStream Stream to write.
     * @return File that was eventually written into.
     * @throws IOException When file cannot be created for some reason.
     */
    public File add(FileStream fileStream) throws IOException {
        File to = generateSignatureContainerFile(fileStream.displayName());
        FileOutputStream outputStream = new FileOutputStream(to);
        ByteStreams.copy(fileStream.inputStream(), outputStream);
        fileStream.inputStream().close();
        outputStream.close();
        return to;
    }

    public File generateSignatureContainerFile(String name) throws IOException {
        File containers = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                DIR_SIGNATURE_CONTAINERS);
        File file = increaseCounterIfExists(new File(containers, name));
        Files.createParentDirs(file);
        return file;
    }

    public File cache(FileStream fileStream) throws IOException {
        File file = new File(cacheDir, fileStream.displayName());
        FileOutputStream outputStream = new FileOutputStream(file);
        ByteStreams.copy(fileStream.inputStream(), outputStream);
        fileStream.inputStream().close();
        outputStream.close();
        return file;
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

    public String getMimeType(File file) {
        return MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(Files.getFileExtension(file.getName()));
    }
}
