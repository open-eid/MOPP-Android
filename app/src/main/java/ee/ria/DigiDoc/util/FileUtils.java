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

package ee.ria.DigiDoc.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.os.Environment;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class FileUtils {

    private static final File baseStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

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
        return new File(baseStorageDir, Constants.CONTAINERS_DIRECTORY);
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
            Timber.e(e, "failed to cache URI data");
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
                    Timber.d("clearDirectory() called with: directory = %s File %s deleted", directory.toString(), child.getName());
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
        File[] containerFilesInBaseDir = baseStorageDir.listFiles();
        File[] containerFilesInDigiDocDir = FileUtils.getContainersDirectory(context).listFiles();

        if (containerFilesInBaseDir == null && containerFilesInDigiDocDir == null) {
            return Collections.emptyList();
        }

        List<File> containers = new ArrayList<>();
        if (containerFilesInBaseDir != null) {
            Collections.addAll(containers, containerFilesInBaseDir);
        }
        if (containerFilesInDigiDocDir != null) {
            Collections.addAll(containers, containerFilesInDigiDocDir);
        }
        return containers;
    }

    public static boolean isContainer(String fileName) {
        return ContainerNameUtils.hasSupportedContainerExtension(fileName);
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
