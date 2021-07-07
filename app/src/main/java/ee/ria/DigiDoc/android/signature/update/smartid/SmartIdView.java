/*
 * app
 * Copyright 2017 - 2021 Riigi Infosüsteemi Amet
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

import android.content.Context;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.SignatureAddView;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.widget.RxTextView.afterTextChangeEvents;

public final class SmartIdView extends LinearLayout implements
        SignatureAddView<SmartIdRequest, SmartIdResponse> {

    private static final List<String> COUNTRY_LIST = Arrays.asList("EE", "LT", "LV");
    private final Subject<Object> positiveButtonStateSubject = PublishSubject.create();
    private final Spinner countryView;
    private final EditText personalCodeView;
    private final CheckBox rememberMeView;

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
        countryView = findViewById(R.id.signatureUpdateSmartIdCountry);
        personalCodeView = findViewById(R.id.signatureUpdateSmartIdPersonalCode);
        rememberMeView = findViewById(R.id.signatureUpdateSmartIdRememberMe);
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

        setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == VISIBLE) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(),
                        AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.signature_update_smart_id_message);
            }
        });
    }

    @Override
    public void reset(SignatureUpdateViewModel viewModel) {
        countryView.setSelection(COUNTRY_LIST.indexOf(viewModel.country()));
        setPersonalCodeViewFilters(countryView.getSelectedItemPosition());
        personalCodeView.setText(viewModel.sidPersonalCode());
        rememberMeView.setChecked(personalCodeView.getText().length() > 0);
    }

    @Override
    public SmartIdRequest request() {
        return SmartIdRequest.create(COUNTRY_LIST.get(countryView.getSelectedItemPosition()),
                personalCodeView.getText().toString(), rememberMeView.isChecked());
    }

    @Override
    public void response(@Nullable SmartIdResponse response, @Nullable RadioGroup methodView) {
    }

    public Observable<Object> positiveButtonState() {
        return Observable.merge(positiveButtonStateSubject, afterTextChangeEvents(personalCodeView));
    }

    public boolean positiveButtonEnabled() {
        return countryView.getSelectedItemPosition() != 0 || personalCodeView.getText().length() == 11;
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
        } else {
            if (pos != -1) {
                InputFilter[] copy = Arrays.copyOf(inputFilters, inputFilters.length - 1);
                System.arraycopy(inputFilters, pos + 1, copy, pos, inputFilters.length - (pos + 1));
                inputFilters = copy;
            }
        }
        personalCodeView.setFilters(inputFilters);
    }
}
