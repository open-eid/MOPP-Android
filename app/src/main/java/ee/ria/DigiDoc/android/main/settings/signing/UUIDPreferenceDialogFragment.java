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
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;

public class UUIDPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    private AppCompatEditText appCompatEditText;
    private TextWatcher uuidTextWatcher;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        UUIDPreference uuidPreference = getUUIDPreference();
        if (uuidPreference != null) {
            CheckBox checkBox = uuidPreference.getCheckBox();

            appCompatEditText = TextUtil.getTextView(view);

            uuidPreference.setOnBindEditTextListener(editText -> {
                checkBox.setChecked(false);
                editText.setText(uuidPreference.getText());
            });

            if (appCompatEditText != null) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    appCompatEditText.setEnabled(!isChecked);
                    if (isChecked) {
                        disableTextViewOnChecked(appCompatEditText);
                    }
                });

                checkBox.setChecked(TextUtils.isEmpty(uuidPreference.getText()));

                ViewGroup parent = ((ViewGroup) appCompatEditText.getParent());
                View oldCheckBox = appCompatEditText.findViewById(checkBox.getId());
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

                appCompatEditText.setSingleLine(true);
                appCompatEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                appCompatEditText.setSelection(appCompatEditText.getText() != null ?
                        appCompatEditText.getText().length() : 0);
            }
        }
    }

    private void disableTextViewOnChecked(AppCompatEditText appCompatEditText) {
        appCompatEditText.setText(null);
        appCompatEditText.setSingleLine(false);
        appCompatEditText.setHint("00000000-0000-0000-0000-000000000000");
        appCompatEditText.clearFocus();
    }

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
        appCompatEditText.removeTextChangedListener(uuidTextWatcher);
    }
}
