package ee.ria.DigiDoc.android.signature.update;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class SignatureUpdateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    private final SettingsDataStore settingsDataStore;

    @Inject SignatureUpdateViewModel(Processor processor, SettingsDataStore settingsDataStore) {
        super(processor);
        this.settingsDataStore = settingsDataStore;
    }

    public String phoneNo() {
        return settingsDataStore.getPhoneNo();
    }

    public String personalCode() {
        return settingsDataStore.getPersonalCode();
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Action action(Intent intent) {
        if (intent instanceof Intent.InitialIntent) {
            Intent.InitialIntent initialIntent = (Intent.InitialIntent) intent;
            return Action.ContainerLoadAction
                    .create(initialIntent.containerFile(), initialIntent.signatureAddMethod(),
                            initialIntent.signatureAddSuccessMessageVisible());
        } else if (intent instanceof Intent.DocumentsAddIntent) {
            return Action.DocumentsAddAction
                    .create(((Intent.DocumentsAddIntent) intent).containerFile());
        } else if (intent instanceof Intent.DocumentOpenIntent) {
            Intent.DocumentOpenIntent openDocumentIntent = (Intent.DocumentOpenIntent) intent;
            return Action.DocumentOpenAction.create(openDocumentIntent.containerFile(),
                    openDocumentIntent.document());
        } else if (intent instanceof Intent.DocumentRemoveIntent) {
            Intent.DocumentRemoveIntent documentRemoveIntent = (Intent.DocumentRemoveIntent) intent;
            return Action.DocumentRemoveAction.create(documentRemoveIntent.showConfirmation(),
                    documentRemoveIntent.containerFile(), documentRemoveIntent.document());
        } else if (intent instanceof Intent.SignatureRemoveIntent) {
            Intent.SignatureRemoveIntent signatureRemoveIntent =
                    (Intent.SignatureRemoveIntent) intent;
            return Action.SignatureRemoveAction.create(signatureRemoveIntent.showConfirmation(),
                    signatureRemoveIntent.containerFile(), signatureRemoveIntent.signature());
        } else if (intent instanceof Intent.SignatureAddIntent) {
            Intent.SignatureAddIntent signatureAddIntent = (Intent.SignatureAddIntent) intent;
            return Action.SignatureAddAction.create(signatureAddIntent.method(),
                    signatureAddIntent.existingContainer(), signatureAddIntent.containerFile(),
                    signatureAddIntent.request());
        } else if (intent instanceof Intent.SendIntent) {
            return Action.SendAction.create(((Intent.SendIntent) intent).containerFile());
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
