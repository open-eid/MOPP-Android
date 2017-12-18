package ee.ria.DigiDoc.android.signature.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

public final class SignatureCreateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject SignatureCreateViewModel(Processor processor, Navigator navigator) {
        super(processor, navigator);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Action actionFromIntent(Intent intent) {
        if (intent instanceof Intent.InitialIntent) {
            return Action.ChooseFilesAction.create();
        } else if (intent instanceof Intent.CreateContainerIntent) {
            return Action.CreateContainerAction
                    .create(((Intent.CreateContainerIntent) intent).fileStreams());
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
