package ee.ria.DigiDoc.sign;

import static ee.ria.DigiDoc.common.CommonConstants.DIR_SIVA_CERT;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_TSA_CERT;

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
    private static SharedPreferences.OnSharedPreferenceChangeListener sivaUrlChangeListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener sivaCertChangeListener;

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
        String sivaUrlPreferenceKey = context.getResources().getString(R.string.main_settings_siva_url_key);
        String sivaCertPreferenceKey = context.getResources().getString(R.string.main_settings_siva_cert_key);
        certBundle = configurationProvider.getCertBundle();

        forcePKCS12Certificate();
        overrideTSLUrl(configurationProvider.getTslUrl());
        overrideTSLCert(configurationProvider.getTslCerts());
        overrideSignatureValidationServiceUrl(context, sivaUrlPreferenceKey, configurationProvider.getSivaUrl());
        overrideOCSPUrls(configurationProvider.getOCSPUrls());
        overrideTSCerts(configurationProvider.getCertBundle());
        initTsaUrl(context, tsaUrlPreferenceKey, configurationProvider.getTsaUrl());
        initTsCert(context, tsaCertPreferenceKey, "",
                tsaUrlPreferenceKey, configurationProvider.getTsaUrl());
        initVerifyServiceCert(context, sivaCertPreferenceKey, configurationProvider.getCertBundle());
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

    private static void initVerifyServiceCert(Context context, String preferenceKey, List<String> certBundle) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sivaCertChangeListener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(sivaCertChangeListener);
        }

        sivaCertChangeListener = new SivaCertChangeListener(context, preferenceKey, certBundle);
        preferences.registerOnSharedPreferenceChangeListener(sivaCertChangeListener);
        sivaCertChangeListener.onSharedPreferenceChanged(preferences, preferenceKey);
    }

    private static void overrideVerifyServiceCert(List<String> certBundle, String customSivaCert) {
        DigiDocConf.instance().setVerifyServiceCert(new byte[0]);
        if (customSivaCert != null && !customSivaCert.isEmpty()) {
            DigiDocConf.instance().addVerifyServiceCert(Base64.decode(customSivaCert));
        }
        for (String cert : certBundle) {
            DigiDocConf.instance().addVerifyServiceCert(Base64.decode(cert));
        }
    }

    private static void overrideSignatureValidationServiceUrl(Context context, String preferenceKey, String defaultValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sivaUrlChangeListener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(sivaUrlChangeListener);
        }
        sivaUrlChangeListener = new SivaUrlChangeListener(preferenceKey, defaultValue);
        preferences.registerOnSharedPreferenceChangeListener(sivaUrlChangeListener);
        sivaUrlChangeListener.onSharedPreferenceChanged(preferences, preferenceKey);
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

    private static String getCustomCertFile(Context context, String fileName, String certFolder) {
        File certFile = FileUtil.getCertFile(context, fileName, certFolder);
        if (certFile != null) {
            String fileContents = FileUtils.readFileContent(certFile.getPath());
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
                    overrideTSCerts(certBundle, getCustomCertFile(context,
                        sharedPreferences.getString(key, defaultValue), DIR_TSA_CERT));
                }
            }
        }
    }

    private static final class SivaUrlChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private final String preferenceKey;
        private final String defaultValue;

        SivaUrlChangeListener(String preferenceKey, String defaultValue) {
            this.preferenceKey = preferenceKey;
            this.defaultValue = defaultValue;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (TextUtils.equals(key, preferenceKey)) {
                DigiDocConf.instance().setVerifyServiceUri(sharedPreferences.getString(key, defaultValue));
            }
        }
    }

    private static final class SivaCertChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private final Context context;
        private final String preferenceKey;
        private final List<String> defaultValues;

        SivaCertChangeListener(Context context, String preferenceKey, List<String> defaultValues) {
            this.context = context;
            this.preferenceKey = preferenceKey;
            this.defaultValues = defaultValues;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (TextUtils.equals(key, preferenceKey)) {
                overrideVerifyServiceCert(defaultValues, getCustomCertFile(context,
                        sharedPreferences.getString(key, ""), DIR_SIVA_CERT));
            }
        }
    }
}
