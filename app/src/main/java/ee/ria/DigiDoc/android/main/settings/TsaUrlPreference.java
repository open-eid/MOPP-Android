package ee.ria.DigiDoc.android.main.settings;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.takisoft.fix.support.v7.preference.EditTextPreference;

import ee.ria.DigiDoc.R;

public class TsaUrlPreference extends EditTextPreference {

    private final CheckBox checkBox;

    public TsaUrlPreference(Context context) {
        this(context, null);
    }

    public TsaUrlPreference(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.editTextPreferenceStyle);
    }

    public TsaUrlPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TsaUrlPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        checkBox = new AppCompatCheckBox(context);
        checkBox.setId(android.R.id.checkbox);
        checkBox.setText(R.string.main_settings_tsa_url_use_default);
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    @Override
    public CharSequence getSummary() {
        String text = getText();
        return TextUtils.isEmpty(text) ? getEditText().getHint() : text;
    }
}
