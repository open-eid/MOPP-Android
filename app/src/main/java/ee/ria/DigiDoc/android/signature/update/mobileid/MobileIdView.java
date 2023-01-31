package ee.ria.DigiDoc.android.signature.update.mobileid;

import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class MobileIdView extends LinearLayout implements
        SignatureAddView<MobileIdRequest, MobileIdResponse> {

    // Country code (3 numbers) + phone number (7 or more numbers)
    private static final int MINIMUM_PHONE_NUMBER_LENGTH = 10;
    private static final int MAXIMUM_PERSONAL_CODE_LENGTH = 11;
    private static final List<String> ALLOWED_PHONE_NUMBER_COUNTRY_CODES = List.of("370", "372");

    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final TextView message;
    private final EditText phoneNoView;
    private final TextInputEditText personalCodeView;
    private final CheckBox rememberMeView;
    private final TextInputLayout phoneNoLabel;
    private final TextInputLayout personalCodeLabel;
    private final TextWatcher textWatcher;

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

        phoneNoLabel = findViewById(R.id.signatureUpdateMobileIdPhoneNoLabel);
        personalCodeLabel = findViewById(R.id.signatureUpdateMobileIdPersonalCodeLabel);

        AccessibilityUtils.setSingleCharactersContentDescription(phoneNoView);
        AccessibilityUtils.setSingleCharactersContentDescription(personalCodeView);
        AccessibilityUtils.setEditTextCursorToEnd(phoneNoView);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);

        checkInputsValidity();

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= MAXIMUM_PERSONAL_CODE_LENGTH) {
                    s.delete(MAXIMUM_PERSONAL_CODE_LENGTH, s.length());
                }
            }
        };

        personalCodeView.addTextChangedListener(textWatcher);
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        phoneNoView.setText(viewModel.phoneNo());
        personalCodeView.setText(viewModel.personalCode());
        setDefaultCheckBoxToggle(viewModel);
        AccessibilityUtils.setEditTextCursorToEnd(phoneNoView);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);
        if (textWatcher != null) {
            personalCodeView.addTextChangedListener(textWatcher);
        }
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
        Editable phoneNumber = phoneNoView.getText();
        Editable personalCode = personalCodeView.getText();
        return (phoneNumber != null && isCountryCodeCorrect(phoneNumber.toString()) &&
                isPhoneNumberCorrect(phoneNumber.toString())) &&
                (personalCode != null && isPersonalCodeCorrect(personalCode.toString()));
    }

    @Override
    protected void onDetachedFromWindow() {
        personalCodeView.removeTextChangedListener(textWatcher);
        super.onDetachedFromWindow();
    }

    private void checkInputsValidity() {
        checkPhoneNumberValidity();
        checkPersonalCodeValidity();

        phoneNoView.setOnFocusChangeListener((view, hasfocus) -> checkPhoneNumberValidity());
        personalCodeView.setOnFocusChangeListener((view, hasfocus) -> checkPersonalCodeValidity());
    }

    private void checkPhoneNumberValidity() {
        phoneNoLabel.setError(null);

        Editable phoneNumber = phoneNoView.getText();

        if (phoneNumber != null && !phoneNumber.toString().isEmpty()) {
            if (isCountryCodeMissing(phoneNumber.toString())) {
                phoneNoLabel.setError(getResources().getString(R.string.signature_update_mobile_id_status_no_country_code));
            } else if (!isCountryCodeCorrect(phoneNoView.getText().toString())) {
                phoneNoLabel.setError(getResources().getString(R.string.signature_update_mobile_id_invalid_country_code));
            } else if (!isPhoneNumberCorrect(phoneNoView.getText().toString())) {
                phoneNoLabel.setError(getResources().getString(R.string.signature_update_mobile_id_invalid_phone_number));
            }
        }
    }

    private void checkPersonalCodeValidity() {
        personalCodeLabel.setError(null);

        if (personalCodeView.getText() != null &&
                !personalCodeView.getText().toString().isEmpty() &&
                !isPersonalCodeCorrect(personalCodeView.getText().toString())) {
            personalCodeLabel.setError(getResources().getString(R.string.signature_update_mobile_id_invalid_personal_code));
        }
    }

    // Country code (3 numbers) + phone number (7 or more numbers)
    private boolean isCountryCodeMissing(String phoneNumber) {
        return phoneNumber.length() < MINIMUM_PHONE_NUMBER_LENGTH &&
                !isCountryCodeCorrect(phoneNumber);
    }

    private boolean isCountryCodeCorrect(String phoneNumber) {
        for (String allowedCountryCode : ALLOWED_PHONE_NUMBER_COUNTRY_CODES) {
            if (phoneNumber.startsWith(allowedCountryCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPhoneNumberCorrect(String phoneNumber) {
        return phoneNumber.length() >= MINIMUM_PHONE_NUMBER_LENGTH;
    }

    private boolean isPersonalCodeCorrect(String personalCode) {
        return personalCode.length() == MAXIMUM_PERSONAL_CODE_LENGTH;
    }
}
