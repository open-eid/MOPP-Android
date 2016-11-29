package ee.ria.EstEIDUtility.util;


import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import ee.ria.EstEIDUtility.FileItem;
import ee.ria.libdigidocpp.Container;

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static String getKilobytes(long length) {
        double kilobytes = (length / 1024);
        return new DecimalFormat("##.##").format(kilobytes);
    }

    public static boolean fileExists(String path, String fileName) {
        File file = new File(path + "/" + fileName);
        return file.exists();
    }

    public static boolean bdocExists(File filesDir, String fileName) {
        File bdocsPath = getBdocsPath(filesDir);
        File bdoc = new File(bdocsPath, fileName);
        return bdoc.exists();
    }

    /*public static Container getContainer(String path, String bdocName) {
        File bdocFile = new File(path + "/" + bdocName);
        if (bdocFile.exists()) {
            return Container.open(path + "/" + bdocName);
        }
        Log.d(TAG, "getContainer: " + path + "/" + bdocName);
        return Container.create(path + "/" + bdocName);
    }*/

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
            int copy = IOUtils.copy(input, output);
            Log.d(TAG, "resolveFileItemFromUri: OUTPUT " + output);
            Log.d(TAG, "resolveFileItemFromUri: FILE " + file);
            Log.d(TAG, "resolveFileItemFromUri: FILE SPACE " + file.getTotalSpace());
            Log.d(TAG, "resolveFileItemFromUri: BYTES COPIED " + copy);
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

    //TODO: for testing only
    public static void removeAllFiles(File filesDir) {
        File bdocsPath = getBdocsPath(filesDir);
        deleteRecursive(bdocsPath);
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    //TODO: for testing only
    public static void showAllFiles(File filesDir) {
        File bdocsPath = getBdocsPath(filesDir);
        showRecursive(bdocsPath);

        File schemaPath = getSchemaPath(filesDir);
        showRecursive(schemaPath);
    }

    private static void showRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            Log.d(TAG, "showRecursive: \t" + fileOrDirectory.getAbsolutePath());
            for (File child : fileOrDirectory.listFiles()) {
                showRecursive(child);
            }
        } else {
            Log.d(TAG, "showRecursive: " + fileOrDirectory.getAbsolutePath());
        }
    }

}
