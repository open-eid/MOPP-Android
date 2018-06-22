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

package ee.ria.DigiDoc.android;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.StrictMode;
import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableList;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
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
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateViewModel;
import ee.ria.DigiDoc.android.eid.EIDHomeViewModel;
import ee.ria.DigiDoc.android.main.home.HomeViewModel;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateViewModel;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.data.source.FileSystemSignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.list.SignatureListViewModel;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.android.utils.LocaleService;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorNavigator;
import ee.ria.cryptolib.RecipientRepository;
import ee.ria.mopplib.MoppLib;
import ee.ria.scardcomlibrary.SmartCardReaderManager;
import ee.ria.scardcomlibrary.acs.AcsSmartCardReader;
import ee.ria.scardcomlibrary.identiv.IdentivSmartCardReader;
import io.reactivex.plugins.RxJavaPlugins;
import timber.log.Timber;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        setupStrictMode();
        super.onCreate();
        setupBouncyCastle();
        setupTimber();
        setupThreeTenAbp();
        setupMoppLib();
        setupRxJava();
        setupDagger();
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
        }
        // TODO error reporting
    }

    // ThreeTenAbp

    private void setupThreeTenAbp() {
        AndroidThreeTen.init(this);
    }

    // Container configuration

    private void setupMoppLib() {
        MoppLib.init(this);
    }

    private void setupRxJava() {
        RxJavaPlugins.setErrorHandler(throwable -> Timber.e(throwable, "RxJava error handler"));
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
            return new RecipientRepository();
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
