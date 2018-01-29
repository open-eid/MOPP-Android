package ee.ria.mopplib;

import android.content.Context;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.libdigidocpp.digidoc;
import timber.log.Timber;

public final class MoppLib {

    /**
     * Sub-directory name in {@link Context#getCacheDir() cache dir} for schema.
     */
    private static final String SCHEMA_DIR = "schema";

    private static final String ACCESS_CERTIFICATE_NAME = "878252.p12";

    public static void init(Context context) {
        initNativeLibs();
        try {
            initSchema(context);
        } catch (IOException e) {
            Timber.e(e, "Init schema failed");
        }
        try {
            initAccessCertificate(context);
        } catch (IOException e) {
            Timber.e(e, "Failed to init access certificate");
        }
        initLibDigiDocpp(context);
    }

    private static void initNativeLibs() {
        System.loadLibrary("c++_shared");
        System.loadLibrary("digidoc_java");
    }

    private static void initSchema(Context context) throws IOException {
        File schemaDir = getSchemaDir(context);
        try (ZipInputStream inputStream = new ZipInputStream(context.getResources()
                .openRawResource(R.raw.schema))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                File entryFile = new File(schemaDir, entry.getName());
                FileOutputStream outputStream = new FileOutputStream(entryFile);
                ByteStreams.copy(inputStream, outputStream);
                outputStream.close();
            }
        }
    }

    private static void initAccessCertificate(Context context) throws IOException {
        File schemaDir = getSchemaDir(context);
        File accessCertificateFile = new File(schemaDir, ACCESS_CERTIFICATE_NAME);
        try (
                InputStream inputStream = context.getResources().openRawResource(R.raw.sk878252);
                FileOutputStream outputStream = new FileOutputStream(accessCertificateFile)
        ) {
            ByteStreams.copy(inputStream, outputStream);
        }
    }

    private static void initLibDigiDocpp(Context context) {
        digidoc.initializeLib("libdigidoc Android", getSchemaDir(context).getAbsolutePath());
    }

    public static File getSchemaDir(Context context) {
        File schemaDir = new File(context.getCacheDir(), SCHEMA_DIR);
        //noinspection ResultOfMethodCallIgnored
        schemaDir.mkdirs();
        return schemaDir;
    }

    private MoppLib() {}
}
