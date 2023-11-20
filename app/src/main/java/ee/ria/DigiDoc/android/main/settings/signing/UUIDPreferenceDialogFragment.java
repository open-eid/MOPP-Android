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

package ee.ria.DigiDoc.android.main.settings.signing;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;

public class UUIDPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    private AppCompatEditText summary;
    private TextWatcher uuidTextWatcher;

    private void handleUuidUrlContentDescription(View view, CheckBox checkBox) {
        AppCompatEditText summaryView = view.findViewById(android.R.id.edit);
        if (AccessibilityUtils.isTalkBackEnabled() && getContext() != null) {
            AccessibilityUtils.setContentDescription(summaryView,
                    getContext().getString(R.string.main_settings_uuid_title));

            summaryView.setAccessibilityTraversalAfter(checkBox.getId());
            checkBox.setNextFocusDownId(summaryView.getId());
        }
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        UUIDPreference uuidPreference = getUUIDPreference();
        if (uuidPreference != null) {
            CheckBox checkBox = uuidPreference.getCheckBox();

            summary = view.findViewById(android.R.id.edit);

            handleUuidUrlContentDescription(view, checkBox);
            AppCompatEditText appCompatEditText = TextUtil.getEditText(view);
            AppCompatTextView appCompatTextView = TextUtil.getTextView(view);

            uuidPreference.setOnBindEditTextListener(editText -> {
                checkBox.setChecked(false);
                editText.setText(uuidPreference.getText());
            });

            if (appCompatEditText != null) {
                setAccessibilityForEditText(uuidPreference, appCompatEditText, appCompatTextView);
            }

            if (summary != null) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    summary.setEnabled(!isChecked);
                    if (isChecked) {
                        disableTextViewOnChecked(summary);
                    }
                });

                checkBox.setChecked(TextUtils.isEmpty(uuidPreference.getText()));

                ViewGroup parent = ((ViewGroup) summary.getParent());
                View oldCheckBox = summary.findViewById(checkBox.getId());
                if (oldCheckBox != null) {
                    parent.removeView(oldCheckBox);
                }
                ViewParent oldParent = checkBox.getParent();
                if (parent != oldParent) {
                    if (oldParent != null) {
                        ((ViewGroup) oldParent).removeView(checkBox);
                    }
                    parent.addView(checkBox, ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                summary.setSingleLine(true);
                summary.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                summary.setSelection(summary.getText() != null ?
                        summary.getText().length() : 0);

                uuidTextWatcher = new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        handleUuidUrlContentDescription(view, checkBox);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                };

                summary.addTextChangedListener(uuidTextWatcher);
            }
        }
    }

    private void setAccessibilityForEditText(
            UUIDPreference uuidPreference,
            AppCompatEditText appCompatEditText,
            AppCompatTextView appCompatTextView
    ) {
        appCompatTextView.setText(uuidPreference.getTitle());
        appCompatTextView.setLabelFor(appCompatEditText.getId());
        appCompatTextView.setVisibility(View.VISIBLE);
        appCompatTextView.setTextColor(Color.WHITE);
        appCompatTextView.setHeight(0);
    }

    private void disableTextViewOnChecked(AppCompatEditText appCompatEditText) {
        appCompatEditText.setText(null);
        appCompatEditText.setSingleLine(false);
        appCompatEditText.setHint("00000000-0000-0000-0000-000000000000");
        appCompatEditText.clearFocus();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = DisplayUtil.setCustomDialogSettings(super.onCreateDialog(savedInstanceState));
        SecureUtil.markAsSecure(getContext(), dialog.getWindow());
        return dialog;
    }

    private UUIDPreference getUUIDPreference() {
        return (UUIDPreference) this.getPreference();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult && getContext() != null) {
            AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.setting_value_change_cancelled);
        }
        summary.removeTextChangedListener(uuidTextWatcher);
    }
}
