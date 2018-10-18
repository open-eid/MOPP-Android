package ee.ria.DigiDoc.sign;

import android.content.Context;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.libdigidocpp.Conf;
import ee.ria.libdigidocpp.digidoc;
import timber.log.Timber;

public final class SignLib {

    /**
     * Sub-directory name in {@link Context#getCacheDir() cache dir} for schema.
     */
    private static final String SCHEMA_DIR = "schema";

    /**
     * Initialize sign-lib.
     *
     * Unzips the schema, access certificate and initializes libdigidocpp.
     */
    public static void init(Context context) {
        initNativeLibs();
        try {
            initSchema(context);
        } catch (IOException e) {
            Timber.e(e, "Init schema failed");
        }
        initLibDigiDocpp(context);
    }

    public static String accessTokenPass() {
        return Objects.requireNonNull(Conf.instance()).PKCS12Pass();
    }

    public static String accessTokenPath() {
        return Objects.requireNonNull(Conf.instance()).PKCS12Cert();
    }

    public static String libdigidocppVersion() {
        return digidoc.version();
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

    private static void initLibDigiDocpp(Context context) {
        digidoc.initializeLib("libdigidoc Android", getSchemaDir(context).getAbsolutePath());
    }

    private static File getSchemaDir(Context context) {
        File schemaDir = new File(context.getCacheDir(), SCHEMA_DIR);
        //noinspection ResultOfMethodCallIgnored
        schemaDir.mkdirs();
        return schemaDir;
    }

    private SignLib() {}
}
