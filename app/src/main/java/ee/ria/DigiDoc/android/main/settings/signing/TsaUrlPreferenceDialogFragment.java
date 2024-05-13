package ee.ria.DigiDoc.android.main.settings.signing;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
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

import java.util.Optional;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.TextUtil;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;

public class TsaUrlPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

    private AppCompatEditText summary;
    private TextWatcher tsaUrlTextWatcher;

    private void handleTsaUrlContentDescription(View view, CheckBox checkBox) {
        AppCompatEditText summaryView = view.findViewById(android.R.id.edit);
        if (AccessibilityUtils.isTalkBackEnabled() && getContext() != null) {
            Optional<Editable> summaryEditable = Optional.ofNullable(summaryView.getText());
            summaryEditable.ifPresent(summaryText ->
                    AccessibilityUtils.setContentDescription(summaryView,
                            String.format("%s %s",
                                    getContext().getString(R.string.main_settings_tsa_url_title),
                                    summaryText)));

            summaryView.setAccessibilityTraversalAfter(checkBox.getId());
            checkBox.setNextFocusDownId(summaryView.getId());
        }
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        TsaUrlPreference tsaUrlPreference = getTsaUrlPreference();
        if (tsaUrlPreference != null) {
            ConfigurationProvider configurationProvider = ((ApplicationApp) requireContext().getApplicationContext()).getConfigurationProvider();
            CheckBox checkBox = tsaUrlPreference.getCheckBox();

            summary = view.findViewById(android.R.id.edit);

            handleTsaUrlContentDescription(view, checkBox);
            AppCompatEditText appCompatEditText = TextUtil.getEditText(view);
            AppCompatTextView appCompatTextView = TextUtil.getTextView(view);

            tsaUrlPreference.setOnBindEditTextListener(editText -> {
                checkBox.setChecked(false);
                editText.setText(tsaUrlPreference.getText());
            });

            if (appCompatEditText != null) {
                setAccessibilityForEditText(tsaUrlPreference, appCompatEditText, appCompatTextView);
                AccessibilityUtils.setTextViewContentDescription(true, configurationProvider.getTsaUrl(), appCompatTextView.getText().toString(), appCompatEditText);
            }

            if (summary != null) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    summary.setEnabled(!isChecked);
                    if (isChecked) {
                        disableTextViewOnChecked(summary, configurationProvider);
                    }
                });

                checkBox.setChecked(TextUtils.isEmpty(tsaUrlPreference.getText()));

                SettingsSigningView.setTsaCertificateViewVisibleValue(!checkBox.isChecked());

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
                            WRAP_CONTENT);
                }

                summary.setSelection(summary.getText() != null ? summary.getText().length() : 0);

                tsaUrlTextWatcher = new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        summary.setSingleLine(summary.getText() != null && summary.getText().length() != 0);
                        handleTsaUrlContentDescription(view, checkBox);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() == 1 && summary.getText() != null) {
                            summary.setSelection(summary.getText().length());
                        }
                    }
                };

                summary.addTextChangedListener(tsaUrlTextWatcher);
            }
        }
    }

    private void setAccessibilityForEditText(
            TsaUrlPreference tsaUrlPreference,
            AppCompatEditText appCompatEditText,
            AppCompatTextView appCompatTextView
    ) {
        AccessibilityUtils.setEditTextCursorToEnd(appCompatEditText);
        appCompatTextView.setText(tsaUrlPreference.getTitle());
        appCompatTextView.setLabelFor(appCompatEditText.getId());
        appCompatTextView.setVisibility(View.VISIBLE);
        appCompatTextView.setTextColor(Color.WHITE);
        appCompatTextView.setHeight(0);
    }

    private void disableTextViewOnChecked(AppCompatEditText appCompatEditText,
                                          ConfigurationProvider configurationProvider) {
        appCompatEditText.setText(null);
        appCompatEditText.setHint(configurationProvider.getTsaUrl());
        appCompatEditText.clearFocus();
    }

    @NonNull
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
        summary.removeTextChangedListener(tsaUrlTextWatcher);
    }
}
