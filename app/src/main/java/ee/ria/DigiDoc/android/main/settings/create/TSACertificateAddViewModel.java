package ee.ria.DigiDoc.android.main.settings.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class TSACertificateAddViewModel extends BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject TSACertificateAddViewModel(Processor processor) {
        super(processor);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.ChooseFileIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Intent.ChooseFileIntent) {
            return Action.ChooseFileAction.create();
        } else {
            throw new IllegalArgumentException("Unknown intent " + intent);
        }
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}