package ee.ria.DigiDoc.android.main.settings.access;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxRadioGroup.checkedChanges;
import static com.jakewharton.rxbinding4.widget.RxTextView.textChanges;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.access.siva.SivaSetting.DEFAULT;
import static ee.ria.DigiDoc.android.main.settings.access.siva.SivaSetting.MANUAL;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarImageButton;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarTextView;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_SIVA_CERT;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_TSA_CERT;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;
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
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.main.settings.access.siva.SivaSetting;
import ee.ria.DigiDoc.android.main.settings.create.CertificateAddViewModel;
import ee.ria.DigiDoc.android.main.settings.create.ChooseFileScreen;
import ee.ria.DigiDoc.android.main.settings.create.ViewState;
import ee.ria.DigiDoc.android.signature.detail.CertificateDetailScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

public final class SettingsAccessView extends CoordinatorLayout {

    private final AppBarLayout appBarLayout;
    private final ScrollView scrollView;
    private final Toolbar toolbarView;
    private final LinearLayout tsaCertContainer;
    private final TextView tsaCertIssuedTo;
    private final TextView tsaCertValidTo;
    private final Button addCertificateButton;
    private final Button showCertificateButton;
    private X509Certificate tsaCertificate;

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

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;
    private final ConfigurationProvider configurationProvider;

    private final ViewDisposables disposables;
    private final CertificateAddViewModel viewModel;

    private static boolean isTsaCertificateViewVisible;
    private static final Subject<Boolean> isTsaCertificateViewVisibleSubject = PublishSubject.create();

    private String previousSivaUrl = "";

    String viewId = String.valueOf(View.generateViewId());

    public SettingsAccessView(Context context) {
        this(context, null);
    }

