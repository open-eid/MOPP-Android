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
        return Intent.InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Intent.InitialIntent) {
            return Action.ContainersLoadAction.create(true);
        } else if (intent instanceof Intent.UpButtonIntent) {
            return Action.NavigateUpAction.create();
        } else if (intent instanceof Intent.ContainerOpenIntent) {
            return Action.NavigateToContainerUpdateAction
                    .create(((Intent.ContainerOpenIntent) intent).containerFile());
        } else if (intent instanceof Intent.ContainerRemoveIntent) {
            Intent.ContainerRemoveIntent containerRemoveIntent =
                    (Intent.ContainerRemoveIntent) intent;
            return Action.ContainerRemoveAction.create(containerRemoveIntent.containerFile(),
                    containerRemoveIntent.confirmation());
        } else if (intent instanceof Intent.RefreshIntent) {
            return Action.ContainersLoadAction.create(false);
        } else {
            throw new IllegalArgumentException("Unknown intent " + intent);
        }
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
