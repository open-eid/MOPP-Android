package ee.ria.DigiDoc.android.signature.list;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class SignatureListViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject SignatureListViewModel(Processor processor) {
        super(processor);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        return intent.action();
    }


    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
