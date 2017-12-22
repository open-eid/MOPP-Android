package ee.ria.DigiDoc.android.signature.update;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;

public final class SignatureUpdateViewModel extends
        BaseMviViewModel<Intent, ViewState, Action, Result> {

    @Inject SignatureUpdateViewModel(Processor processor, Navigator navigator) {
        super(processor, navigator);
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
        } else if (intent instanceof Intent.DocumentsSelectionIntent) {
            return Action.DocumentsSelectionAction
                    .create(((Intent.DocumentsSelectionIntent) intent).documents());
        } else if (intent instanceof Intent.RemoveDocumentsIntent) {
            Intent.RemoveDocumentsIntent removeDocumentsIntent =
                    (Intent.RemoveDocumentsIntent) intent;
            return Action.RemoveDocumentsAction.create(removeDocumentsIntent.containerFile(),
                    removeDocumentsIntent.documents());
        } else if (intent instanceof Intent.SignatureListVisibilityIntent) {
            return Action.SignatureListVisibilityAction
                    .create(((Intent.SignatureListVisibilityIntent) intent).isVisible());
        } else if (intent instanceof Intent.SignatureRemoveSelectionIntent) {
            return Action.SignatureRemoveSelectionAction
                    .create(((Intent.SignatureRemoveSelectionIntent) intent).signature());
        } else if (intent instanceof Intent.SignatureRemoveIntent) {
            Intent.SignatureRemoveIntent signatureRemoveIntent =
                    (Intent.SignatureRemoveIntent) intent;
            return Action.SignatureRemoveAction.create(signatureRemoveIntent.containerFile(),
                    signatureRemoveIntent.signature());
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
