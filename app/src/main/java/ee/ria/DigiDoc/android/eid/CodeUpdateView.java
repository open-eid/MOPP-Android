package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.textfield.TextInputLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeInvalidError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeMinLengthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfDateOfBirthError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodePartOfPersonalCodeError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeRepeatMismatchError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeSameAsCurrentError;
import ee.ria.DigiDoc.android.eid.CodeUpdateError.CodeTooEasyError;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.mvi.State;
import io.reactivex.rxjava3.core.Observable;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.InputMethodUtils.hideSoftKeyboard;

public final class CodeUpdateView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final TextView successMessageView;
    private final OrderedListView textView;
    private final TextInputLayout currentLabelView;
    private final EditText currentView;
    private final TextInputLayout newLabelView;
    private final EditText newTextView;
    private final TextInputLayout repeatLabelView;
    private final EditText repeatView;
    private final Button negativeButton;
    private final Button positiveButton;
    private final View activityOverlayView;
    private final View activityIndicatorView;
    private final ScrollView scrollView;

    private static final int MAXIMUM_PIN_CODE_LENGTH = 12;

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
        newLabelView = findViewById(R.id.eidHomeUpdateNewLabel);
        newTextView = findViewById(R.id.eidHomeUpdateNewText);
        repeatLabelView = findViewById(R.id.eidHomeCodeUpdateRepeatLabel);
        repeatView = findViewById(R.id.eidHomeCodeUpdateRepeat);
        negativeButton = findViewById(R.id.eidHomeCodeUpdateNegativeButton);
        positiveButton = findViewById(R.id.eidHomeCodeUpdatePositiveButton);
        activityOverlayView = findViewById(R.id.activityOverlay);
        activityIndicatorView = findViewById(R.id.activityIndicator);
        scrollView = findViewById(R.id.eidHomeCodeUpdateScroll);
    }

    public void render(@State String state, CodeUpdateAction action,
                       @Nullable CodeUpdateResponse response, boolean successMessageVisible) {
        toolbarView.setTitle(action.titleRes());
        toolbarView.setNavigationIcon(R.drawable.ic_clear);
        setToolbarContentDescription(toolbarView,
                getResources().getString(action.titleRes()).toLowerCase(),
                getResources().getString(R.string.close));
        successMessageView.setText(action.successMessageRes());
        AccessibilityUtils.setContentDescription(successMessageView, getResources().getString(action.successMessageRes()));
        textView.itemsRes(action.textRowsRes());
        currentLabelView.setHint(getResources().getString(action.currentRes()));
        AccessibilityUtils.setContentDescription(currentLabelView, getResources().getString(action.currentRes()));
        newLabelView.setHint(getResources().getString(action.newRes(), action.newMinLength()));
        AccessibilityUtils.setContentDescription(newLabelView, getResources().getString(action.newRes(), action.newMinLength()));
        repeatLabelView.setHint(getResources().getString(action.repeatRes()));
        AccessibilityUtils.setContentDescription(repeatLabelView, getResources().getString(action.repeatRes()));
        positiveButton.setText(action.positiveButtonRes());
        positiveButton.setContentDescription(getResources().getString(action.positiveButtonRes()).toLowerCase());
        activityOverlayView.setVisibility(state.equals(State.ACTIVE) ? VISIBLE : GONE);
        activityIndicatorView.setVisibility(state.equals(State.ACTIVE) ? VISIBLE : GONE);

        checkPinLength(currentView);
        checkPinLength(newView);
        checkPinLength(repeatView);

        int changeButtonDescriptionResId;
        int cancelButtonDescriptionResId;
        int overlayPaneTitleResId;
        switch (action.pinType()) {
            case PIN1:
                if (action.updateType().equals(CodeUpdateType.UNBLOCK)) {
                    changeButtonDescriptionResId = R.string.confirm_pin1_unblock;
                    cancelButtonDescriptionResId = R.string.cancel_pin1_unblock;
                    overlayPaneTitleResId = R.string.eid_home_code_update_title_pin1_unblock;
                } else {
                    changeButtonDescriptionResId = R.string.confirm_pin1_change;
                    cancelButtonDescriptionResId = R.string.cancel_pin1_change;
                    overlayPaneTitleResId = R.string.eid_home_code_update_title_pin1_edit;
                }
                break;
            case PIN2:
                if (action.updateType().equals(CodeUpdateType.UNBLOCK)) {
                    changeButtonDescriptionResId = R.string.confirm_pin2_unblock;
                    cancelButtonDescriptionResId = R.string.cancel_pin2_unblock;
                    overlayPaneTitleResId = R.string.eid_home_code_update_title_pin2_unblock;
                } else {
                    changeButtonDescriptionResId = R.string.confirm_pin2_change;
                    cancelButtonDescriptionResId = R.string.cancel_pin2_change;
                    overlayPaneTitleResId = R.string.eid_home_code_update_title_pin2_edit;
                }
                break;
            case PUK:
                changeButtonDescriptionResId = R.string.confirm_puk_change;
                cancelButtonDescriptionResId = R.string.cancel_puk_change;
                overlayPaneTitleResId = R.string.eid_home_code_update_title_puk_edit;
                break;
            default:
                throw new IllegalArgumentException("Unknown pin type " + action.pinType());
        }

        positiveButton.setContentDescription(getResources().getString(changeButtonDescriptionResId));
        negativeButton.setContentDescription(getResources().getString(cancelButtonDescriptionResId));
        AccessibilityUtils.setViewAccessibilityPaneTitle(toolbarView, overlayPaneTitleResId);

        successMessageView.setVisibility(successMessageVisible ? VISIBLE : GONE);
        if (successMessageVisible) {
            AccessibilityUtils.setContentDescription(successMessageView, getResources().getString(action.successMessageRes()));
            scrollView.smoothScrollTo(0, 0);
            new Handler(Looper.getMainLooper()).post(successMessageView::requestFocus);
            AccessibilityUtils.sendAccessibilityEvent(getContext(),
                        AccessibilityEvent.TYPE_ANNOUNCEMENT,
                    successMessageView.getText().toString().toLowerCase());
        }

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
                setViewContentDescription(currentLabelView, getResources().getString(
                        action.currentMinLengthErrorRes(), ((CodeMinLengthError) currentError).minLength()));
            } else if (currentError instanceof CodeInvalidError) {
                int retryCount = ((CodeInvalidError) currentError).retryCount();
                if (retryCount == 1) {
                    currentLabelView.setError(getResources().getString(
                            action.currentInvalidFinalErrorRes()));
                    setViewContentDescription(currentLabelView, getResources().getString(
                            action.currentInvalidFinalErrorRes()));
                } else {
                    currentLabelView.setError(getResources().getString(
                            action.currentInvalidErrorRes(), retryCount));
                    setViewContentDescription(currentLabelView, getResources().getString(
                            action.currentInvalidErrorRes(), retryCount));
                }
            }

            if (newError == null) {
                 newLabelView.setError(null);
            } else if (newError instanceof CodeMinLengthError) {
                repeatLabelView.setError(getResources().getString(action.newMinLengthErrorRes(),
                        ((CodeMinLengthError) newError).minLength()));
                setViewContentDescription(repeatLabelView, getResources().getString(action.newMinLengthErrorRes(),
                        ((CodeMinLengthError) newError).minLength()));
            } else if (newError instanceof CodePartOfPersonalCodeError) {
                repeatLabelView.setError(getResources().getString(action.newPersonalCodeErrorRes()));
                setViewContentDescription(repeatLabelView, getResources().getString(action.newPersonalCodeErrorRes()));
            } else if (newError instanceof CodePartOfDateOfBirthError) {
                repeatLabelView.setError(getResources().getString(action.newDateOfBirthErrorRes()));
                setViewContentDescription(repeatLabelView, getResources().getString(action.newDateOfBirthErrorRes()));
            } else if (newError instanceof CodeTooEasyError) {
                repeatLabelView.setError(getResources().getString(action.newTooEasyErrorRes()));
                setViewContentDescription(repeatLabelView, getResources().getString(action.newTooEasyErrorRes()));
            } else if (newError instanceof CodeSameAsCurrentError) {
                repeatLabelView.setError(getResources().getString(action.newSameAsCurrentErrorRes()));
                setViewContentDescription(repeatLabelView, getResources().getString(action.newSameAsCurrentErrorRes()));
            }

            if (repeatError instanceof CodeRepeatMismatchError) {
                repeatLabelView.setError(getResources().getString(action.repeatMismatchErrorRes()));
                setViewContentDescription(repeatLabelView, getResources().getString(action.repeatMismatchErrorRes()));
            }

            if (newError != null || repeatError != null) {
                newLabelView.setError(" ");
            }
        }

        AccessibilityUtils.setContentDescription(currentView, getResources().getString(action.currentRes()));
        AccessibilityUtils.setContentDescription(newTextView, getResources().getString(action.newRes(), action.newMinLength()));
        AccessibilityUtils.setContentDescription(repeatView, getResources().getString(action.repeatRes()));
    }

    public void clear() {
        currentView.setText(null);
        newTextView.setText(null);
        repeatView.setText(null);
        hideSoftKeyboard(this);

        currentView.clearFocus();
        newTextView.clearFocus();
        repeatView.clearFocus();
    }

    public Observable closeButtonClick() {
        return clicks(negativeButton);
    }

    public Observable backButtonClick() {
        return navigationClicks(toolbarView);
    }

    public Observable<CodeUpdateRequest> requests() {
        return clicks(positiveButton)
                .map(ignored -> CodeUpdateRequest.create(currentView.getText().toString().trim(),
                        newTextView.getText().toString().trim(),
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

    private void setViewContentDescription(TextInputLayout textInputLayout, String contentDescription) {
        AppCompatTextView appCompatTextView = TextUtil.getTextInputLayoutAppCompatTextView(textInputLayout);
        if (appCompatTextView != null) {
            AccessibilityUtils.setContentDescription(appCompatTextView, contentDescription);
        }
    }

    private void setToolbarContentDescription(Toolbar toolbarView, String titleContentDescription, String imageContentDescription) {
        for (int i = 0; i < toolbarView.getChildCount(); i++) {
            View view = toolbarView.getChildAt(i);
            if (view instanceof TextView) {
                view.setContentDescription(titleContentDescription);
            } else if (view instanceof ImageView) {
                view.setContentDescription(imageContentDescription);
            }
        }
    }

    private void checkPinLength(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= MAXIMUM_PIN_CODE_LENGTH) {
                    s.delete(MAXIMUM_PIN_CODE_LENGTH, s.length());
                }
            }
        });
    }
}
