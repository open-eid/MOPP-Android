package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeInvalidError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeMinLengthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfDateOfBirthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfPersonalCodeError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeRepeatMismatchError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeSameAsCurrentError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeTooEasyError;
import ee.ria.DigiDoc.android.utils.mvi.State;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.InputMethodUtils.hideSoftKeyboard;

public final class CodeUpdateView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final TextView successMessageView;
    private final OrderedListView textView;
    private final TextInputLayout currentLabelView;
    private final EditText currentView;
    private final TextInputLayout newLabelView;
    private final EditText newView;
    private final TextInputLayout repeatLabelView;
    private final EditText repeatView;
    private final Button negativeButton;
    private final Button positiveButton;
    private final View activityOverlayView;
    private final View activityIndicatorView;

    public CodeUpdateView(Context context) {
        this(context, null);
    }

    public CodeUpdateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodeUpdateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.eid_home_code_update, this);
        toolbarView = findViewById(R.id.toolbar);
        successMessageView = findViewById(R.id.eidHomeCodeUpdateSuccessMessage);
        textView = findViewById(R.id.eidHomeCodeUpdateText);
        currentLabelView = findViewById(R.id.eidHomeCodeUpdateCurrentLabel);
        currentView = findViewById(R.id.eidHomeCodeUpdateCurrent);
        newLabelView = findViewById(R.id.eidHomeCodeUpdateNewLabel);
        newView = findViewById(R.id.eidHomeCodeUpdateNew);
        repeatLabelView = findViewById(R.id.eidHomeCodeUpdateRepeatLabel);
        repeatView = findViewById(R.id.eidHomeCodeUpdateRepeat);
        negativeButton = findViewById(R.id.eidHomeCodeUpdateNegativeButton);
        positiveButton = findViewById(R.id.eidHomeCodeUpdatePositiveButton);
        activityOverlayView = findViewById(R.id.activityOverlay);
        activityIndicatorView = findViewById(R.id.activityIndicator);
    }

    public void render(@State String state, CodeUpdateAction action,
                       @Nullable CodeUpdateResponse response, boolean successMessageVisible) {
        toolbarView.setTitle(action.titleRes());
        successMessageView.setText(action.successMessageRes());
        textView.itemsRes(action.textRowsRes());
        currentLabelView.setHint(getResources().getString(action.currentRes()));
        newLabelView.setHint(getResources().getString(action.newRes(), action.newMinLength()));
        repeatLabelView.setHint(getResources().getString(action.repeatRes()));
        positiveButton.setText(action.positiveButtonRes());
        activityOverlayView.setVisibility(state.equals(State.ACTIVE) ? VISIBLE : GONE);
        activityIndicatorView.setVisibility(state.equals(State.ACTIVE) ? VISIBLE : GONE);

        successMessageView.setVisibility(successMessageVisible ? VISIBLE : GONE);

        if (state.equals(State.CLEAR)) {
            clear();
        }

        if (response == null) {
            currentLabelView.setError(null);
            newLabelView.setError(null);
            repeatLabelView.setError(null);
        } else {
            CodeUpdateError currentError = response.currentError();
            CodeUpdateError newError = response.newError();
            CodeUpdateError repeatError = response.repeatError();

            if (currentError == null) {
                currentLabelView.setError(null);
            } else if (currentError instanceof CodeMinLengthError) {
                currentLabelView.setError(
                        getResources().getString(action.currentMinLengthErrorRes(),
                                ((CodeMinLengthError) currentError).minLength()));
            } else if (currentError instanceof CodeInvalidError) {
                int retryCount = ((CodeInvalidError) currentError).retryCount();
                if (retryCount == 1) {
                    currentLabelView.setError(getResources().getString(
                            action.currentInvalidFinalErrorRes()));
                } else {
                    currentLabelView.setError(getResources().getString(
                            action.currentInvalidErrorRes(), retryCount));
                }
            }

            if (newError == null) {
                newLabelView.setError(null);
            } else if (newError instanceof CodeMinLengthError) {
                newLabelView.setError(getResources().getString(action.newMinLengthErrorRes(),
                        ((CodeMinLengthError) newError).minLength()));
            } else if (newError instanceof CodePartOfPersonalCodeError) {
                newLabelView.setError(getResources().getString(action.newPersonalCodeErrorRes()));
            } else if (newError instanceof CodePartOfDateOfBirthError) {
                newLabelView.setError(getResources().getString(action.newDateOfBirthErrorRes()));
            } else if (newError instanceof CodeTooEasyError) {
                newLabelView.setError(getResources().getString(action.newTooEasyErrorRes()));
            } else if (newError instanceof CodeSameAsCurrentError) {
                newLabelView.setError(getResources().getString(action.newSameAsCurrentErrorRes()));
            }

            if (repeatError == null) {
                repeatLabelView.setError(null);
            } else if (repeatError instanceof CodeRepeatMismatchError) {
                repeatLabelView.setError(getResources().getString(action.repeatMismatchErrorRes()));
            }
        }
    }

    public void clear() {
        currentView.setText(null);
        newView.setText(null);
        repeatView.setText(null);
        hideSoftKeyboard(this);
    }

    public Observable<Object> closes() {
        return Observable.merge(navigationClicks(toolbarView), clicks(negativeButton));
    }

    public Observable<CodeUpdateRequest> requests() {
        return clicks(positiveButton).map(ignored ->
                CodeUpdateRequest.create(currentView.getText().toString().trim(),
                        newView.getText().toString().trim(),
                        repeatView.getText().toString().trim()));
    }

    @Override
    public void onDetachedFromWindow() {
        /*
         * On some devices (Nexus 6, Android 7.1.1),
         * exception is thrown without closing the keyboard.
         *
         * java.lang.NullPointerException: Attempt to invoke interface method
         * 'void android.view.inputmethod.InputConnection.closeConnection()'
         * on a null object reference
         */
        hideSoftKeyboard(this);
        super.onDetachedFromWindow();
    }
}
