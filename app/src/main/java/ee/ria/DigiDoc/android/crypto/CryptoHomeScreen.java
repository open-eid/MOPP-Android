package ee.ria.DigiDoc.android.crypto;

import android.content.Context;
import android.view.View;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.conductor.ConductorScreen;

public final class CryptoHomeScreen extends ConductorScreen {

    public static CryptoHomeScreen create() {
        return new CryptoHomeScreen();
    }

    @SuppressWarnings("WeakerAccess")
    public CryptoHomeScreen() {
        super(R.id.cryptoHomeScreen);
    }

    @Override
    protected View createView(Context context) {
        return new CryptoHomeView(context);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof CryptoHomeScreen;
    }
}
