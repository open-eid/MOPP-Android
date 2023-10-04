package ee.ria.DigiDoc.sign;

import android.content.Context;
import android.content.SharedPreferences;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.common.io.ByteStreams;

import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import ee.ria.libdigidocpp.Conf;
import ee.ria.libdigidocpp.DigiDocConf;
import ee.ria.libdigidocpp.digidoc;
import timber.log.Timber;

public final class SignLib {

    /**
     * Sub-directory name in {@link Context#getCacheDir() cache dir} for schema.
     */
    private static final String SCHEMA_DIR = "schema";
    private static final int LIBDIGIDOCPP_LOG_LEVEL = 4; // Debug messages

    private static List<String> certBundle = new ArrayList<>();

    private static SharedPreferences.OnSharedPreferenceChangeListener tsaUrlChangeListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener tsCertChangeListener;

    /**
     * Initialize sign-lib.
     * <p>
     * Unzips the schema, access certificate and initializes libdigidocpp.
     */
    public static void init(Context context, String tsaUrlPreferenceKey, ConfigurationProvider configurationProvider, String userAgent, boolean isLoggingEnabled) {
        initNativeLibs();
        try {
            initSchema(context);
        } catch (IOException e) {
            Timber.log(Log.ERROR, e, "Init schema failed");
        }

        initLibDigiDocpp(context, tsaUrlPreferenceKey, configurationProvider, userAgent, isLoggingEnabled);

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
                if (!entryFile.toPath().normalize().startsWith(schemaDir.toPath()) ||
                        !isChild(schemaDir, entryFile)) {
                    throw new ZipException("Bad zip entry: " + entry.getName());
                }
                FileOutputStream outputStream = new FileOutputStream(entryFile);
                ByteStreams.copy(inputStream, outputStream);
                outputStream.close();
            }
        }
    }

    private static void initLibDigiDocpp(Context context, String tsaUrlPreferenceKey,
                                         ConfigurationProvider configurationProvider,
                                         String userAgent,
                                         boolean isLoggingEnabled) {
        String path = getSchemaDir(context).getAbsolutePath();
        try {
            Os.setenv("HOME", path, true);
        } catch (ErrnoException e) {
            Timber.log(Log.ERROR, e, "Setting HOME environment variable failed");
        }

        initLibDigiDocConfiguration(context, tsaUrlPreferenceKey, configurationProvider, isLoggingEnabled);
        digidoc.initializeLib(userAgent, path);
    }

    private static void initLibDigiDocLogging(Context context) {
        final File logDirectory = new File(context.getFilesDir() + "/logs");
        if (!logDirectory.exists()) {
            boolean isDirCreated = logDirectory.mkdir();
            if (isDirCreated) {
                Timber.log(Log.DEBUG, "Directories created for %s", logDirectory.getPath());
            }
        }
        DigiDocConf.instance().setLogLevel(LIBDIGIDOCPP_LOG_LEVEL);
        DigiDocConf.instance().setLogFile(logDirectory.getAbsolutePath() + File.separator + "libdigidocpp.log");
    }

    private static void initLibDigiDocConfiguration(Context context, String tsaUrlPreferenceKey,
                                                    ConfigurationProvider configurationProvider,
                                                    boolean isLoggingEnabled) {
        DigiDocConf conf = new DigiDocConf(getSchemaDir(context).getAbsolutePath());
        Conf.init(conf.transfer());
        if (isLoggingEnabled || BuildConfig.BUILD_TYPE.contentEquals("debug")) {
            initLibDigiDocLogging(context);
        }

        String tsaCertPreferenceKey = context.getResources().getString(R.string.main_settings_tsa_cert_key);
        certBundle = configurationProvider.getCertBundle();

        forcePKCS12Certificate();
        overrideTSLUrl(configurationProvider.getTslUrl());
        overrideTSLCert(configurationProvider.getTslCerts());
        overrideSignatureValidationServiceUrl(configurationProvider.getSivaUrl());
        overrideOCSPUrls(configurationProvider.getOCSPUrls());
        overrideTSCerts(configurationProvider.getCertBundle());
        overrideVerifyServiceCert(configurationProvider.getCertBundle());
        initTsaUrl(context, tsaUrlPreferenceKey, configurationProvider.getTsaUrl());
        initTsCert(context, tsaCertPreferenceKey, "",
                tsaUrlPreferenceKey, configurationProvider.getTsaUrl());
    }

    private static void forcePKCS12Certificate() {
        DigiDocConf.instance().setPKCS12Cert("798.p12");
    }

    private static void overrideTSCerts(List<String> certBundle, @Nullable String customTsCert) {
        DigiDocConf.instance().setTSCert(new byte[0]); // Clear existing TS certificates list
        for (String tsCert : certBundle) {
            DigiDocConf.instance().addTSCert(Base64.decode(tsCert));
        }

        if (customTsCert != null) {
            DigiDocConf.instance().addTSCert(Base64.decode(customTsCert));
        }
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

    private static void overrideTSCerts(List<String> certBundle) {
        DigiDocConf.instance().setTSCert(new byte[0]);
        for (String cert : certBundle) {
            DigiDocConf.instance().addTSCert(Base64.decode(cert));
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

    private static void initTsCert(Context context, String preferenceKey, String defaultValue,
                                   String tsaUrlPreferenceKey, String defaultTsaUrl) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (tsCertChangeListener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(tsCertChangeListener);
        }

        tsCertChangeListener = new TsCertChangeListener(context, preferenceKey, defaultValue,
                tsaUrlPreferenceKey, defaultTsaUrl);
        preferences.registerOnSharedPreferenceChangeListener(tsCertChangeListener);
        tsCertChangeListener.onSharedPreferenceChanged(preferences, preferenceKey);
    }

    private static File getSchemaDir(Context context) {
        File schemaDir = new File(context.getCacheDir(), SCHEMA_DIR);
        boolean isDirsCreated = schemaDir.mkdirs();
        if (isDirsCreated) {
            Timber.log(Log.DEBUG, "Directories created for %s", schemaDir.getPath());
        }
        return schemaDir;
    }

    private static boolean isChild(File parent, File potentialChild) {
        try {
            if (!potentialChild.toPath().normalize().startsWith(parent.toPath())) {
                throw new IOException("Invalid path: " + potentialChild.getCanonicalPath());
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String getCustomTSAFile(Context context, String fileName) {
        File tsaFile = FileUtil.getTSAFile(context, fileName);
        if (tsaFile != null) {
            String fileContents = FileUtils.readFileContent(tsaFile.getPath());
            return fileContents
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
        }
        return null;
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

    private static final class TsCertChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private final Context context;
        private final String preferenceKey;
        private final String defaultValue;
        private final String tsaUrlPreferenceKey;
        private final String defaultTsaUrl;

        TsCertChangeListener(Context context, String preferenceKey, String defaultValue,
                             String tsaUrlPreferenceKey, String defaultTsaUrl) {
            this.context = context;
            this.preferenceKey = preferenceKey;
            this.defaultValue = defaultValue;
            this.tsaUrlPreferenceKey = tsaUrlPreferenceKey;
            this.defaultTsaUrl = defaultTsaUrl;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (TextUtils.equals(key, preferenceKey)) {
                if (sharedPreferences.getString(tsaUrlPreferenceKey, defaultTsaUrl).equals(defaultTsaUrl)) {
                    overrideTSCerts(certBundle, null);
                } else {
                    overrideTSCerts(certBundle, getCustomTSAFile(context, sharedPreferences.getString(key, defaultValue)));
                }
            }
        }
    }
}
