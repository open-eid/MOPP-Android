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

public class TsaUrlPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        TsaUrlPreference tsaUrlPreference = getTsaUrlPreference();
        if (tsaUrlPreference != null) {
            EditText editText = tsaUrlPreference.getEditText();
            ViewGroup parent = (ViewGroup) editText.getParent();
            CheckBox checkBox = tsaUrlPreference.getCheckBox();
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                editText.setEnabled(!isChecked);
                if (isChecked) {
                    editText.setText(null);
                }
            });
            checkBox.setChecked(TextUtils.isEmpty(tsaUrlPreference.getText()));

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

    private TsaUrlPreference getTsaUrlPreference() {
        return (TsaUrlPreference) this.getPreference();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult && getContext() != null) {
            AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.setting_value_change_cancelled);
        }
    }
}
