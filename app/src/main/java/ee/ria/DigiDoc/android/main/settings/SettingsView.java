package ee.ria.DigiDoc.android.main.settings;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_TSA_CERT;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

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

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.main.settings.create.ChooseFileScreen;
import ee.ria.DigiDoc.android.main.settings.create.TSACertificateAddViewModel;
import ee.ria.DigiDoc.android.main.settings.create.ViewState;
import ee.ria.DigiDoc.android.signature.detail.CertificateDetailScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.CertificateUtil;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.configuration.util.FileUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

public final class SettingsView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final TextView toolbarTitleView;
    private final LinearLayout tsaCertContainer;
    private final TextView tsaCertIssuedTo;
    private final TextView tsaCertValidTo;
    private final Button addCertificateButton;
    private final Button showCertificateButton;
    private X509Certificate tsaCertificate;

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;

    private final ViewDisposables disposables;
    private final TSACertificateAddViewModel viewModel;

    private static boolean isTsaCertificateViewVisible;
    private static final Subject<Boolean> isTsaCertificateViewVisibleSubject = PublishSubject.create();

    String viewId = String.valueOf(View.generateViewId());

    public SettingsView(Context context) {
        this(context, null);
    }

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        viewModel = Application.component(context).navigator()
                .viewModel(viewId, TSACertificateAddViewModel.class);
        inflate(context, R.layout.main_settings, this);
        toolbarView = findViewById(R.id.toolbar);
        toolbarTitleView = getToolbarViewTitle();
        navigator = Application.component(context).navigator();
        settingsDataStore = Application.component(context).settingsDataStore();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_settings_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        if (toolbarTitleView != null) {
            toolbarTitleView.setContentDescription("\u202F");
        }

        tsaCertContainer = findViewById(R.id.mainSettingsTsaCertificateContainer);
        tsaCertIssuedTo = findViewById(R.id.mainSettingsTsaCertificateIssuedTo);
        tsaCertValidTo = findViewById(R.id.mainSettingsTsaCertificateValidTo);
        addCertificateButton = findViewById(R.id.mainSettingsTsaCertificateAddCertificateButton);
        showCertificateButton = findViewById(R.id.mainSettingsTsaCertificateShowCertificateButton);

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
    }

    private TextView getToolbarViewTitle() {
        for (int i = 0; i < toolbarView.getChildCount(); i++) {
            View childView = toolbarView.getChildAt(i);
            if (childView instanceof TextView) {
                return (TextView) childView;
            }
        }

        return null;
    }

    private void restartIntent() {
        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getContext().getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent restartIntent = Intent.makeRestartActivityTask(componentName);
        restartIntent.setAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getContext().startActivity(restartIntent);
    }

    public void render(ViewState state) {
        if (settingsDataStore != null) {
            String tsaCertName = settingsDataStore.getTSACertName();

            File tsaFile = FileUtil.getTSAFile(getContext(), tsaCertName);

            if (tsaFile != null) {
                String fileContents = FileUtils.readFileContent(tsaFile.getPath());
                try {
                    tsaCertificate = CertificateUtil.x509Certificate(fileContents);
                    X509CertificateHolder certificateHolder = new JcaX509CertificateHolder(tsaCertificate);
                    String issuer = getIssuer(certificateHolder);
                    tsaCertIssuedTo.setText(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title),
                            issuer));
                    tsaCertValidTo.setText(String.format("%s %s",
                            getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title),
                            getFormattedDateTime(certificateHolder.getNotAfter())));
                } catch (CertificateException e) {
                    Timber.log(Log.ERROR, e, "Unable to get TSA certificate");

                    // Remove invalid files
                    FileUtils.removeFile(tsaFile.getPath());
                    settingsDataStore.setTSACertName(null);

                    tsaCertIssuedTo.setText(getResources().getText(R.string.main_settings_timestamp_cert_issued_to_title));
                    tsaCertValidTo.setText(getResources().getText(R.string.main_settings_timestamp_cert_valid_to_title));
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

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
        disposables.add(clicks(showCertificateButton).subscribe(o -> {
            if (tsaCertificate != null) {
                navigator.execute(Transaction.push(CertificateDetailScreen.create(tsaCertificate)));
            }
        }));
        disposables.add(clicks(addCertificateButton).subscribe(o ->
                navigator.execute(Transaction.push(ChooseFileScreen.create()))));
        disposables.add(observeTsaCertificateViewVisibleChanges().subscribe(isVisible -> {
            settingsDataStore.setIsTsaCertificateViewVisible(isVisible);
            setTSAContainerViewVisibility(isVisible);
        }));
        disposables.add(viewModel.viewStates().subscribe(this::render));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    public static void setTsaCertificateViewVisibleValue(boolean value) {
        isTsaCertificateViewVisible = value;
        isTsaCertificateViewVisibleSubject.onNext(value);
    }

    public static Observable<Boolean> observeTsaCertificateViewVisibleChanges() {
        return isTsaCertificateViewVisibleSubject;
    }

    private void setTSAContainerViewVisibility(boolean isVisible) {
        tsaCertContainer.setVisibility(!isVisible ? GONE : VISIBLE);
    }
}
