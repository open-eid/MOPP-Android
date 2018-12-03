package ee.ria.DigiDoc.sign;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.libdigidocpp.Conf;
import ee.ria.libdigidocpp.XmlConfV3;
import ee.ria.libdigidocpp.digidoc;
import timber.log.Timber;

public final class SignLib {

    /**
     * Sub-directory name in {@link Context#getCacheDir() cache dir} for schema.
     */
    private static final String SCHEMA_DIR = "schema";

    private static SharedPreferences.OnSharedPreferenceChangeListener tsaUrlChangeListener;

    /**
     * Initialize sign-lib.
     * <p>
     * Unzips the schema, access certificate and initializes libdigidocpp.
     */
    public static void init(Context context, String tsaUrlPreferenceKey, String defaultTsaUrl) {
        initNativeLibs();
        try {
            initSchema(context);
        } catch (IOException e) {
            Timber.e(e, "Init schema failed");
        }
        initLibDigiDocpp(context);
        initTsaUrl(context, tsaUrlPreferenceKey, defaultTsaUrl);
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
        String path = getSchemaDir(context).getAbsolutePath();
        try {
            Os.setenv("HOME", path, true);
        } catch (ErrnoException e) {
            Timber.e(e, "Setting HOME environment variable failed");
        }
        digidoc.initializeLib("libdigidoc Android", path);
    }

    private static void initTsaUrl(Context context, String preferenceKey, String defaultValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (tsaUrlChangeListener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(tsaUrlChangeListener);
        }
        tsaUrlChangeListener = new TsaUrlChangeListener(preferenceKey, defaultValue);
        preferences.registerOnSharedPreferenceChangeListener(tsaUrlChangeListener);
        tsaUrlChangeListener.onSharedPreferenceChanged(preferences, preferenceKey);
    }

    private static File getSchemaDir(Context context) {
        File schemaDir = new File(context.getCacheDir(), SCHEMA_DIR);
        //noinspection ResultOfMethodCallIgnored
        schemaDir.mkdirs();
        return schemaDir;
    }

    private SignLib() {
    }

    private static final class TsaUrlChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private final String preferenceKey;
        private final String defaultValue;

        TsaUrlChangeListener(String preferenceKey, String defaultValue) {
            this.preferenceKey = preferenceKey;
            this.defaultValue = defaultValue;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (TextUtils.equals(key, preferenceKey)) {
                XmlConfV3.instance().setTSUrl(sharedPreferences.getString(key, defaultValue));
            }
        }
    }
}
