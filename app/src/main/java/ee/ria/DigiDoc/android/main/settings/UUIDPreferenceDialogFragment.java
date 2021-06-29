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

package ee.ria.DigiDoc.android.main.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.EditText;

import com.takisoft.fix.support.v7.preference.EditTextPreferenceDialogFragmentCompat;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

public class UUIDPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        UUIDPreference uuidPreference = getUUIDPreference();
        if (uuidPreference != null) {
            EditText editText = uuidPreference.getEditText();
            ViewGroup parent = (ViewGroup) editText.getParent();
            CheckBox checkBox = uuidPreference.getCheckBox();
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                editText.setEnabled(!isChecked);
                if (isChecked) {
                    editText.setText(null);
                }
            });
            checkBox.setChecked(TextUtils.isEmpty(uuidPreference.getText()));

            View oldCheckBox = parent.findViewById(checkBox.getId());
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
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        SecureUtil.markAsSecure(dialog.getWindow());
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
    }
}
