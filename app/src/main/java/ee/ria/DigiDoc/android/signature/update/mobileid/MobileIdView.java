package ee.ria.DigiDoc.android.signature.update.mobileid;

import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class MobileIdView extends LinearLayout implements
        SignatureAddView<MobileIdRequest, MobileIdResponse> {

    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final TextView message;
    private final EditText phoneNoView;
    private final EditText personalCodeView;
    private final CheckBox rememberMeView;

    public MobileIdView(Context context) {
        this(context, null);
    }

    public MobileIdView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MobileIdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MobileIdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                        int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_mobile_id, this);
        message = findViewById(R.id.signatureUpdateMobileIdMessage);
        phoneNoView = findViewById(R.id.signatureUpdateMobileIdPhoneNo);
        personalCodeView = findViewById(R.id.signatureUpdateMobileIdPersonalCode);
        rememberMeView = findViewById(R.id.signatureUpdateMobileIdRememberMe);

        AccessibilityUtils.setSingleCharactersContentDescription(phoneNoView);
        AccessibilityUtils.setSingleCharactersContentDescription(personalCodeView);
        AccessibilityUtils.setEditTextCursorToEnd(phoneNoView);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        phoneNoView.setText(viewModel.phoneNo());
        personalCodeView.setText(viewModel.personalCode());
        setDefaultCheckBoxToggle(viewModel);
        AccessibilityUtils.setEditTextCursorToEnd(phoneNoView);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);
        message.clearFocus();
        phoneNoView.clearFocus();
        personalCodeView.clearFocus();
    }

    @Override
    public MobileIdRequest request() {
        return MobileIdRequest.create(phoneNoView.getText().toString(),
                personalCodeView.getText().toString(), rememberMeView.isChecked());
    }

    @Override
    public void response(@Nullable MobileIdResponse response, @Nullable RadioGroup methodView) {
    }

    public void setDefaultPhoneNoPrefix(String phoneNoPrefix) {
        if (TextUtils.isEmpty(phoneNoView.getText())) {
            phoneNoView.setText(phoneNoPrefix, TextView.BufferType.EDITABLE);
            phoneNoView.setOnFocusChangeListener((view, hasfocus) -> {
                if (hasfocus) {
                    phoneNoView.setHint("372XXXXXXXX");
                } else {
                    phoneNoView.setHint("");
                }
            });
        }
    }

    private void setDefaultCheckBoxToggle(SignatureUpdateViewModel viewModel) {
        if (viewModel.phoneNo().length() > 0 && viewModel.personalCode().length() > 0) {
            rememberMeView.setChecked(true);
        } else {
            rememberMeView.setChecked(false);
        }
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(phoneNoView), afterTextChangeEvents(personalCodeView));
    }

    public boolean positiveButtonEnabled() {
        return phoneNoView.getText().length() > 3 && personalCodeView.getText().length() == 11;
    }
}
