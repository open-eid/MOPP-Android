package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.Nullable;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;

public final class HomeViewModel extends BaseMviViewModel<Intent, ViewState, Action, Result> {

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
    protected Action action(Intent intent) {
        if (intent instanceof Intent.NavigationIntent) {
            return Action.NavigationAction.create(((Intent.NavigationIntent) intent).item());
        } else if (intent instanceof Intent.MenuIntent) {
            Intent.MenuIntent menuIntent = (Intent.MenuIntent) intent;
            return Action.MenuAction.create(menuIntent.isOpen(), menuIntent.menuItem());
        } else if (intent instanceof Action) {
            return (Action) intent;
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
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
