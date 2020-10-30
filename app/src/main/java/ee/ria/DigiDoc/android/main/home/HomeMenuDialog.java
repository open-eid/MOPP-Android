package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;

public final class HomeMenuDialog extends AlertDialog {

    private final HomeMenuView menuView;

    public HomeMenuDialog(@NonNull Context context) {
        super(context, R.style.ThemeOverlay_Application_Menu);
        menuView = new HomeMenuView(getContext());
        menuView.setId(R.id.mainHomeMenu);
        AccessibilityUtils.setAccessibilityPaneTitle(menuView, R.string.main_home_menu_title);
        setView(menuView);
    }

    public HomeMenuView getMenuView() {
        return menuView;
    }
}
