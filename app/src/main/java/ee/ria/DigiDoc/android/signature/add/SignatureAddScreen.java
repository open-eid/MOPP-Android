package ee.ria.DigiDoc.android.signature.add;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

import static ee.ria.DigiDoc.android.utils.BundleUtils.getFile;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putFile;

public class SignatureAddScreen extends ConductorScreen {

    private static final String CONTAINER_FILE = "containerFile";

    public static SignatureAddScreen create(File containerFile) {
        Bundle args = new Bundle();
        putFile(args, CONTAINER_FILE, containerFile);
        return new SignatureAddScreen(args);
    }

    private final File containerFile;

    @SuppressWarnings("WeakerAccess")
    public SignatureAddScreen(Bundle args) {
        super(R.id.signatureAddScreen, args);
        containerFile = getFile(args, CONTAINER_FILE);
    }

    @Override
    protected View createView(Context context) {
        TextView view = new TextView(context);
        view.setText("Add signature screen for " + containerFile.getName());
        view.setGravity(Gravity.CENTER);
        return view;
    }
}
