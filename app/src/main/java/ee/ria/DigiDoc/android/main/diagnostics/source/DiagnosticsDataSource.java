package ee.ria.DigiDoc.android.main.diagnostics.source;

import java.io.File;

import io.reactivex.rxjava3.core.Single;

public interface DiagnosticsDataSource {
    Single<File> get(File diagnosticsFile);
}