package ee.ria.EstEIDUtility.util;

import android.content.ContentResolver;
import android.content.Context;
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

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static String getKilobytes(long length) {
        double kilobytes = (length / 1024);
        return new DecimalFormat("##.##").format(kilobytes);
    }

    public static File cacheUriAsDataFile(Context context, Uri uri, String fileName) {
        File directory = getDataFilesCacheDirectory(context);
        return writeUriDataToDirectory(directory, uri, fileName, context.getContentResolver());
    }

    public static File cacheUriAsDataFile(Context context, Uri uri) {
        String fileName = resolveFileName(uri, context.getContentResolver());
        return cacheUriAsDataFile(context, uri, fileName);
    }

    public static File cacheUriAsContainerFile(Context context, Uri uri, String fileName) {
        File directory = getContainerCacheDirectory(context);
        return writeUriDataToDirectory(directory, uri, fileName, context.getContentResolver());
    }

    public static File cacheUriAsContainerFile(Context context, Uri uri) {
        String fileName = resolveFileName(uri, context.getContentResolver());
        return cacheUriAsContainerFile(context, uri, fileName);
    }

    private static File writeUriDataToDirectory(File directory, Uri uri, String filename, ContentResolver contentResolver) {
        File destination = new File(directory, filename);
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            return writeToFile(destination, inputStream);
        } catch (Exception e) {
            Log.e(TAG, "failed to cache URI data", e);
            throw new FailedToCacheUriDataException(e);
        }
    }

    private static File writeToFile(File destinationFile, InputStream input) throws IOException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(destinationFile);
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        return destinationFile;
    }

    public static String resolveFileName(Uri uri, ContentResolver contentResolver) {
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

    public static File getContainerCacheDirectory(Context context) {
        return new File(getCacheDir(context), Constants.CONTAINERS_DIRECTORY);
    }

    public static File getDataFilesCacheDirectory(Context context) {
        return new File(getCacheDir(context), Constants.DATA_FILES_DIRECTORY);
    }

    public static File getSchemaCacheDirectory(Context context) {
        return new File(getCacheDir(context), Constants.SCHEMA_DIRECTORY);
    }

    public static File getContainersDirectory(Context context) {
        return new File(getFilesDir(context), Constants.CONTAINERS_DIRECTORY);
    }

    public static File getDataFilesDirectory(Context context) {
        return new File(getFilesDir(context), Constants.DATA_FILES_DIRECTORY);
    }

    public static File getSchemaDirectory(Context context) {
        return new File(getFilesDir(context), Constants.SCHEMA_DIRECTORY);
    }

    public static File getContainerFileFromCache(Context context, String fileName) {
        return new File(getContainerCacheDirectory(context), fileName);
    }

    public static File getDataFileFromCache(Context context, String fileName) {
        return new File(getDataFilesCacheDirectory(context), fileName);
    }

    public static File getContainerFile(Context context, String fileName) {
        return new File(getContainersDirectory(context), fileName);
    }

    public static File getDataFile(Context context, String fileName) {
        return new File(getDataFilesDirectory(context), fileName);
    }

    public static void clearDataFileCache(Context context) {
        clearDirectory(getDataFilesCacheDirectory(context));
    }

    public static void clearContainerCache(Context context) {
        clearDirectory(getContainerCacheDirectory(context));
    }

    private static void clearDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                child.delete();
            }
        }
    }

    private static File getCacheDir(Context context) {
        return context.getApplicationContext().getCacheDir();
    }

    private static File getFilesDir(Context context) {
        return context.getApplicationContext().getFilesDir();
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
