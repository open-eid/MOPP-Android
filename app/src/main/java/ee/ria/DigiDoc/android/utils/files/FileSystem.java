package ee.ria.DigiDoc.android.utils.files;

import android.app.Application;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Locale;

import javax.inject.Inject;

import ee.ria.DigiDoc.common.FileUtil;
import timber.log.Timber;

import static ee.ria.DigiDoc.android.Constants.DIR_SIGNATURE_CONTAINERS;

public final class FileSystem {

    private static final Comparator<File> FILE_MODIFIED_DATE_COMPARATOR = (o1, o2) -> {
        if (o1.lastModified() == o2.lastModified()) {
            return 0;
        }
        return o1.lastModified() > o2.lastModified() ? -1 : 1;
    };

    private static final String DATA_FILE_DIR = "%s-data-files";

    private final Application application;

    @Inject FileSystem(Application application) {
        this.application = application;
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
        File file = increaseCounterIfExists(new File(signatureContainersDir(), FilenameUtils.getName(name)));
        File fileInDirectory = FileUtil.getFileInDirectory(file, signatureContainersDir());
        Files.createParentDirs(fileInDirectory);
        return file;
    }

    /**
     * Get all signature container files currently cached on the device.
     *
     * The files are sorted by modified date.
     *
     * @return File objects for signature containers.
     */
    public ImmutableList<File> findSignatureContainerFiles() {
        File[] signatureContainerFileList = signatureContainersDir().listFiles();
        if (signatureContainerFileList != null) {
            return ImmutableList.sortedCopyOf(FILE_MODIFIED_DATE_COMPARATOR,
                    ImmutableList.copyOf(signatureContainerFileList));
        }

        return ImmutableList.copyOf(new File[]{});
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

    public File getContainerDataFilesDir(File containerFile) {
        File directory;
        if (containerFile.getParentFile().equals(signatureContainersDir())) {
            directory = createDataFileDirectory(cacheDir(), containerFile);
        } else {
            directory = createDataFileDirectory(containerFile.getParentFile(), containerFile);
        }
        return directory;
    }

    private File cacheDir() {
        return application.getCacheDir();
    }

    /**
     * Get path to a file in cache directory.
     *
     * @param name Name of the file
     * @return File with absolute path to file in cache directory.
     */
    private File getCacheFile(String name) throws IOException {
        File cacheFile = new File(cacheDir(), FilenameUtils.getName(name));
        return FileUtil.getFileInDirectory(cacheFile, cacheDir());
    }

    private File signatureContainersDir() {
        File dir = new File(application.getFilesDir(), DIR_SIGNATURE_CONTAINERS);
        boolean isDirsCreated = dir.mkdirs();
        if (isDirsCreated) {
            Timber.d("Directories created for %s", dir.getPath());
        }
        return dir;
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File createDataFileDirectory(File directory, File container) {
        File dir;
        int i = 0;
        while (true) {
            StringBuilder name = new StringBuilder(
                    String.format(Locale.US, DATA_FILE_DIR, container.getName()));
            if (i > 0) {
                name.append(i);
            }
            dir = new File(directory, name.toString());
            if (dir.isDirectory() || !dir.exists()) {
                break;
            }
            i++;
        }
        boolean isDirsCreated = dir.mkdirs();
        boolean isDirCreated = dir.mkdir();

        if (isDirsCreated || isDirCreated) {
            Timber.d("Directories created for %s", directory.getPath());
        }

        return dir;
    }
}
