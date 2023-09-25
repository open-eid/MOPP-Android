package ee.ria.DigiDoc.android.main.settings.access;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.CheckBox;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.takisoft.preferencex.EditTextPreference;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.main.settings.access.SettingsAccessView;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;

public class TsaUrlPreference extends EditTextPreference {

    private final CheckBox checkBox;
    private final ConfigurationProvider configurationProvider;

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
        configurationProvider = ((ApplicationApp) context.getApplicationContext()).getConfigurationProvider();
        checkBox = new AppCompatCheckBox(context);
        checkBox.setId(android.R.id.checkbox);
        checkBox.setText(R.string.main_settings_tsa_url_use_default);
        checkBox.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, context.getResources().getDisplayMetrics()));
        checkBox.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120f, context.getResources().getDisplayMetrics()));
        checkBox.setX(48f);

        setViewId(R.id.mainSettingsAccessToTimeStampingService);

        setOnPreferenceChangeListener((preference, newValue) -> {
            SettingsAccessView.setTsaCertificateViewVisibleValue(!checkBox.isChecked());
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
        return TextUtils.isEmpty(text) ? configurationProvider.getTsaUrl() : text;
    }
}
