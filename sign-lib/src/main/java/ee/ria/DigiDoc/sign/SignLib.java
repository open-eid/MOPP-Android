package ee.ria.DigiDoc.sign;

import android.content.Context;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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

    private static final ImmutableMap<Integer, String> ACCESS_CERTIFICATES =
            ImmutableMap.<Integer, String>builder()
                    .put(R.raw.sk878252, "878252.p12")
                    .put(R.raw.sk798, "798.p12")
                    .build();

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
        try {
            initAccessCertificates(context);
        } catch (IOException e) {
            Timber.e(e, "Failed to init access certificate");
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

    private static void initAccessCertificates(Context context) throws IOException {
        for (Map.Entry<Integer, String> certificate : ACCESS_CERTIFICATES.entrySet()) {
            try (
                    InputStream inputStream = context.getResources()
                            .openRawResource(certificate.getKey());
                    FileOutputStream outputStream = new FileOutputStream(
                            new File(getSchemaDir(context), certificate.getValue()))
            ) {
                ByteStreams.copy(inputStream, outputStream);
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
