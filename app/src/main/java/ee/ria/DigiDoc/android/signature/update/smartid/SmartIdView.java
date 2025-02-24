/*
 * app
 * Copyright 2017 - 2024 Riigi Infosüsteemi Amet
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
import static ee.ria.DigiDoc.android.Constants.MAXIMUM_ESTONIAN_PERSONAL_CODE_LENGTH;
import static ee.ria.DigiDoc.android.Constants.MAXIMUM_LATVIAN_PERSONAL_CODE_LENGTH;
import static ee.ria.DigiDoc.android.accessibility.AccessibilityUtils.removeAccessibilityStateChanged;
import static ee.ria.DigiDoc.android.utils.ErrorMessageUtil.setTextViewError;
import static ee.ria.DigiDoc.android.utils.TextUtil.removeTextWatcher;
import static ee.ria.DigiDoc.common.TextUtil.PERSONAL_CODE_SYMBOLS;
import static ee.ria.DigiDoc.common.TextUtil.getSymbolsFilter;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.validator.PersonalCodeValidator;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class SmartIdView extends LinearLayout implements
        SignatureAddView<SmartIdRequest, SmartIdResponse> {

    private SignatureUpdateViewModel viewModel;

    private static final List<String> COUNTRY_LIST = Arrays.asList("EE", "LT", "LV");
    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final TextView message;
    private final TextView countryViewLabel;
    private final Spinner countryView;
    private final TextView personalCodeViewLabel;
    private final TextInputLayout personalCodeViewLayoutLabel;
    private final TextInputEditText personalCodeView;
    private final CheckBox rememberMeView;

    private final TextWatcher personalCodeTextWatcher;
    private final TextWatcher personalCodeHandler;
    private AccessibilityManager.TouchExplorationStateChangeListener accessibilityTouchExplorationStateChangeListener;

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
        countryViewLabel = findViewById(R.id.signatureUpdateSmartIdCountryText);
        countryView = findViewById(R.id.signatureUpdateSmartIdCountry);
        personalCodeViewLabel = findViewById(R.id.signatureUpdateSmartIdPersonalCodeLabel);
        personalCodeViewLayoutLabel = findViewById(R.id.signatureUpdateSmartIdPersonalCodeLayoutLabel);
        personalCodeView = findViewById(R.id.signatureUpdateSmartIdPersonalCode);
        rememberMeView = findViewById(R.id.signatureUpdateSmartIdRememberMe);
        countryView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setPersonalCodeViewFilters(position);
                countryView.setContentDescription(getCountryViewAccessibilityText());
                personalCodeView.setHint((position == 2) ? "123456-78901" : null);
                personalCodeView.setText(
                        viewModel != null &&
                                COUNTRY_LIST.indexOf(viewModel.country()) == position ?
                                viewModel.sidPersonalCode() : "");
                if (position != 2) {
                    personalCodeView.removeTextChangedListener(personalCodeHandler);
                } else {
                    setSmartIdPersonalCodeHandler(personalCodeView, personalCodeHandler);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setPersonalCodeViewFilters(0);
            }
        });

        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);

        checkForDoneButtonClick();
        checkInputsValidity();

        personalCodeTextWatcher = TextUtil.addTextWatcher(personalCodeView);
        personalCodeHandler = TextUtil.smartIdLatvianPersonalCodeHandler();
        personalCodeView.setFilters(new InputFilter[]{getSymbolsFilter(PERSONAL_CODE_SYMBOLS)});

        if (countryView.getSelectedItemPosition() == 2) {
            setSmartIdPersonalCodeHandler(personalCodeView, personalCodeHandler);
        }

        if (AccessibilityUtils.isTalkBackEnabled()) {
            setAccessibilityDescription();
            AccessibilityUtils.setTextViewContentDescription(context, true, null, personalCodeViewLabel.getText().toString(), personalCodeView);
        }
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        this.viewModel = viewModel;

        countryView.setSelection(COUNTRY_LIST.indexOf(viewModel.country()));
        setPersonalCodeViewFilters(countryView.getSelectedItemPosition());
        personalCodeView.setText(viewModel.sidPersonalCode());
        rememberMeView.setChecked(true);
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);

        setTextViewError(getContext(), null, personalCodeViewLabel, personalCodeViewLayoutLabel, personalCodeView);

        message.clearFocus();
        countryView.clearFocus();
        personalCodeView.clearFocus();
        rememberMeView.clearFocus();

        removeTextWatcher(personalCodeView, personalCodeTextWatcher);
        removeTextWatcher(personalCodeView, personalCodeHandler);
        removeAccessibilityStateChanged(accessibilityTouchExplorationStateChangeListener);
    }

    @Override
    public SmartIdRequest request() {
        return SmartIdRequest.create(COUNTRY_LIST.get(countryView.getSelectedItemPosition()),
                personalCodeView.getText() != null ? personalCodeView.getText().toString() : "",
                rememberMeView.isChecked());
    }

    @Override
    public void response(@Nullable SmartIdResponse response, @Nullable RadioGroup methodView) {
        if (methodView != null) {
            handleAccessibility(methodView);
        }
    }

    void setSmartIdPersonalCodeHandler(TextInputEditText personalCodeView, TextWatcher textHandler) {
        personalCodeView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                personalCodeView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                removeTextWatcher(personalCodeView, textHandler);
                personalCodeView.addTextChangedListener(textHandler);
            }
        });
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(personalCodeView));
    }

    public boolean positiveButtonEnabled() {
        Editable personalCode = personalCodeView.getText();
        if (personalCode != null) {
            if (countryView.getSelectedItemPosition() == 0) {
                PersonalCodeValidator.validateEstonianPersonalCode(personalCodeView);
            } else if (countryView.getSelectedItemPosition() == 2) {
                PersonalCodeValidator.validateLatvianPersonalCode(personalCodeView);
            }
            return ((countryView.getSelectedItemPosition() == 0 || countryView.getSelectedItemPosition() == 1) &&
                    personalCode.toString().length() == MAXIMUM_ESTONIAN_PERSONAL_CODE_LENGTH) ||
                    (countryView.getSelectedItemPosition() == 2 &&
                            personalCode.toString().length() == MAXIMUM_LATVIAN_PERSONAL_CODE_LENGTH);
        }
        return false;
    }

    private void checkInputsValidity() {
        checkPersonalCodeValidity();

        personalCodeView.setOnFocusChangeListener((view, hasFocus) -> {
            checkPersonalCodeValidity();
            if (hasFocus) {
                AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);
            }
        });
    }

    private void checkPersonalCodeValidity() {
        setTextViewError(getContext(), null, personalCodeViewLabel, personalCodeViewLayoutLabel, null);
        personalCodeViewLabel.setError(null);

        if (Optional.ofNullable(personalCodeView.getText())
                .map(Editable::toString)
                .filter(text -> !text.isEmpty() && !isPersonalCodeCorrect(text))
                .isPresent()) {
            setTextViewError(getContext(), getResources().getString(
                            R.string.signature_update_smart_id_invalid_personal_code),
                    personalCodeViewLabel, personalCodeViewLayoutLabel, null);
        }
    }

    private boolean isPersonalCodeCorrect(String personalCode) {
        if (personalCode.contains("-")) {
            return personalCode.length() == MAXIMUM_LATVIAN_PERSONAL_CODE_LENGTH;
        }
        return personalCode.length() == MAXIMUM_ESTONIAN_PERSONAL_CODE_LENGTH;
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
            personalCodeView.setText(Objects.requireNonNull(personalCodeView.getText())
                    .subSequence(0, Math.min(personalCodeView.getText().length(), 11)));
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
            public void onPopulateAccessibilityEvent(
                    @NonNull View host, @NonNull AccessibilityEvent event) {
                if (!event.getText().isEmpty() &&
                        (event.getText().get(0).toString().equals(getResources().getString(R.string.signature_update_signature_add_method_mobile_id)) ||
                                event.getText().get(0).toString().equals(getResources().getString(R.string.signature_update_signature_add_method_id_card)))) {
                    event.getText().clear();
                    event.getText().add(getContentDescription());
                }
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfo info) {
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

        RadioButton nfcRadioButton = methodView.findViewById(R.id.signatureUpdateSignatureAddMethodNFC);
        CharSequence nfcContentDescription = nfcRadioButton.getContentDescription();
        nfcRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        nfcRadioButton.setContentDescription("");
        setupContentDescriptions(nfcRadioButton, nfcContentDescription);

        postDelayed(() -> {
            mobileIdRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            mobileIdRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_mobile_id, 1, 4));

            idCardRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            idCardRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_id_card, 3, 4));

            nfcRadioButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            nfcRadioButton.setContentDescription(getResources().getString(R.string.signature_update_signature_selected_method_nfc, 4, 4));
        }, 3500);
    }

    private String getCountryViewAccessibilityText() {
        Object selectedCountry = countryView.getSelectedItem();
        if (selectedCountry != null) {
            return countryViewLabel.getText().toString() + " " + selectedCountry;
        }
        return countryViewLabel.getText().toString();
    }

    private void setAccessibilityDescription() {
        personalCodeView.setContentDescription(String.format("%s, %s", getResources().getString(R.string.signature_update_mobile_id_personal_code),
                AccessibilityUtils.getTextViewAccessibility(personalCodeView)));
        AccessibilityUtils.setSingleCharactersContentDescription(personalCodeView,
                getResources().getString(R.string.signature_update_mobile_id_personal_code));
        AccessibilityUtils.setEditTextCursorToEnd(personalCodeView);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (countryView.getSelectedItemPosition() == 2) {
            setSmartIdPersonalCodeHandler(personalCodeView, personalCodeHandler);
        } else {
            removeTextWatcher(personalCodeView, personalCodeHandler);
        }

        // Better support for Voice Assist to not delete wrong characters
        accessibilityTouchExplorationStateChangeListener = AccessibilityUtils.addAccessibilityStateChanged(enabled -> {
            boolean isTalkBackEnabled = AccessibilityUtils.isTalkBackEnabled();
            if (isTalkBackEnabled) {
                setAccessibilityDescription();
            } else {
                AccessibilityUtils.setJoinedCharactersContentDescription(personalCodeView);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        removeTextWatcher(personalCodeView, personalCodeTextWatcher);
        removeTextWatcher(personalCodeView, personalCodeHandler);
        removeAccessibilityStateChanged(accessibilityTouchExplorationStateChangeListener);
    }
}
