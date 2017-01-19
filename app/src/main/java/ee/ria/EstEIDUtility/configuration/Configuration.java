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

package ee.ria.EstEIDUtility.configuration;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.digidoc;

public class Configuration {

    private static final String TAG = Configuration.class.getName();

    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("xerces-c-3.1");
        System.loadLibrary("xalanMsg");
        System.loadLibrary("xalan-c");
        System.loadLibrary("xml-security-c");
        System.loadLibrary("digidoc_java");
    }

    public static void init(Context context) {
        createDirectoriesIfNotCreated(context);
        unpackSchema(context);
        initLibDigidoc(context);
    }

    private static void createDirectoriesIfNotCreated(Context context) {
        createDirectory(FileUtils.getDataFilesCacheDirectory(context));
        createDirectory(FileUtils.getContainerCacheDirectory(context));
        createDirectory(FileUtils.getContainersDirectory(context));
        createDirectory(FileUtils.getSchemaCacheDirectory(context));
    }

    private static void unpackSchema(Context context) {
        File schemaPath = FileUtils.getSchemaCacheDirectory(context);
        try (ZipInputStream zis = new ZipInputStream(context.getResources().openRawResource(R.raw.schema))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File entryFile = new File(schemaPath, ze.getName());
                FileOutputStream out = new FileOutputStream(entryFile);
                IOUtils.copy(zis, out);
                out.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "initLibraryConfiguration: ", e);
        }
    }

    private static void initLibDigidoc(Context context) {
        digidoc.initializeLib("libdigidoc Android", FileUtils.getSchemaCacheDirectory(context).getAbsolutePath());
    }

    private static void createDirectory(File dir) {
        if (!dir.exists()) {
            boolean mkdir = dir.mkdir();
            if (mkdir) {
                Log.d(TAG, "initLibraryConfiguration: created " + dir.getName() + " directory");
            }
        }
    }
}
