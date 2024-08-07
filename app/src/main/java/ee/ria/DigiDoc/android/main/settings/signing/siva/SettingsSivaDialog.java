package ee.ria.DigiDoc.android.main.settings.signing.siva;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.View.inflate;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxRadioGroup.checkedChanges;
import static com.jakewharton.rxbinding4.widget.RxTextView.textChanges;
import static ee.ria.DigiDoc.android.accessibility.AccessibilityUtils.setTextViewContentDescription;
import static ee.ria.DigiDoc.android.main.settings.signing.siva.SivaSetting.DEFAULT;
import static ee.ria.DigiDoc.android.main.settings.signing.siva.SivaSetting.MANUAL;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_SIVA_CERT;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.main.settings.create.CertificateAddViewModel;
import ee.ria.DigiDoc.android.main.settings.create.ChooseFileScreen;
import ee.ria.DigiDoc.android.main.settings.create.ViewState;
import ee.ria.DigiDoc.android.signature.detail.CertificateDetailScreen;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import timber.log.Timber;

public class SettingsSivaDialog extends Dialog {

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;
    private final ConfigurationProvider configurationProvider;

    private final ViewDisposables disposables;
    private final CertificateAddViewModel viewModel;

    private final String viewId = String.valueOf(View.generateViewId());

    private TextWatcher sivaUrlTextWatcher;

    private final ImageButton backButton;
    private final RadioGroup sivaServiceChoiceGroup;
    private final RadioButton sivaServiceDefaultChoice;
    private final RadioButton sivaServiceManualChoice;
    private final TextInputLayout sivaServiceUrlLayout;
    private final TextInputEditText sivaServiceUrl;
    private final TextView sivaCertificateIssuedTo;
    private final TextView sivaCertificateValidTo;

    private final LinearLayout sivaServiceCertificateContainer;
    private final Button sivaCertificateAddCertificateButton;
    private final Button sivaCertificateShowCertificateButton;
    private X509Certificate sivaCertificate;

    private String previousSivaUrl = "";

    public SettingsSivaDialog(Context context) {
        super(context);

        Window window = getWindow();
        if (window != null) {
            SecureUtil.markAsSecure(context, window);
        }

        setContentView(R.layout.main_settings_siva_dialog_layout);

        viewModel = ApplicationApp.component(context).navigator()
                .viewModel(viewId, CertificateAddViewModel.class);
        inflate(context, R.layout.main_settings_siva_dialog_layout, null);

        navigator = ApplicationApp.component(context).navigator();
        settingsDataStore = ApplicationApp.component(context).settingsDataStore();
        configurationProvider = ((ApplicationApp) context.getApplicationContext()).getConfigurationProvider();

        disposables = new ViewDisposables();

        backButton = findViewById(R.id.mainSettingsSivaBackButton);
        backButton.requestFocus();
        sivaServiceChoiceGroup = findViewById(R.id.mainSettingsSivaServiceChoiceGroup);
        sivaServiceDefaultChoice = findViewById(R.id.mainSettingsSivaServiceDefaultChoice);
        sivaServiceManualChoice = findViewById(R.id.mainSettingsSivaServiceManualChoice);
        sivaServiceUrlLayout = findViewById(R.id.mainSettingsSivaServiceUrlLayout);
        sivaServiceUrl = findViewById(R.id.mainSettingsSivaServiceUrl);
        sivaCertificateIssuedTo = findViewById(R.id.mainSettingsSivaCertificateIssuedTo);
        sivaCertificateValidTo = findViewById(R.id.mainSettingsSivaCertificateValidTo);

        sivaServiceCertificateContainer = findViewById(R.id.mainSettingsSivaServiceCertificateContainer);
        sivaCertificateAddCertificateButton = findViewById(R.id.mainSettingsSivaCertificateAddCertificateButton);
        sivaCertificateShowCertificateButton = findViewById(R.id.mainSettingsSivaCertificateShowCertificateButton);

        sivaCertificateAddCertificateButton.setContentDescription(sivaCertificateAddCertificateButton.getText().toString().toLowerCase());
        sivaCertificateShowCertificateButton.setContentDescription(sivaCertificateShowCertificateButton.getText().toString().toLowerCase());

        if (sivaServiceUrl.getText() != null) {
            previousSivaUrl = sivaServiceUrl.getText().toString();
        }

        checkSivaServiceSetting(settingsDataStore, configurationProvider.getSivaUrl());

        if (AccessibilityUtils.isTalkBackEnabled()) {
            handleSivaUrlContentDescription();
            AccessibilityUtils.setEditTextCursorToEnd(sivaServiceUrl);
            sivaServiceUrl.setOnClickListener(v -> setTextViewContentDescription(context, false, null, context.getString(R.string.main_settings_siva_service_title), sivaServiceUrl));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AccessibilityUtils.setEditTextCursorToEnd(sivaServiceUrl);

        sivaUrlTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sivaServiceUrl.setSingleLine(sivaServiceUrl.getText() != null && sivaServiceUrl.getText().length() != 0);
                if (AccessibilityUtils.isTalkBackEnabled()) {
                    handleSivaUrlContentDescription();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (sivaServiceUrl.getText() != null) {
                    AccessibilityUtils.setEditTextCursorToEnd(sivaServiceUrl);
                }
            }
        };

        sivaServiceUrl.addTextChangedListener(sivaUrlTextWatcher);

        updateData(settingsDataStore);
    }

