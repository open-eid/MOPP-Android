package ee.ria.DigiDoc.android.signature.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface Intent extends MviIntent {

    @AutoValue
    abstract class InitialIntent implements Intent {

        @Nullable abstract android.content.Intent intent();

        static InitialIntent create(@Nullable android.content.Intent intent) {
            return new AutoValue_Intent_InitialIntent(intent);
        }
    }
}