    public SettingsAccessView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsAccessView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        viewModel = ApplicationApp.component(context).navigator()
                .viewModel(viewId, CertificateAddViewModel.class);
        inflate(context, R.layout.main_settings_access, this);
        toolbarView = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBar);
        scrollView = findViewById(R.id.scrollView);
        navigator = ApplicationApp.component(context).navigator();
        settingsDataStore = ApplicationApp.component(context).settingsDataStore();
        configurationProvider = ((ApplicationApp) context.getApplicationContext()).getConfigurationProvider();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_settings_access_button);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        tsaCertContainer = findViewById(R.id.mainSettingsTsaCertificateContainer);
        tsaCertIssuedTo = findViewById(R.id.mainSettingsTsaCertificateIssuedTo);
        tsaCertValidTo = findViewById(R.id.mainSettingsTsaCertificateValidTo);
        addCertificateButton = findViewById(R.id.mainSettingsTsaCertificateAddCertificateButton);
        showCertificateButton = findViewById(R.id.mainSettingsTsaCertificateShowCertificateButton);

        addCertificateButton.setContentDescription(addCertificateButton.getText().toString().toLowerCase());
        showCertificateButton.setContentDescription(showCertificateButton.getText().toString().toLowerCase());
        
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

        if (settingsDataStore != null) {
            isTsaCertificateViewVisible = settingsDataStore.getIsTsaCertificateViewVisible();
            setTSAContainerViewVisibility(isTsaCertificateViewVisible);
        }

        Activity activityContext = (Activity) this.getContext();
        SwitchCompat openAllFileTypesSwitch = findViewById(R.id.mainSettingsOpenAllFileTypes);
        if (openAllFileTypesSwitch != null && activityContext != null) {
            openAllFileTypesSwitch.setChecked(activityContext.getSettingsDataStore().getIsOpenAllFileTypesEnabled());

            openAllFileTypesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                activityContext.getSettingsDataStore().setIsOpenAllFileTypesEnabled(isChecked);
                restartIntent();
            });
        }

        SwitchCompat allowScreenshotsSwitch = findViewById(R.id.mainSettingsAllowScreenshots);
        if (allowScreenshotsSwitch != null && activityContext != null) {
            allowScreenshotsSwitch.setChecked(activityContext.getSettingsDataStore().getIsScreenshotAllowed());

            allowScreenshotsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                activityContext.getSettingsDataStore().setIsScreenshotAllowed(isChecked);
                restartIntent();
            });
        }

        checkSivaServiceSetting(settingsDataStore, configurationProvider.getSivaUrl());
    }

    private void restartIntent() {
        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getContext().getPackageName());
        assert intent != null;
        ComponentName componentName = intent.getComponent();
        Intent restartIntent = Intent.makeRestartActivityTask(componentName);
        restartIntent.setAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getContext().startActivity(restartIntent);
    }

    public void render(ViewState state) {
        if (settingsDataStore != null) {
            String tsaCertName = settingsDataStore.getTSACertName();
            File tsaFile = FileUtil.getCertFile(getContext(), tsaCertName, DIR_TSA_CERT);

            if (tsaFile != null) {
                String fileContents = FileUtils.readFileContent(tsaFile.getPath());
                try {
                    tsaCertificate = CertificateUtil.x509Certificate(fileContents);
                    X509CertificateHolder certificateHolder = new JcaX509CertificateHolder(tsaCertificate);
                    String issuer = getIssuer(certificateHolder);
                    tsaCertIssuedTo.setText(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title),
                            issuer));
                    tsaCertIssuedTo.setContentDescription(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title),
                            issuer).toLowerCase());
                    tsaCertValidTo.setText(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title),
                            getFormattedDateTime(certificateHolder.getNotAfter())));
                    tsaCertValidTo.setContentDescription(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title),
                            getFormattedDateTime(certificateHolder.getNotAfter())).toLowerCase());
                } catch (CertificateException e) {
                    Timber.log(Log.ERROR, e, "Unable to get TSA certificate");

                    // Remove invalid files
                    removeCertificate(tsaFile, settingsDataStore);

                    tsaCertIssuedTo.setText(getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title));
                    tsaCertValidTo.setText(getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title));
                }
            }

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
                            getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title),
                            issuer));
                    sivaCertificateValidTo.setText(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title),
                            getFormattedDateTime(certificateHolder.getNotAfter())));
                } catch (CertificateException e) {
                    Timber.log(Log.ERROR, e, "Unable to get SiVa certificate");

                    // Remove invalid files
                    removeSivaCert(settingsDataStore, sivaCertificateIssuedTo, sivaCertificateValidTo);
                }
            }
        }
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

    private void saveSivaUrl(String sivaServiceUrl) {
        Optional<String> sivaUrl = Optional.ofNullable(sivaServiceUrl)
                .filter(text -> !text.isEmpty());
        if (sivaUrl.isPresent()) {
            setSivaUrl(settingsDataStore, sivaUrl.get().trim());
        } else {
            String defaultSivaUrl = configurationProvider.getSivaUrl();
            setSivaUrl(settingsDataStore, defaultSivaUrl);
            setSivaPlaceholderText(defaultSivaUrl);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        scrollView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        TextView toolbarTextView = getToolbarTextView(toolbarView);
        if (toolbarTextView != null) {
            toolbarTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        ImageButton toolbarImageButton = getToolbarImageButton(toolbarView);
        if (toolbarImageButton != null) {
            toolbarImageButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        appBarLayout.postDelayed(() -> {
            scrollView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            if (toolbarImageButton != null) {
                toolbarImageButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }, 1000);

        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o -> navigator.execute(Transaction.pop())));
        disposables.add(clicks(showCertificateButton).subscribe(o -> {
            if (tsaCertificate != null) {
                navigator.execute(Transaction.push(CertificateDetailScreen.create(tsaCertificate)));
            }
        }));
        disposables.add(clicks(addCertificateButton).subscribe(o ->
                navigator.execute(Transaction.push(ChooseFileScreen.create(true, false)))));
        disposables.add(observeTsaCertificateViewVisibleChanges().subscribe(isVisible -> {
            settingsDataStore.setIsTsaCertificateViewVisible(isVisible);
            setTSAContainerViewVisibility(isVisible);
        }));
        disposables.add(checkedChanges(sivaServiceChoiceGroup).subscribe(setting ->
                setSivaServiceSetting(settingsDataStore, configurationProvider.getSivaUrl(), setting)));
        disposables.add(clicks(sivaCertificateAddCertificateButton).subscribe(o ->
                navigator.execute(Transaction.push(ChooseFileScreen.create(false, true)))));
        disposables.add(clicks(sivaCertificateShowCertificateButton).subscribe(o -> {
            if (sivaCertificate != null) {
                navigator.execute(Transaction.push(CertificateDetailScreen.create(sivaCertificate)));
            }
        }));
        disposables.add(textChanges(sivaServiceUrl).subscribe(text -> {
            SivaSetting currentSivaSetting = getSivaSetting(settingsDataStore);
            if (currentSivaSetting == MANUAL && text.toString().isEmpty() && !previousSivaUrl.isEmpty()) {
                removeSivaCert(settingsDataStore, sivaCertificateIssuedTo, sivaCertificateValidTo);
            }
            previousSivaUrl = text.toString();
        }));
        disposables.add(viewModel.viewStates().subscribe(this::render));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        SivaSetting currentSivaSetting = getSivaSetting(settingsDataStore);
        Editable sivaUrl = sivaServiceUrl.getText();
        if (sivaUrl != null) {
            saveSivaUrl(currentSivaSetting == DEFAULT ?
                    configurationProvider.getSivaUrl() : sivaUrl.toString());
        } else {
            saveSivaUrl(configurationProvider.getSivaUrl());
        }
        super.onDetachedFromWindow();
    }

    public static void setTsaCertificateViewVisibleValue(boolean value) {
        isTsaCertificateViewVisible = value;
        isTsaCertificateViewVisibleSubject.onNext(value);
    }

    public static Observable<Boolean> observeTsaCertificateViewVisibleChanges() {
        return isTsaCertificateViewVisibleSubject;
    }

    public static void resetSettings(Context context, SettingsDataStore settingsDataStore) {
        settingsDataStore.setUuid("");
        settingsDataStore.setTsaUrl("");
        settingsDataStore.setIsOpenAllFileTypesEnabled(true);
        settingsDataStore.setIsScreenshotAllowed(false);
        File certFile = FileUtil.getCertFile(context, settingsDataStore.getTSACertName(), DIR_SIVA_CERT);
        removeCertificate(certFile, settingsDataStore);
        setTsaCertificateViewVisibleValue(false);
    }

    private static void removeCertificate(File tsaFile, SettingsDataStore settingsDataStore) {
        if (tsaFile != null) {
            FileUtils.removeFile(tsaFile.getPath());
        }
        settingsDataStore.setTSACertName(null);
    }

    private void setTSAContainerViewVisibility(boolean isVisible) {
        tsaCertContainer.setVisibility(!isVisible ? GONE : VISIBLE);
    }

    private SivaSetting getSivaSetting(SettingsDataStore settingsDataStore) {
        if (settingsDataStore != null) {
            return settingsDataStore.getSivaSetting();
        }
        return DEFAULT;
    }

    private void checkSivaServiceSetting(SettingsDataStore settingsDataStore,
                                         String defaultSivaUrl) {
        SivaSetting currentSivaSetting = getSivaSetting(settingsDataStore);
        switch (currentSivaSetting) {
            case DEFAULT -> {
                String trimmedDefaultSivaUrl = defaultSivaUrl.trim();
                setSivaPlaceholderText(trimmedDefaultSivaUrl);
                setSivaUrl(settingsDataStore, trimmedDefaultSivaUrl);
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
                    setSivaUrl(settingsDataStore, defaultSivaUrl);
                    setSivaPlaceholderText(defaultSivaUrl);
                }
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

    private void removeSivaCert(SettingsDataStore settingsDataStore, TextView issuedTo, TextView validTo) {
        String sivaCertName = settingsDataStore.getSivaCertName();
        File sivaFile = FileUtil.getCertFile(getContext(), sivaCertName, DIR_SIVA_CERT);

        if (sivaFile != null) {
            FileUtils.removeFile(sivaFile.getPath());
        }
        settingsDataStore.setSivaCertName(null);

        issuedTo.setText(getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title));
        validTo.setText(getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title));
    }
}
