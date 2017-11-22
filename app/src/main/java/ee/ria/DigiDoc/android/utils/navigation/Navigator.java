package ee.ria.DigiDoc.android.utils.navigation;

import android.view.ViewGroup;

public interface Navigator {

    /**
     * Returns whether this Navigator has root screen.
     *
     * @return True if the back-stack contains any screens.
     */
    boolean hasRootScreen();

    /**
     * Set the root screen, removing any others in the back-stack.
     *
     * @param screen Screen to set as root.
     */
    void setRootScreen(Screen screen);

    /**
     * Should be called from {@link android.app.Activity#onBackPressed()}.
     *
     * @return Whether or not a back action was handled by the Navigator.
     */
    boolean handleBack();

    void pushScreen(Screen screen);

    void popScreen();

    Navigator childNavigator(ViewGroup container);
}
