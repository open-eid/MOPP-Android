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

import android.content.Context;
import android.os.StrictMode;

import com.jakewharton.threetenabp.AndroidThreeTen;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.BindsInstance;
import dagger.Component;
import dagger.MapKey;
import dagger.Module;
import dagger.multibindings.IntoMap;
import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateViewModel;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.data.source.FileSystemSignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.android.utils.conductor.ConductorNavigator;
import ee.ria.DigiDoc.android.utils.conductor.ConductorViewModelProvider;
import ee.ria.DigiDoc.android.utils.mvi.MviViewModel;
import ee.ria.DigiDoc.android.utils.mvi.MviViewModelProvider;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.configuration.Configuration;
import timber.log.Timber;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        setupStrictMode();
        super.onCreate();
        setupTimber();
        setupThreeTenAbp();
        setupContainerConfiguration();
        setupDagger();
    }

    // StrictMode

    private void setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
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

    private void setupContainerConfiguration() {
        Configuration.init(getApplicationContext());
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
    @Component(modules = ApplicationModule.class)
    public interface ApplicationComponent {

        void inject(Activity activity);

        Navigator navigator();

        Formatter formatter();

        MviViewModelProvider viewModelProvider();

        @Component.Builder
        interface Builder {

            @BindsInstance Builder application(android.app.Application application);

            ApplicationComponent build();
        }
    }

    @Module
    static abstract class ApplicationModule {

        @SuppressWarnings("unused")
        @Binds abstract Navigator navigator(ConductorNavigator conductorNavigator);

        @SuppressWarnings("unused")
        @Binds abstract SignatureContainerDataSource signatureContainerDataSource(
                FileSystemSignatureContainerDataSource fileSystemSignatureContainerDataSource);

        @SuppressWarnings("unused")
        @Binds abstract MviViewModelProvider viewModelProvider(
                ConductorViewModelProvider conductorViewModelProvider);

        @Binds @IntoMap @SuppressWarnings("unused")
        @ViewModelKey(SignatureCreateViewModel.class)
        abstract MviViewModel signatureCreateViewModel(SignatureCreateViewModel viewModel);

        @Binds @IntoMap @SuppressWarnings("unused")
        @ViewModelKey(SignatureUpdateViewModel.class)
        abstract MviViewModel signatureUpdateModel(SignatureUpdateViewModel viewModel);
    }

    @MapKey
    @interface ViewModelKey {
        Class<? extends MviViewModel> value();
    }
}