    private void updateData(SettingsDataStore settingsDataStore) {
        if (sivaServiceUrl.getText() != null) {
            previousSivaUrl = sivaServiceUrl.getText().toString();
        }

        String sivaCertName = settingsDataStore.getSivaCertName();
        File sivaFile = FileUtil.getCertFile(getContext(), sivaCertName, DIR_SIVA_CERT);

        if (sivaFile != null) {
            String fileContents = FileUtils.readFileContent(sivaFile.getPath());
            try {
                sivaCertificate = CertificateUtil.x509Certificate(fileContents);
                X509CertificateHolder certificateHolder = new JcaX509CertificateHolder(sivaCertificate);
                String issuer = getIssuer(certificateHolder);
                sivaCertificateIssuedTo.setText(String.format("%s %s",
                        navigator.activity().getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title),
                        issuer));
                sivaCertificateValidTo.setText(String.format("%s %s",
                        navigator.activity().getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title),
                        getFormattedDateTime(certificateHolder.getNotAfter())));
            } catch (CertificateException e) {
                Timber.log(Log.ERROR, e, "Unable to get SiVa certificate");

                // Remove invalid files
                removeSivaCert(getContext(), settingsDataStore);
                resetCertificateInfo(getContext());
            }
        }
    }

    public void render(ViewState state) {
        updateData(settingsDataStore);
    }

    private String getIssuer(X509CertificateHolder certificateHolder) {
        RDN[] organizationRDNs = certificateHolder.getIssuer().getRDNs(BCStyle.O);
        if (organizationRDNs.length > 0) {
            return organizationRDNs[0].getFirst().getValue().toString();
        }
        RDN[] organizationUnitRDNs = certificateHolder.getIssuer().getRDNs(BCStyle.OU);
        if (organizationUnitRDNs.length > 0) {
            return organizationUnitRDNs[0].getFirst().getValue().toString();
        }
        RDN[] commonNameRDNs = certificateHolder.getIssuer().getRDNs(BCStyle.CN);
        if (commonNameRDNs.length > 0) {
            return commonNameRDNs[0].getFirst().getValue().toString();
        }

        return "-";
    }

    private static String getFormattedDateTime(Date date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            if (date != null) {
                return dateFormat.format(date);
            }
        } catch (IllegalStateException e) {
            Timber.log(Log.ERROR, e, "Unable to format date");
        }
        return "-";
    }

    private SivaSetting getSivaSetting(SettingsDataStore settingsDataStore) {
        if (settingsDataStore != null) {
            return settingsDataStore.getSivaSetting();
        }
        return DEFAULT;
    }

    private void saveSivaUrl(String sivaServiceUrl) {
        Optional<String> sivaUrl = Optional.ofNullable(sivaServiceUrl)
                .filter(text -> !text.isEmpty());
        if (sivaUrl.isPresent()) {
            setSivaUrl(settingsDataStore, sivaUrl.get().trim());
        } else {
            setSivaUrl(settingsDataStore, "");
            setSivaPlaceholderText(configurationProvider.getSivaUrl());
        }
    }

    private void checkSivaServiceSetting(SettingsDataStore settingsDataStore,
                                         String defaultSivaUrl) {
        SivaSetting currentSivaSetting = getSivaSetting(settingsDataStore);
        switch (currentSivaSetting) {
            case DEFAULT -> {
                setSivaPlaceholderText(defaultSivaUrl.trim());
                setSivaUrl(settingsDataStore, "");
                sivaServiceDefaultChoice.setChecked(true);
                isSivaCertViewShown(GONE);
                setSivaUrlEnabled(false);
            }
            case MANUAL -> {
                sivaServiceManualChoice.setChecked(true);
                isSivaCertViewShown(VISIBLE);
                setSivaUrlEnabled(true);
                String manualSivaUrl = settingsDataStore.getSivaUrl();
                if (!manualSivaUrl.isEmpty() && !defaultSivaUrl.equals(manualSivaUrl)) {
                    sivaServiceUrl.setText(manualSivaUrl.trim());
                } else {
                    setSivaUrl(settingsDataStore, "");
                    setSivaPlaceholderText(defaultSivaUrl);
                }
                sivaServiceUrl.setOnFocusChangeListener((view, hasFocus) -> {
                    if (hasFocus) {
                        AccessibilityUtils.setEditTextCursorToEnd(sivaServiceUrl);
                    }
                });
            }
        }
    }

    private void isSivaCertViewShown(int visibility) {
        sivaServiceCertificateContainer.setVisibility(visibility);
    }

    private void setSivaServiceSetting(SettingsDataStore settingsDataStore,
                                       String defaultSivaUrl, int buttonId) {
        if (settingsDataStore != null) {
            if (sivaServiceDefaultChoice.getId() == buttonId) {
                settingsDataStore.setSivaSetting(DEFAULT);
            } else if (sivaServiceManualChoice.getId() == buttonId) {
                settingsDataStore.setSivaSetting(MANUAL);
            }

            checkSivaServiceSetting(settingsDataStore, defaultSivaUrl);
        }
    }

    private void setSivaUrlEnabled(boolean isEnabled) {
        sivaServiceUrl.setEnabled(isEnabled);
    }

    private void setSivaUrl(SettingsDataStore settingsDataStore, String sivaUrl) {
        if (settingsDataStore != null) {
            settingsDataStore.setSivaUrl(sivaUrl);
        }
    }

    private void setSivaPlaceholderText(String text) {
        sivaServiceUrlLayout.setPlaceholderText(text);
    }

    private void resetCertificateInfo(Context context) {
        sivaCertificateIssuedTo.setText(context.getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title));
        sivaCertificateValidTo.setText(context.getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title));
    }

    private static void removeSivaCert(Context context, SettingsDataStore settingsDataStore) {
        String sivaCertName = settingsDataStore.getSivaCertName();
        File sivaFile = FileUtil.getCertFile(context, sivaCertName, DIR_SIVA_CERT);

        if (sivaFile != null) {
            FileUtils.removeFile(sivaFile.getPath());
        }
        settingsDataStore.setSivaCertName(null);
    }

    private void handleSivaUrlContentDescription() {
        Editable sivaUrlEditable = sivaServiceUrl.getText();
        if (sivaUrlEditable != null) {
            String sivaUrl = sivaUrlEditable.toString();
            AccessibilityUtils.setContentDescription(sivaServiceUrl, String.format("%s %s",
                    getContext().getString(R.string.main_settings_siva_service_title),
                    sivaUrl.isEmpty() ? sivaServiceUrlLayout.getPlaceholderText() : sivaUrl));
        }
    }

    public static void resetSettings(Context context, SettingsDataStore settingsDataStore) {
        settingsDataStore.setSivaSetting(DEFAULT);
        settingsDataStore.setSivaUrl("");
        settingsDataStore.setSivaCertName(null);
        removeSivaCert(context, settingsDataStore);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        sivaServiceUrl.clearFocus();

        disposables.attach();
        disposables.add(clicks(backButton).subscribe(o -> dismiss()));
        disposables.add(checkedChanges(sivaServiceChoiceGroup).subscribe(setting ->
                setSivaServiceSetting(settingsDataStore, configurationProvider.getSivaUrl(), setting)));
        disposables.add(textChanges(sivaServiceUrl).subscribe(text -> {
            SivaSetting currentSivaSetting = getSivaSetting(settingsDataStore);
            if (currentSivaSetting == MANUAL && text.toString().isEmpty() && !previousSivaUrl.isEmpty()) {
                removeSivaCert(getContext(), settingsDataStore);
                resetCertificateInfo(getContext());
            }
            previousSivaUrl = text.toString();
        }));
        disposables.add(clicks(sivaCertificateAddCertificateButton).subscribe(o ->
                navigator.execute(Transaction.push(ChooseFileScreen.create(false, true)))));
        disposables.add(clicks(sivaCertificateShowCertificateButton).subscribe(o -> {
            if (sivaCertificate != null) {
                dismiss();
                navigator.execute(Transaction.push(CertificateDetailScreen.create(sivaCertificate)));
            }
        }));
        disposables.add(viewModel.viewStates().subscribe(this::render));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        SivaSetting currentSivaSetting = getSivaSetting(settingsDataStore);
        Editable sivaUrl = sivaServiceUrl.getText();
        if (sivaUrl != null) {
            saveSivaUrl(currentSivaSetting == DEFAULT ? "" : sivaUrl.toString());
        } else {
            saveSivaUrl(configurationProvider.getSivaUrl());
        }
        if (sivaUrlTextWatcher != null) {
            sivaServiceUrl.removeTextChangedListener(sivaUrlTextWatcher);
        }
        sivaServiceUrl.setOnClickListener(null);
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            updateData(settingsDataStore);
        }
    }
}
