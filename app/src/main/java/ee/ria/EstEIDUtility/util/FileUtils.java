package ee.ria.EstEIDUtility.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import ee.ria.EstEIDUtility.domain.FileItem;
import ee.ria.libdigidocpp.Container;

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static String getKilobytes(long length) {
        double kilobytes = (length / 1024);
        return new DecimalFormat("##.##").format(kilobytes);
    }

    public static boolean containerExists(File filesDir, String fileName) {
        File bdocsPath = getBdocsPath(filesDir);
        File bdoc = new File(bdocsPath, fileName);
        return bdoc.exists();
    }

    public static Container getContainer(File containerFile) {
        if (containerFile.exists()) {
            return Container.open(containerFile.getAbsolutePath());
        }
        Log.d(TAG, "getContainer: " + containerFile.getAbsolutePath());
        return Container.create(containerFile.getAbsolutePath());
    }

    public static Container getContainer(String path, String containerName) {
        return getContainer(new File(path, containerName));
    }

    public static Container getContainer(File filesDir, String containerName) {
        File bdocsPath = getBdocsPath(filesDir);
        return getContainer(new File(bdocsPath, containerName));
    }

    public static FileItem resolveFileItemFromUri(Uri uri, ContentResolver contentResolver, String path) {
        try (InputStream input = contentResolver.openInputStream(uri)) {
            String fileName = FileUtils.resolveFileName(uri, contentResolver);
            File file = new File(path, fileName);
            OutputStream output = new FileOutputStream(file);
            IOUtils.copy(input, output);
            return new FileItem(fileName, file.getPath(), 1);
        } catch (IOException e) {
            Log.e(TAG, "resolveFileItemFromUri: ", e);
        }
        return null;
    }

    private static String resolveFileName(Uri uri, ContentResolver contentResolver) {
        String uriString = uri.toString();
        if (isContentUri(uriString)) {
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        } else if (isFileUri(uriString)) {
            return new File(uriString).getName();
        }
        return null;
    }

    public static File getSchemaPath(File filesDir) {
        return new File(filesDir, Constants.SCHEMA_DIRECTORY);
    }

    public static File getBdocsPath(File filesDir) {
        return new File(filesDir, Constants.CONTAINERS_DIRECTORY);
    }

    public static File getCachePath(File cacheDir) {
        return new File(cacheDir, Constants.CACHE_DIRECTORY);
    }

    public static File getBdocFile(File filesDir, String fileName) {
        File bdocsPath = getBdocsPath(filesDir);
        return new File(bdocsPath, fileName);
    }

    public static void clearCacheDir(File cacheDir) {
        File cachePath = getCachePath(cacheDir);
        if (cachePath.isDirectory()) {
            for (File child : cachePath.listFiles()) {
                child.delete();
            }
        }
    }

    private static boolean isContentUri(String uriString) {
        return uriString.startsWith("content://");
    }

    private static boolean isFileUri(String uriString) {
        return uriString.startsWith("file://");
    }

    public static File getFile(String path, String filename) {
        return new File(path, filename);
    }
}
