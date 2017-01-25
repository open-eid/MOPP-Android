/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.EstEIDUtility.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileUtils {

    private static final String TAG = FileUtils.class.getName();

    public static String getKilobytes(long length) {
        double kilobytes = (length / 1024);
        return new DecimalFormat("##.##").format(kilobytes);
    }

    public static File cacheUriAsDataFile(Context context, Uri uri) {
        String fileName = resolveFileName(uri, context.getContentResolver());
        return cacheUriAsDataFile(context, uri, fileName);
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

    public static void clearDataFileCache(Context context) {
        clearDirectory(getDataFilesCacheDirectory(context));
    }

    public static void clearContainerCache(Context context) {
        clearDirectory(getContainerCacheDirectory(context));
    }

    private static File cacheUriAsDataFile(Context context, Uri uri, String fileName) {
        File directory = getDataFilesCacheDirectory(context);
        return writeUriDataToDirectory(directory, uri, fileName, context.getContentResolver());
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

    private static void clearDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                boolean delete = child.delete();
                if (delete) {
                    Log.d(TAG, "clearDirectory() called with: directory = [" + directory + "]" + " File [" + child.getName() + "] deleted");
                }
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

    public static List<File> getContainers(Context context) {
        File[] containerFiles = FileUtils.getContainersDirectory(context).listFiles();
        if (containerFiles == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(containerFiles);
    }

    public static boolean isContainer(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return Arrays.asList("bdoc", "asice").contains(extension);
    }

    public static String resolveMimeType(String fileName) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
        if (mimeType == null) {
            if (isContainer(fileName)) {
                mimeType = "application/zip";
            }
        }
        return mimeType;
    }

    public static File uriAsContainerFile(Context context, Uri uri) {
        String fileName = resolveFileName(uri, context.getContentResolver());
        return uriAsContainerFile(context, uri, fileName);
    }

    private static File uriAsContainerFile(Context context, Uri uri, String fileName) {
        File directory = getContainerCacheDirectory(context);
        return writeUriDataToDirectory(directory, uri, fileName, context.getContentResolver());
    }

    public static File incrementFileName(File directory, String containerName) {
        File file = new File(directory, containerName);
        int num = 0;
        while(file.exists()) {
            num++;
            String fileName = containerName;
            fileName = FilenameUtils.removeExtension(fileName) + " (" + num + ")." + FilenameUtils.getExtension(fileName);
            file = new File(directory, fileName);
        }
        return file;
    }
}
