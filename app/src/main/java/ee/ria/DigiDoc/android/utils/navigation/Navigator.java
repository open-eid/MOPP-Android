package ee.ria.DigiDoc.android.utils.navigation;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import io.reactivex.Observable;

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

    /**
     * @see #getActivityResult(int, Intent, Bundle) Same but options = null.
     */
    void getActivityResult(int requestCode, Intent intent);

    /**
     * Query for activity results.
     *
     * @param requestCode Request code is used to differentiate between queries and results.
     * @param intent The intent to start to get results.
     * @param options See {@link android.app.Activity#startActivityForResult(Intent, int, Bundle)}.
     */
    void getActivityResult(int requestCode, Intent intent, @Nullable Bundle options);

    /**
     * Observe activity results with given request code.
     *
     * @param requestCode Filter out results with other request codes.
     */
    Observable<ActivityResult> activityResults(int requestCode);

    /**
     * Observe activity results with given request code and {@link android.app.Activity#RESULT_OK}
     * result.
     *
     * @param requestCode Filter out results with other request codes.
     */
    Observable<Intent> activityOkResults(int requestCode);
}
