package ee.ria.DigiDoc.android.main.diagnostics;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.BaseMviViewModel;

public final class DiagnosticsViewModel extends BaseMviViewModel<Intent, ViewState, Intent, Result> {

    @Inject DiagnosticsViewModel(Processor processor) {
        super(processor);
    }

    @Override
    protected Class<? extends Intent> initialIntentType() {
        return Intent.InitialIntent.class;
    }

    @Override
    protected Intent action(Intent intent) {
        if (intent instanceof Intent.DiagnosticsSaveIntent) {
            Intent.DiagnosticsSaveIntent diagnosticsSaveIntent =
                    (Intent.DiagnosticsSaveIntent) intent;
            return Intent.DiagnosticsSaveIntent.create(diagnosticsSaveIntent.diagnosticsFile());
        }
        return intent;
    }

    @Override
    protected ViewState initialViewState() {
        return ViewState.initial();
    }
}
