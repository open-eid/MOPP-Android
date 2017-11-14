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

import javax.inject.Singleton;

import dagger.Binds;
import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;
import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.android.utils.conductor.ConductorNavigator;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.configuration.Configuration;
import timber.log.Timber;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        setupStrictMode();
        super.onCreate();
        setupTimber();
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
    }
}
