package ee.ria.DigiDoc.android.utils.navigator;

import android.app.Activity;
import androidx.lifecycle.ViewModel;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.ViewGroup;

import io.reactivex.rxjava3.core.Observable;

public interface Navigator {

    interface BackButtonClickListener {

        /**
         * Called when back button is clicked.
         *
         * @return Return true if this listener takes care of the back button handling,
         * false for default behavior.
         */
        boolean onBackButtonClick();
    }

    /**
     * Has to be called in {@link Activity#onCreate(Bundle)}.
     *
     * @param activity Activity instance.
     * @param container Container for screens.
     * @param savedInstanceState Saved instance state.
     */
    void onCreate(Activity activity, ViewGroup container, @Nullable Bundle savedInstanceState);

    /**
     * Has to be called in {@link Activity#onBackPressed()}.
     *
     * if (!navigator.onBackPressed()) {
     *     super.onBackPressed();
     * }
     *
     * @return Whether there were any screens in the back stack.
     */
    boolean onBackPressed();

    /**
     * Has to be called in {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode Request code.
     * @param resultCode Result code.
     * @param data Data.
     */
    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    /**
     * Create or return an existing {@link ViewModel view model} for current screen.
     *
     * @param screenId ID of the screen.
     * @param type Type of the view model.
     * @param <T> Type of the view model.
     * @return View model instance.
     */
    <T extends ViewModel> T viewModel(String screenId, Class<T> type);

    void clearViewModel(String screenId);

    /**
     * Execute {@link Transaction transaction}.
     *
     * @param transaction Transaction to execute.
     */
    void execute(Transaction transaction);

    Observable<ActivityResult> activityResults();

    void addBackButtonClickListener(BackButtonClickListener listener);

    void removeBackButtonClickListener(BackButtonClickListener listener);

    Activity activity();
}
