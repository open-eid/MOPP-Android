package ee.ria.DigiDoc.android.main.settings.util;

import android.view.View;
import android.widget.TextView;
import android.widget.Toolbar;

public class SettingsUtil {

    public static TextView getToolbarViewTitle(Toolbar toolbarView) {
        for (int i = 0; i < toolbarView.getChildCount(); i++) {
            View childView = toolbarView.getChildAt(i);
            if (childView instanceof TextView) {
                return (TextView) childView;
            }
        }

        return null;
    }
}
