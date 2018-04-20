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
            return Action.LoadAction.create(false);
        } else if (intent instanceof Intent.LoadIntent) {
            return Action.LoadAction.create(true);
        } else if (intent instanceof Intent.CertificatesTitleClickIntent) {
            return Action.CertificatesTitleClickAction
                    .create(((Intent.CertificatesTitleClickIntent) intent).expand());
        } else if (intent instanceof Action) {
            return (Action) intent;
        } else {
            throw new IllegalArgumentException("Unknown intent " + intent);
        }
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
