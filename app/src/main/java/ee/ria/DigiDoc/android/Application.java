/*
 * Copyright 2021 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.android;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.StrictMode;
import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Date;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateViewModel;
import ee.ria.DigiDoc.android.eid.EIDHomeViewModel;
import ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsView;
import ee.ria.DigiDoc.android.main.home.HomeViewModel;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateViewModel;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.data.source.FileSystemSignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.list.SignatureListViewModel;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.android.utils.LocaleService;
import ee.ria.DigiDoc.android.utils.TSLUtil;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorNavigator;
import ee.ria.DigiDoc.configuration.util.UserAgentUtil;
import ee.ria.DigiDoc.configuration.ConfigurationConstants;
import ee.ria.DigiDoc.configuration.ConfigurationManager;
import ee.ria.DigiDoc.configuration.ConfigurationManagerService;
import ee.ria.DigiDoc.configuration.ConfigurationProperties;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import ee.ria.DigiDoc.crypto.RecipientRepository;
import ee.ria.DigiDoc.sign.SignLib;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderManager;
import ee.ria.DigiDoc.smartcardreader.acs.AcsSmartCardReader;
import ee.ria.DigiDoc.smartcardreader.identiv.IdentivSmartCardReader;
import io.reactivex.plugins.RxJavaPlugins;
import timber.log.Timber;

public class Application extends android.app.Application {

    private static ConfigurationProvider configurationProvider;

    @Override
    public void onCreate() {
        setupTSLFiles();
        setupStrictMode();
        super.onCreate();
        setupBouncyCastle();
        setupTimber();
        setupThreeTenAbp();
        setupConfiguration();
        setupRxJava();
        setupDagger();
    }

    // Copy every TSL file from APKs assets into cache if non-existent
    private void setupTSLFiles() {
        String destination = getCacheDir().toString() + "/schema";
        String assetsPath = "tslFiles";
        String[] tslFiles = null;
        try {
            tslFiles = getAssets().list(assetsPath);
        } catch (IOException e) {
            Timber.e(e, "Failed to get folder list: %s", assetsPath);
        }

        if (tslFiles != null && tslFiles.length > 0) {
            FileUtils.createDirectoryIfNotExist(destination);
            for (String fileName : tslFiles) {
                if (shouldCopyTSL(assetsPath, fileName, destination)) {
                    copyTSLFromAssets(assetsPath, fileName, destination);
                    removeExistingETag(destination + File.separator + fileName);
                }
            }
        }
    }

    private boolean shouldCopyTSL(String sourcePath, String fileName, String destionationDir) {
        if (!FileUtils.fileExists(destionationDir + File.separator + fileName)) {
            return true;
        } else {
            try (
                    InputStream assetsTSLInputStream = getAssets().open(sourcePath + File.separator + fileName);
                    InputStream cachedTSLInputStream = new FileInputStream(destionationDir + File.separator + fileName)
            ) {
                Integer assetsTslVersion = TSLUtil.readSequenceNumber(assetsTSLInputStream);
                Integer cachedTslVersion = TSLUtil.readSequenceNumber(cachedTSLInputStream);
                return assetsTslVersion != null && assetsTslVersion > cachedTslVersion;
            } catch (Exception e) {
                String message = "Error comparing sequence number between assets and cached TSLs";
                Timber.e(e, message);
                return false;
            }
        }
    }

    private void copyTSLFromAssets(String sourcePath, String fileName, String destionationDir) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(sourcePath + File.separator + fileName),
                StandardCharsets.UTF_8))) {
            FileUtils.writeToFile(reader, destionationDir, fileName);
        } catch (IOException ex) {
            Timber.e(ex, "Failed to copy file: %s from assets", fileName);
        }
    }

    private void removeExistingETag(String filePath) {
        String eTagPath = filePath + ".etag";
        FileUtils.removeFile(eTagPath);
    }

    // StrictMode

    private void setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
    }

    // BouncyCastle Security provider

    private void setupBouncyCastle() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());
    }

    // Timber

    private void setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }
        // TODO error reporting
    }

    // ThreeTenAbp

    private void setupThreeTenAbp() {
        AndroidThreeTen.init(this);
    }

    // Container configuration

    private void setupSignLib() {
        SignLib.init(this, getString(R.string.main_settings_tsa_url_key), getConfigurationProvider(), UserAgentUtil.getUserAgent(getApplicationContext()));
    }

    private void setupRxJava() {
        RxJavaPlugins.setErrorHandler(throwable -> Timber.e(throwable, "RxJava error handler"));
    }

    private void setupConfiguration() {
        CachedConfigurationHandler cachedConfHandler = new CachedConfigurationHandler(getCacheDir());
        ConfigurationProperties confProperties = new ConfigurationProperties(getAssets());
        ConfigurationManager confManager = new ConfigurationManager(this, confProperties, cachedConfHandler, UserAgentUtil.getUserAgent(getApplicationContext()));

        // Initially load cached configuration (if it exists and default configuration is not newer) in a blocking (synchronous)
        // manner. If default conf is newer then load default conf (case where application was updated and cache was not removed,
        // then in new packaged APK the default might be newer than the previously cached conf).
        // Initial conf is load synchronously so there would be no state where asynchronous central configuration loading timed
        // out or not ready yet and application features not working due to missing configuration.
        if (cachedConfHandler.doesCachedConfigurationExist() && !isDefaultConfNewerThanCachedConf(confProperties, cachedConfHandler)) {
            configurationProvider = confManager.forceLoadCachedConfiguration();
        } else {
            configurationProvider = confManager.forceLoadDefaultConfiguration();
        }

        Bundle bundle = new Bundle();
        bundle.putParcelable(ConfigurationConstants.CONFIGURATION_PROVIDER, configurationProvider);
        ConfigurationProviderReceiver confProviderReceiver = new ConfigurationProviderReceiver(new Handler());
        confProviderReceiver.send(ConfigurationManagerService.NEW_CONFIGURATION_LOADED, bundle);

        // Load configuration again in asynchronous manner, from central if needed or cache if present.
        initAsyncConfigurationLoad(new ConfigurationProviderReceiver(new Handler()), false);
    }

    private boolean isDefaultConfNewerThanCachedConf(ConfigurationProperties confProperties, CachedConfigurationHandler cachedConfHandler) {
        int defaultConfVersion = confProperties.getConfigurationVersionSerial();
        Integer cachedConfVersion = cachedConfHandler.getConfigurationVersionSerial();
        return cachedConfVersion == null || defaultConfVersion > cachedConfVersion;
    }

    // Following configuration updating should be asynchronous
    public void updateConfiguration(DiagnosticsView diagnosticsView) {
        ConfigurationProviderReceiver confProviderReceiver = new ConfigurationProviderReceiver(new Handler());
        confProviderReceiver.setDiagnosticView(diagnosticsView);
        initAsyncConfigurationLoad(confProviderReceiver, true);
    }

    private void initAsyncConfigurationLoad(ConfigurationProviderReceiver confProviderReceiver, boolean forceLoadCentral) {
        Intent intent = new Intent(this, ConfigurationManagerService.class);
        intent.putExtra(ConfigurationConstants.CONFIGURATION_RESULT_RECEIVER, confProviderReceiver);
        intent.putExtra(ConfigurationConstants.FORCE_LOAD_CENTRAL_CONFIGURATION, forceLoadCentral);
        Date configurationUpdateDate = configurationProvider.getConfigurationUpdateDate();
        if (configurationUpdateDate != null) {
            intent.putExtra(ConfigurationConstants.LAST_CONFIGURATION_UPDATE, configurationUpdateDate.getTime());
        }
        ConfigurationManagerService.enqueueWork(this, intent);
    }

    public ConfigurationProvider getConfigurationProvider() {
        return configurationProvider;
    }

    public class ConfigurationProviderReceiver extends ResultReceiver {

        private DiagnosticsView diagnosticsView;

        ConfigurationProviderReceiver(Handler handler) {
            super(handler);
        }

        void setDiagnosticView(DiagnosticsView diagnosticView) {
            this.diagnosticsView = diagnosticView;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            configurationProvider = resultData.getParcelable(ConfigurationConstants.CONFIGURATION_PROVIDER);
            if (resultCode == ConfigurationManagerService.NEW_CONFIGURATION_LOADED) {
                setupSignLib();
            }
            if (diagnosticsView != null) {
                diagnosticsView.updateViewData(configurationProvider, resultCode);
            }
        }
    }

    // Dagger

    private ApplicationComponent component;

    private void setupDagger() {
        component = DaggerApplication_ApplicationComponent.builder()
                .application(this)
                .build();
    }

    public static ApplicationComponent component(Context context) {
        return ((Application) context.getApplicationContext()).component;
    }

    @Singleton
    @Component(modules = {
            AndroidModule.class,
            ApplicationModule.class,
            SmartCardModule.class,
            CryptoLibModule.class
    })
    public interface ApplicationComponent {

        Navigator navigator();

        Formatter formatter();

        Activity.RootScreenFactory rootScreenFactory();

        LocaleService localeService();

        SettingsDataStore settingsDataStore();

        @Component.Builder
        interface Builder {

            @BindsInstance Builder application(android.app.Application application);

            ApplicationComponent build();
        }
    }

    @Module
    static abstract class AndroidModule {

        @Provides
        static UsbManager usbManager(android.app.Application application) {
            return (UsbManager) application.getSystemService(Context.USB_SERVICE);
        }

        @Provides
        static ContentResolver contentResolver(android.app.Application application) {
            return application.getContentResolver();
        }
    }

    @Module
    static abstract class ApplicationModule {

        @Provides @Singleton
        static Navigator navigator(Activity.RootScreenFactory rootScreenFactory,
                                   ViewModelProvider.Factory viewModelFactory) {
            return new ConductorNavigator(rootScreenFactory, viewModelFactory);
        }

        @Provides @Singleton
        static ViewModelProvider.Factory viewModelFactory(
                Map<Class<?>, Provider<ViewModel>> viewModelProviders) {
            return new ViewModelFactory(viewModelProviders);
        }

        @SuppressWarnings("unused")
        @Binds abstract SignatureContainerDataSource signatureContainerDataSource(
                FileSystemSignatureContainerDataSource fileSystemSignatureContainerDataSource);

        @SuppressWarnings("unused")
        @Binds @IntoMap @ClassKey(HomeViewModel.class)
        abstract ViewModel mainHomeViewModel(HomeViewModel homeViewModel);

        @SuppressWarnings("unused")
        @Binds @IntoMap @ClassKey(SignatureListViewModel.class)
        abstract ViewModel signatureListViewModel(SignatureListViewModel viewModel);

        @SuppressWarnings("unused")
        @Binds @IntoMap @ClassKey(SignatureCreateViewModel.class)
        abstract ViewModel signatureCreateViewModel(SignatureCreateViewModel viewModel);

        @SuppressWarnings("unused")
        @Binds @IntoMap @ClassKey(SignatureUpdateViewModel.class)
        abstract ViewModel signatureUpdateModel(SignatureUpdateViewModel viewModel);

        @SuppressWarnings("unused")
        @Binds @IntoMap @ClassKey(CryptoCreateViewModel.class)
        abstract ViewModel cryptoCreateViewModel(CryptoCreateViewModel viewModel);

        @SuppressWarnings("unused")
        @Binds @IntoMap @ClassKey(EIDHomeViewModel.class)
        abstract ViewModel eidHomeViewModel(EIDHomeViewModel viewModel);
    }

    @Module
    static abstract class SmartCardModule {

        @Provides @Singleton
        static SmartCardReaderManager smartCardReaderManager(
                android.app.Application application, UsbManager usbManager,
                AcsSmartCardReader acsSmartCardReader,
                IdentivSmartCardReader identivSmartCardReader) {
            return new SmartCardReaderManager(application, usbManager,
                    ImmutableList.of(acsSmartCardReader, identivSmartCardReader));
        }

        @Provides @Singleton
        static AcsSmartCardReader acsSmartCardReader(UsbManager usbManager) {
            return new AcsSmartCardReader(usbManager);
        }

        @Provides @Singleton
        static IdentivSmartCardReader identivSmartCardReader(android.app.Application application,
                                                             UsbManager usbManager) {
            return new IdentivSmartCardReader(application, usbManager);
        }
    }

    @Module
    static abstract class CryptoLibModule {

        @Provides @Singleton
        static RecipientRepository recipientRepository() {
            return new RecipientRepository(configurationProvider.getLdapPersonUrl(), configurationProvider.getLdapCorpUrl());
        }
    }

    static final class ViewModelFactory implements ViewModelProvider.Factory {

        private final Map<Class<?>, Provider<ViewModel>> viewModelProviders;

        ViewModelFactory(Map<Class<?>, Provider<ViewModel>> viewModelProviders) {
            this.viewModelProviders = viewModelProviders;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) viewModelProviders.get(modelClass).get();
        }
    }
}
