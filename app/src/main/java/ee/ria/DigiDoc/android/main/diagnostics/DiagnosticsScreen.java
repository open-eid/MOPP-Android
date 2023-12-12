package ee.ria.DigiDoc.android.main.diagnostics;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class DiagnosticsScreen extends ConductorScreen implements MviView<Intent, ViewState> {

    private final ViewDisposables disposables = new ViewDisposables();
    private DiagnosticsViewModel viewModel;
    static final Subject<File> diagnosticsFileSaveClicksSubject = PublishSubject.create();
    static final Subject<File> diagnosticsFileLogsSaveClicksSubject = PublishSubject.create();

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());

        return super.onCreateView(inflater, container, savedViewState);
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        viewModel = ApplicationApp.component(context).navigator()
                .viewModel(getInstanceId(), DiagnosticsViewModel.class);
    }

    @Override
    protected void onContextUnavailable() {
        super.onContextUnavailable();
        viewModel = null;
    }

    public static DiagnosticsScreen create() {
        return new DiagnosticsScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public DiagnosticsScreen() {
        super(R.id.mainDiagnosticsScreen);
    }

    @Override
    protected View view(Context context) {
        return new DiagnosticsView(context);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.DiagnosticsSaveIntent> diagnosticsFileSaveIntent() {
        return diagnosticsFileSaveClicksSubject
                .map(Intent.DiagnosticsSaveIntent::create);
    }

    private Observable<Intent.DiagnosticsLogsSaveIntent> diagnosticsLogsFilesSaveIntent() {
        return diagnosticsFileLogsSaveClicksSubject
                .map(Intent.DiagnosticsLogsSaveIntent::create);
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), diagnosticsFileSaveIntent(),
                diagnosticsLogsFilesSaveIntent());
    }

    @Override
    public void render(ViewState state) {

    }
}
