package ee.ria.DigiDoc.android.main.home;

import androidx.annotation.Nullable;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;

public final class HomeViewModel extends BaseMviViewModel<Intent, ViewState, Intent, Result> {

    private final Processor processor;
    private final Navigator navigator;

    @Nullable private String eidScreenId;

    @Inject HomeViewModel(Processor processor, Navigator navigator) {
        super(processor);
        this.processor = processor;
        this.navigator = navigator;
    }

    void eidScreenId(@Nullable String eidScreenId) {
        this.eidScreenId = eidScreenId;
        processor.eidScreenId(eidScreenId);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Intent action(Intent intent) {
        return intent;
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (eidScreenId != null) {
            navigator.clearViewModel(eidScreenId);
        }
    }
}
