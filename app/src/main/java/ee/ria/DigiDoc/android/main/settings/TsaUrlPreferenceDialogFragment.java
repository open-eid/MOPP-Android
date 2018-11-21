package ee.ria.DigiDoc.android.main.settings;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.EditText;

import com.takisoft.fix.support.v7.preference.EditTextPreferenceDialogFragmentCompat;

public class TsaUrlPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText editText = getTsaUrlPreference().getEditText();
        ViewGroup parent = (ViewGroup) editText.getParent();
        CheckBox checkBox = getTsaUrlPreference().getCheckBox();
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editText.setEnabled(!isChecked);
            if (isChecked) {
                editText.setText(null);
            }
        });
        checkBox.setChecked(TextUtils.isEmpty(getTsaUrlPreference().getText()));

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

    private TsaUrlPreference getTsaUrlPreference() {
        return (TsaUrlPreference) this.getPreference();
    }
}
