package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

import static ee.ria.DigiDoc.android.utils.BundleUtils.getFile;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putFile;

public final class SignatureUpdateScreen extends ConductorScreen {

    private static final String CONTAINER_FILE = "containerFile";

    public static SignatureUpdateScreen create(File containerFile) {
        Bundle args = new Bundle();
        putFile(args, CONTAINER_FILE, containerFile);
        return new SignatureUpdateScreen(args);
    }

    private final File containerFile;

    @SuppressWarnings("WeakerAccess")
    public SignatureUpdateScreen(Bundle args) {
        super(R.id.signatureUpdateScreen, args);
        containerFile = getFile(args, CONTAINER_FILE);
    }

    @Override
    protected View createView(Context context) {
        return new SignatureUpdateView(context)
                .containerFile(containerFile);
    }
}
