package ee.ria.DigiDoc.android.main.settings.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class CertificateAddViewModel extends BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject
    CertificateAddViewModel(Processor processor) {
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