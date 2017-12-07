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
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
