package ee.ria.DigiDoc.android.main.settings.util;

import android.widget.TextView;
import android.widget.Toolbar;

public class SettingsUtil {

    public static TextView getToolbarViewTitle(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            if (toolbar.getChildAt(i) instanceof TextView) {
                return ((TextView) toolbar.getChildAt(i));
            }
        }

        return null;
    }
}
