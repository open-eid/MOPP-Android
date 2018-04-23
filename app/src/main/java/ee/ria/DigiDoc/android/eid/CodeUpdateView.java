package ee.ria.DigiDoc.android.eid;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
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
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class CodeUpdateView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final TextView textView;
    private final TextInputLayout currentLabelView;
    private final EditText currentView;
    private final TextInputLayout newLabelView;
    private final EditText newView;
    private final TextInputLayout repeatLabelView;
    private final EditText repeatView;
    private final Button negativeButton;
    private final Button positiveButton;

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
        textView = findViewById(R.id.eidHomeCodeUpdateText);
        currentLabelView = findViewById(R.id.eidHomeCodeUpdateCurrentLabel);
        currentView = findViewById(R.id.eidHomeCodeUpdateCurrent);
        newLabelView = findViewById(R.id.eidHomeCodeUpdateNewLabel);
        newView = findViewById(R.id.eidHomeCodeUpdateNew);
        repeatLabelView = findViewById(R.id.eidHomeCodeUpdateRepeatLabel);
        repeatView = findViewById(R.id.eidHomeCodeUpdateRepeat);
        negativeButton = findViewById(R.id.eidHomeCodeUpdateNegativeButton);
        positiveButton = findViewById(R.id.eidHomeCodeUpdatePositiveButton);
    }

    public void action(CodeUpdateAction action, @Nullable CodeUpdateResponse response) {
        toolbarView.setTitle(action.titleRes());
        textView.setText(action.textRes());
        currentLabelView.setHint(getResources().getString(action.currentRes()));
        newLabelView.setHint(getResources().getString(action.newRes()));
        repeatLabelView.setHint(getResources().getString(action.repeatRes()));
        positiveButton.setText(action.positiveButtonRes());

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
                currentLabelView.setError(getResources().getString(action.currentInvalidErrorRes(),
                        ((CodeInvalidError) currentError).retryCount()));
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

    public Observable<Object> closes() {
        return Observable.merge(navigationClicks(toolbarView), clicks(negativeButton));
    }

    public Observable<CodeUpdateRequest> requests() {
        return clicks(positiveButton).map(ignored ->
                CodeUpdateRequest.create(currentView.getText().toString().trim(),
                        newView.getText().toString().trim(),
                        repeatView.getText().toString().trim()));
    }
}
