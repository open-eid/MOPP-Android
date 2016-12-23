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

    private static final String TAG = "Configuration";

    static {
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
        createDirectory(FileUtils.getDataFilesDirectory(context));
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
        digidoc.initJava(FileUtils.getSchemaCacheDirectory(context).getAbsolutePath());
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
