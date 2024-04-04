package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;

public final class HomeMenuDialog extends AlertDialog {

    private final HomeMenuView menuView;

    public HomeMenuDialog(@NonNull Context context, Intent intent) {
        super(context, R.style.ThemeOverlay_Application_Menu);
        SecureUtil.markAsSecure(context, getWindow());
        menuView = new HomeMenuView(getContext(), intent, null);
        menuView.setId(R.id.mainHomeMenu);
        AccessibilityUtils.setViewAccessibilityPaneTitle(menuView, R.string.main_home_menu_title);
        setView(menuView);
    }

    public HomeMenuView getMenuView() {
        return menuView;
    }
}
