package ee.ria.EstEIDUtility.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
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

    public static boolean bdocExists(File filesDir, String fileName) {
        File bdocsPath = getBdocsPath(filesDir);
        File bdoc = new File(bdocsPath, fileName);
        return bdoc.exists();
    }

    public static Container getContainer(File filesDir, String bdocName) {
        File bdocsPath = getBdocsPath(filesDir);
        File bdocFile = new File(bdocsPath, bdocName);
        if (bdocFile.exists()) {
            return Container.open(bdocFile.getAbsolutePath());
        }
        Log.d(TAG, "getContainer: " + bdocFile.getAbsolutePath());
        return Container.create(bdocFile.getAbsolutePath());
    }

    public static FileItem resolveFileItemFromUri(Uri uri, ContentResolver contentResolver, String path) {
        try (InputStream input = contentResolver.openInputStream(uri)) {
            String fileName = FileUtils.resolveFileName(uri, contentResolver);
            File file = new File(path, fileName);
            OutputStream output = new FileOutputStream(file);
            IOUtils.copy(input, output);
            return new FileItem(fileName, file.getPath(), 1);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "resolveFileItemFromUri: ", e);
        } catch (IOException e) {
            Log.e(TAG, "resolveFileItemFromUri: ", e);
        }
        return null;
    }

    public static String resolveFileName(Uri uri, ContentResolver contentResolver) {
        String uriString = uri.toString();
        if (uriString.startsWith("content://")) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        } else if (uriString.startsWith("file://")) {
            return new File(uriString).getName();
        }
        return null;
    }

    public static File getSchemaPath(File filesDir) {
        return new File(filesDir, Constants.SCHEMA_PATH);
    }

    public static File getBdocsPath(File filesDir) {
        return new File(filesDir, Constants.BDOCS_PATH);
    }

    public static File getBdocsFilesPath(File filesDir) {
        File bdocsPath = getBdocsPath(filesDir);
        return new File(bdocsPath, Constants.BDOCS_FILES_PATH);
    }

    public static File getBdocFile(File filesDir, String bdocFileName) {
        File bdocsPath = getBdocsPath(filesDir);
        return new File(bdocsPath, bdocFileName);
    }

}
