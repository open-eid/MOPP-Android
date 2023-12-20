package ee.ria.DigiDoc.android.main.settings.signing;


import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;

public class TsaUrlPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    private AppCompatEditText appCompatEditText;
    private TextWatcher tsaUrlTextWatcher;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        TsaUrlPreference tsaUrlPreference = getTsaUrlPreference();
        if (tsaUrlPreference != null) {
            ConfigurationProvider configurationProvider = ((ApplicationApp) getContext().getApplicationContext()).getConfigurationProvider();
            CheckBox checkBox = tsaUrlPreference.getCheckBox();

            appCompatEditText = TextUtil.getTextView(view);

            tsaUrlPreference.setOnBindEditTextListener(editText -> {
                checkBox.setChecked(false);
                editText.setText(tsaUrlPreference.getText());
            });

            if (appCompatEditText != null) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    appCompatEditText.setEnabled(!isChecked);
                    if (isChecked) {
                        disableTextViewOnChecked(appCompatEditText, configurationProvider);
                    }
                });

                checkBox.setChecked(TextUtils.isEmpty(tsaUrlPreference.getText()));

                SettingsSigningView.setTsaCertificateViewVisibleValue(!checkBox.isChecked());

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
                            WRAP_CONTENT);
                }

                appCompatEditText.setSelection(appCompatEditText.getText() != null ? appCompatEditText.getText().length() : 0);

                tsaUrlTextWatcher = new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        appCompatEditText.setSingleLine(appCompatEditText.getText() != null && appCompatEditText.getText().length() != 0);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() == 1 && appCompatEditText.getText() != null) {
                            appCompatEditText.setSelection(appCompatEditText.getText().length());
                        }
                    }
                };

                appCompatEditText.addTextChangedListener(tsaUrlTextWatcher);
            }
        }
    }

    private void disableTextViewOnChecked(AppCompatEditText appCompatEditText,
                                          ConfigurationProvider configurationProvider) {
        appCompatEditText.setText(null);
        appCompatEditText.setHint(configurationProvider.getTsaUrl());
        appCompatEditText.clearFocus();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = DisplayUtil.setCustomDialogSettings(super.onCreateDialog(savedInstanceState));
        SecureUtil.markAsSecure(getContext(), dialog.getWindow());
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
        appCompatEditText.removeTextChangedListener(tsaUrlTextWatcher);
    }
}
