package ee.ria.DigiDoc.android.signature.update;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class SignatureUpdateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject
    SignatureUpdateViewModel(Processor processor) {
        super(processor);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Action actionFromIntent(Intent intent) {
        if (intent instanceof Intent.InitialIntent) {
            return Action.LoadContainerAction
                    .create(((Intent.InitialIntent) intent).containerFile());
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
