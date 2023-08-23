package ee.ria.DigiDoc.android.main.settings.create;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.utils.mvi.MviViewState;

@AutoValue
public abstract class ViewState implements MviViewState {

    @Nullable abstract Context context();

    abstract Builder buildWith();

    static ViewState initial() {
        return new AutoValue_ViewState.Builder()
                .context(null)
                .build();
    }

    @AutoValue.Builder
    interface Builder {
        Builder context(Context context);
        ViewState build();
    }
}
