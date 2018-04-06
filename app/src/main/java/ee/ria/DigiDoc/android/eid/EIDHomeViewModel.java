package ee.ria.DigiDoc.android.eid;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class EIDHomeViewModel extends BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject EIDHomeViewModel(Processor processor) {
        super(processor);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Intent.InitialIntent) {
            return Action.LoadAction.create();
        } else {
            throw new IllegalArgumentException("Unknown intent " + intent);
        }
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
