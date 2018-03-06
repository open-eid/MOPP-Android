package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import ee.ria.DigiDoc.R;

public final class HomeMenuDialog extends AlertDialog {

    private final HomeMenuView menuView;

    public HomeMenuDialog(@NonNull Context context) {
        super(context, R.style.ThemeOverlay_Application_Dialog_Alert);

        menuView = new HomeMenuView(getContext());
        menuView.setId(R.id.mainHomeMenu);
        setView(menuView);
    }

    public HomeMenuView getMenuView() {
        return menuView;
    }
}
