package ee.ria.DigiDoc.sign;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import com.google.common.io.ByteStreams;

import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.libdigidocpp.Conf;
import ee.ria.libdigidocpp.DigiDocConf;
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
    public static void init(Context context, String tsaUrlPreferenceKey, ConfigurationProvider configurationProvider, String userAgent) {
        initNativeLibs();
        try {
            initSchema(context);
        } catch (IOException e) {
            Timber.e(e, "Init schema failed");
        }

        initLibDigiDocpp(context, tsaUrlPreferenceKey, configurationProvider, userAgent);

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
                if (!isChild(schemaDir, entryFile)) {
                    throw new ZipException("Bad zip entry: " + entry.getName());
                }
                FileOutputStream outputStream = new FileOutputStream(entryFile);
                ByteStreams.copy(inputStream, outputStream);
                outputStream.close();
            }
        }
    }

    private static void initLibDigiDocpp(Context context, String tsaUrlPreferenceKey, ConfigurationProvider configurationProvider, String userAgent) {
        String path = getSchemaDir(context).getAbsolutePath();
        try {
            Os.setenv("HOME", path, true);
        } catch (ErrnoException e) {
            Timber.e(e, "Setting HOME environment variable failed");
        }

        initLibDigiDocConfiguration(context, tsaUrlPreferenceKey, configurationProvider);
        digidoc.initializeLib(userAgent, path);
    }

    private static void initLibDigiDocLogging(Context context) {
        final File logDirectory = new File(context.getFilesDir() + "/logs");
        if (!logDirectory.exists()) {
            boolean isDirCreated = logDirectory.mkdir();
            if (isDirCreated) {
                Timber.d("Directories created for %s", logDirectory.getPath());
            }
        }
        DigiDocConf.instance().setLogLevel(4);
        DigiDocConf.instance().setLogFile(logDirectory.getAbsolutePath() + File.separator + "libdigidocpp.log");
    }

    private static void initLibDigiDocConfiguration(Context context, String tsaUrlPreferenceKey, ConfigurationProvider configurationProvider) {
        DigiDocConf conf = new DigiDocConf(getSchemaDir(context).getAbsolutePath());
        Conf.init(conf.transfer());
        if (BuildConfig.BUILD_TYPE.contentEquals("debug")) {
            initLibDigiDocLogging(context);
        }

        forcePKCS12Certificate();
        overrideTSLUrl(configurationProvider.getTslUrl());
        overrideTSLCert(configurationProvider.getTslCerts());
        overrideSignatureValidationServiceUrl(configurationProvider.getSivaUrl());
        overrideOCSPUrls(configurationProvider.getOCSPUrls());
        overrideVerifyServiceCert(configurationProvider.getCertBundle());
        initTsaUrl(context, tsaUrlPreferenceKey, configurationProvider.getTsaUrl());
    }

    private static void forcePKCS12Certificate() {
        DigiDocConf.instance().setPKCS12Cert("798.p12");
    }

    private static void overrideTSLUrl(String TSLUrl) {
        DigiDocConf.instance().setTSLUrl(TSLUrl);
    }

    private static void overrideTSLCert(List<String> tslCerts) {
        DigiDocConf.instance().setTSLCert(new byte[0]); // Clear existing TSL certificates list
        for (String tslCert : tslCerts) {
            DigiDocConf.instance().addTSLCert(Base64.decode(tslCert));
        }
    }

    private static void overrideVerifyServiceCert(List<String> certBundle) {
        for (String cert : certBundle) {
            DigiDocConf.instance().addVerifyServiceCert(Base64.decode(cert));
        }
    }

    private static void overrideSignatureValidationServiceUrl(String sivaUrl) {
        DigiDocConf.instance().setVerifyServiceUri(sivaUrl);
//        DigiDocConf.instance().setVerifyServiceCert(new byte[0]);
    }

    private static void overrideOCSPUrls(Map<String, String> ocspUrls) {
        ee.ria.libdigidocpp.StringMap stringMap = new ee.ria.libdigidocpp.StringMap();
        for (Map.Entry<String, String> entry : ocspUrls.entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue());
        }
        DigiDocConf.instance().setOCSPUrls(stringMap);
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
        boolean isDirsCreated = schemaDir.mkdirs();
        if (isDirsCreated) {
            Timber.d("Directories created for %s", schemaDir.getPath());
        }
        return schemaDir;
    }

    private static boolean isChild(File parent, File potentialChild) {
        try {
            String destDirCanonicalPath = parent.getCanonicalPath();
            String potentialChildCanonicalPath = potentialChild.getCanonicalPath();
            return potentialChildCanonicalPath.startsWith(destDirCanonicalPath);
        } catch (IOException e) {
            return false;
        }
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
                DigiDocConf.instance().setTSUrl(sharedPreferences.getString(key, defaultValue));
            }
        }
    }
}
