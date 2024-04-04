package ee.ria.DigiDoc.android.root;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class RootCreateScreen extends ConductorScreen {

    public static RootCreateScreen create() {
        return new RootCreateScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public RootCreateScreen() {
        super(R.id.signatureCreateScreen);
    }

    @Override
    protected View view(Context context) {
        return new RootCreateView(context);
    }
}
