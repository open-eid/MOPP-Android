package ee.ria.DigiDoc.android.signature.update;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

public final class SignatureUpdateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    private final SettingsDataStore settingsDataStore;

    @Inject SignatureUpdateViewModel(Processor processor, Navigator navigator,
                                     SettingsDataStore settingsDataStore) {
        super(processor, navigator);
        this.settingsDataStore = settingsDataStore;
    }

    String getPhoneNo() {
        return settingsDataStore.getPhoneNo();
    }

    String getPersonalCode() {
        return settingsDataStore.getPersonalCode();
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
        } else if (intent instanceof Intent.AddDocumentsIntent) {
            Intent.AddDocumentsIntent addDocumentsIntent = (Intent.AddDocumentsIntent) intent;
            return Action.AddDocumentsAction.create(addDocumentsIntent.containerFile(),
                    addDocumentsIntent.fileStreams());
        } else if (intent instanceof Intent.OpenDocumentIntent) {
            Intent.OpenDocumentIntent openDocumentIntent = (Intent.OpenDocumentIntent) intent;
            return Action.OpenDocumentAction.create(openDocumentIntent.containerFile(),
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
            return Action.SignatureAddAction.create(signatureAddIntent.show(),
                    signatureAddIntent.containerFile(), signatureAddIntent.phoneNo(),
                    signatureAddIntent.personalCode(), signatureAddIntent.rememberMe());
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
