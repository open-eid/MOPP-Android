package ee.ria.DigiDoc.android.signature.create;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

public final class SignatureCreateScreen extends ConductorScreen {

    private static final String KEY_INTENT = "intent";

    public static SignatureCreateScreen create(@Nullable android.content.Intent intent) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_INTENT, intent);
        return new SignatureCreateScreen(args);
    }

    @Nullable private final Intent intent;

    @SuppressWarnings("WeakerAccess")
    public SignatureCreateScreen(Bundle args) {
        super(R.id.signatureCreateScreen);
        intent = args.getParcelable(KEY_INTENT);
    }

    @Override
    protected View view(Context context) {
        return new SignatureCreateView(context, getInstanceId(), intent);
    }
}
