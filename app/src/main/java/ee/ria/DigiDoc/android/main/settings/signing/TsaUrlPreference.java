package ee.ria.DigiDoc.android.main.settings.signing;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.preference.PreferenceViewHolder;

import com.takisoft.preferencex.EditTextPreference;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;

public class TsaUrlPreference extends EditTextPreference {

    private final CheckBox checkBox;
    private final ConfigurationProvider configurationProvider;

    private PreferenceViewHolder holder;

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

        float sizePreference = 48f;
        int sizeDisplayMetrics = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizePreference, context.getResources().getDisplayMetrics());

        checkBox = new AppCompatCheckBox(context);
        checkBox.setId(android.R.id.checkbox);
        checkBox.setText(R.string.main_settings_tsa_url_use_default);
        checkBox.setMinHeight(sizeDisplayMetrics);
        checkBox.setX(sizePreference);
        checkBox.setPadding(checkBox.getPaddingLeft(), checkBox.getPaddingTop(), sizeDisplayMetrics, checkBox.getPaddingBottom());

        setViewId(R.id.mainSettingsAccessToTimeStampingService);

        setOnPreferenceChangeListener((preference, newValue) -> {
            if (holder != null) {
                setPreferenceContentDescription(holder);
            }
            SettingsSigningView.setTsaCertificateViewVisibleValue(!checkBox.isChecked());
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

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        this.holder = holder;

        setPreferenceContentDescription(holder);
    }

    private void setPreferenceContentDescription(PreferenceViewHolder holder) {
        if (AccessibilityUtils.isTalkBackEnabled()) {
            String buttonLabel = AccessibilityUtils.getButtonTranslation();

            TextView summary = (TextView) holder.findViewById(android.R.id.summary);
            if (summary != null) {
                summary.setContentDescription(buttonLabel);
            }
        }
    }
}
