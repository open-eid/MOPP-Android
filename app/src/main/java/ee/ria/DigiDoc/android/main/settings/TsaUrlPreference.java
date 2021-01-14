package ee.ria.DigiDoc.android.main.settings;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.CheckBox;

import com.takisoft.fix.support.v7.preference.EditTextPreference;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

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
        ConfigurationProvider configurationProvider = ((Application) context.getApplicationContext()).getConfigurationProvider();
        getEditText().setHint(configurationProvider.getTsaUrl());
        checkBox = new AppCompatCheckBox(context);
        checkBox.setId(android.R.id.checkbox);
        checkBox.setText(R.string.main_settings_tsa_url_use_default);
        checkBox.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, context.getResources().getDisplayMetrics()));
        checkBox.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120f, context.getResources().getDisplayMetrics()));

        setOnPreferenceChangeListener((preference, newValue) -> {
            AccessibilityUtils.sendAccessibilityEvent(context, TYPE_ANNOUNCEMENT, R.string.setting_value_changed);
            return true;
        });
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
