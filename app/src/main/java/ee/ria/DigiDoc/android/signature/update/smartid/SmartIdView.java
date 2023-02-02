/*
 * app
 * Copyright 2017 - 2022 Riigi Infosüsteemi Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.android.signature.update.smartid;

import static com.jakewharton.rxbinding4.widget.RxTextView.afterTextChangeEvents;
import static ee.ria.DigiDoc.android.Constants.MAXIMUM_PERSONAL_CODE_LENGTH;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.validator.PersonalCodeValidator;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class SmartIdView extends LinearLayout implements
        SignatureAddView<SmartIdRequest, SmartIdResponse> {

    private static final List<String> COUNTRY_LIST = Arrays.asList("EE", "LT", "LV");
    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final TextView message;
    private final Spinner countryView;
    private final TextInputEditText personalCodeView;
    private final CheckBox rememberMeView;
    private final TextInputLayout personalCodeLabel;

    public SmartIdView(Context context) {
        this(context, null);
    }

    public SmartIdView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartIdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SmartIdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                       int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_smart_id, this);
        message = findViewById(R.id.signatureUpdateSmartIdMessage);
        countryView = findViewById(R.id.signatureUpdateSmartIdCountry);
        personalCodeView = findViewById(R.id.signatureUpdateSmartIdPersonalCode);
        rememberMeView = findViewById(R.id.signatureUpdateSmartIdRememberMe);
        personalCodeLabel = findViewById(R.id.signatureUpdateSmartIdPersonalCodeLabel);
        countryView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setPersonalCodeViewFilters(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setPersonalCodeViewFilters(0);
            }
        });

        AccessibilityUtils.setSingleCharactersContentDescription(personalCodeView);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);

        checkForDoneButtonClick();
        checkInputsValidity();
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        countryView.setSelection(COUNTRY_LIST.indexOf(viewModel.country()));
        setPersonalCodeViewFilters(countryView.getSelectedItemPosition());
        personalCodeView.setText(viewModel.sidPersonalCode());
        rememberMeView.setChecked(personalCodeView.getText().length() > 0);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);

        message.clearFocus();
        countryView.clearFocus();
        personalCodeView.clearFocus();
        rememberMeView.clearFocus();
    }

    @Override
    public SmartIdRequest request() {
        return SmartIdRequest.create(COUNTRY_LIST.get(countryView.getSelectedItemPosition()),
                personalCodeView.getText().toString(), rememberMeView.isChecked());
    }

    @Override
    public void response(@Nullable SmartIdResponse response, @Nullable RadioGroup methodView) {
        if (methodView != null) {
            handleAccessibility(methodView);
        }
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(personalCodeView));
    }

    public boolean positiveButtonEnabled() {
        Editable personalCode = personalCodeView.getText();
        if (personalCode != null) {
            PersonalCodeValidator.validatePersonalCode(personalCodeView);
            return countryView.getSelectedItemPosition() != 0 ||
                    isPersonalCodeCorrect(personalCode.toString());
        }
        return false;
    }

    private void checkInputsValidity() {
        checkPersonalCodeValidity();

        personalCodeView.setOnFocusChangeListener((view, hasfocus) -> checkPersonalCodeValidity());
    }

    private void checkPersonalCodeValidity() {
        personalCodeLabel.setError(null);

        if (personalCodeView.getText() != null &&
                !personalCodeView.getText().toString().isEmpty() &&
                !isPersonalCodeCorrect(personalCodeView.getText().toString())) {
            personalCodeLabel.setError(getResources().getString(
                    R.string.signature_update_smart_id_invalid_personal_code));
        }
    }

    private boolean isPersonalCodeCorrect(String personalCode) {
        return personalCode.length() == MAXIMUM_PERSONAL_CODE_LENGTH;
    }

    private void checkForDoneButtonClick() {
        // Remove focus on "Done" click
        personalCodeView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                personalCodeView.setEnabled(false);
                personalCodeView.setEnabled(true);
            }
            return false;
        });
    }

    private void setPersonalCodeViewFilters(int country) {
        personalCodeView.setInputType(country == 0 ? EditorInfo.TYPE_CLASS_NUMBER : EditorInfo.TYPE_CLASS_PHONE);
        InputFilter[] inputFilters = personalCodeView.getFilters();
        int pos = -1;
        if (inputFilters == null) {
            inputFilters = new InputFilter[0];
        }
        for (int i = 0; i < inputFilters.length; ++i) {
            if (inputFilters[i] instanceof InputFilter.LengthFilter) {
                pos = i;
                break;
            }
        }
        if (country == 0) {
            if (pos == -1) {
                inputFilters = Arrays.copyOf(inputFilters, inputFilters.length + 1);
                inputFilters[inputFilters.length - 1] = new InputFilter.LengthFilter(11);
            }
            personalCodeView.setText(personalCodeView.getText().subSequence(0,
                    Math.min(personalCodeView.getText().length(), 11)));
            AccessibilityUtils.setSingleCharactersContentDescription(personalCodeView);
        } else {
            if (pos != -1) {
                InputFilter[] copy = Arrays.copyOf(inputFilters, inputFilters.length - 1);
                System.arraycopy(inputFilters, pos + 1, copy, pos, inputFilters.length - (pos + 1));
                inputFilters = copy;
            }
        }
        personalCodeView.setFilters(inputFilters);
    }

    private void setupContentDescriptions(RadioButton radioButton, CharSequence contentDescription) {
        radioButton.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                if (!event.getText().isEmpty() &&
                        (event.getText().get(0).toString().equals(
                                getResources().getString(R.string.signature_update_signature_add_method_mobile_id)) ||
                                event.getText().get(0).toString().equals(
                                        getResources().getString(R.string.signature_update_signature_add_method_id_card)))) {
                    event.getText().clear();
                    event.getText().add(getContentDescription());
                }
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setContentDescription(contentDescription);
                info.setCheckable(false);
                info.setClickable(false);
                info.setClassName("");
                info.setPackageName("");
                info.setText(contentDescription);
                info.setViewIdResourceName("");
                info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION);
            }
        });
    }

    private void handleAccessibility(RadioGroup methodView) {
        RadioButton mobileIdRadioButton = methodView.findViewById(R.id.signatureUpdateSignatureAddMethodMobileId);
        CharSequence mobileIdContentDescription = mobileIdRadioButton.getContentDescription();
        mobileIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mobileIdRadioButton.setContentDescription("");
        setupContentDescriptions(mobileIdRadioButton, mobileIdContentDescription);

        RadioButton idCardRadioButton = methodView.findViewById(R.id.signatureUpdateSignatureAddMethodIdCard);
        CharSequence idCardContentDescription = idCardRadioButton.getContentDescription();
        idCardRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        idCardRadioButton.setContentDescription("");
        setupContentDescriptions(idCardRadioButton, idCardContentDescription);

        postDelayed(() -> {
            mobileIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            mobileIdRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_mobile_id, 1, 3));

            idCardRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            idCardRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_id_card, 3, 3));
        }, 3500);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
