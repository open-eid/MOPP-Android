package ee.ria.DigiDoc.android.main.diagnostics.source;

import java.io.File;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class FileSystemDiagnosticsDataSource implements DiagnosticsDataSource {

    @Inject
    FileSystemDiagnosticsDataSource() {}

    @Override
    public Single<File> get(File diagnosticsFile) {
        return Single.fromCallable(() -> diagnosticsFile);
    }
}
