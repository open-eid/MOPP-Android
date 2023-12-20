package ee.ria.DigiDoc.android.main.settings.signing;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_SIVA_CERT;
import static ee.ria.DigiDoc.common.CommonConstants.DIR_TSA_CERT;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.jakewharton.rxbinding4.widget.RxCompoundButton;

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
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.main.settings.create.CertificateAddViewModel;
import ee.ria.DigiDoc.android.main.settings.create.ChooseFileScreen;
import ee.ria.DigiDoc.android.main.settings.create.ViewState;
import ee.ria.DigiDoc.android.main.settings.proxy.SettingsProxyDialog;
import ee.ria.DigiDoc.android.main.settings.signing.siva.SettingsSivaDialog;
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

public final class SettingsSigningView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final SwitchCompat askRoleAndAddressSwitch;
    private final LinearLayout tsaCertContainer;
    private final TextView tsaCertIssuedTo;
    private final TextView tsaCertValidTo;
    private final Button addCertificateButton;
    private final Button showCertificateButton;
    private X509Certificate tsaCertificate;

    private final Button sivaCategory;
    private final SettingsSivaDialog sivaDialog;
    private final Button proxyCategory;
    private final SettingsProxyDialog proxyDialog;

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;

    private final ViewDisposables disposables;
    private final CertificateAddViewModel viewModel;

    private static boolean isTsaCertificateViewVisible;
    private static final Subject<Boolean> isTsaCertificateViewVisibleSubject = PublishSubject.create();

    private final String viewId = String.valueOf(View.generateViewId());

    public SettingsSigningView(Context context) {
        this(context, null);
    }

    public SettingsSigningView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsSigningView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        viewModel = ApplicationApp.component(context).navigator()
                .viewModel(viewId, CertificateAddViewModel.class);
        inflate(context, R.layout.main_settings_signing, this);
        toolbarView = findViewById(R.id.toolbar);
        navigator = ApplicationApp.component(context).navigator();
        settingsDataStore = ApplicationApp.component(context).settingsDataStore();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.signature_update_title_created);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        tsaCertContainer = findViewById(R.id.mainSettingsTsaCertificateContainer);
        tsaCertIssuedTo = findViewById(R.id.mainSettingsTsaCertificateIssuedTo);
        tsaCertValidTo = findViewById(R.id.mainSettingsTsaCertificateValidTo);
        addCertificateButton = findViewById(R.id.mainSettingsTsaCertificateAddCertificateButton);
        showCertificateButton = findViewById(R.id.mainSettingsTsaCertificateShowCertificateButton);

        if (settingsDataStore != null) {
            isTsaCertificateViewVisible = settingsDataStore.getIsTsaCertificateViewVisible();
            setTSAContainerViewVisibility(isTsaCertificateViewVisible);
        }

        sivaCategory = findViewById(R.id.signingSettingsSivaCategory);
        proxyCategory = findViewById(R.id.signingSettingsProxyCategory);
        sivaDialog = new SettingsSivaDialog(navigator.activity());
        proxyDialog = new SettingsProxyDialog(navigator.activity());

        askRoleAndAddressSwitch = findViewById(R.id.mainSettingsAskRoleAndAddress);
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
        disposables.add(RxCompoundButton.checkedChanges(askRoleAndAddressSwitch)
                .subscribe(settingsDataStore::setIsRoleAskingEnabled));
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
        disposables.add(clicks(askRoleAndAddressSwitch)
                .subscribe(o -> {
                    boolean isChecked = askRoleAndAddressSwitch.isChecked();
                    settingsDataStore.setIsRoleAskingEnabled(isChecked);
                }));
        disposables.add(clicks(sivaCategory).subscribe(o -> sivaDialog.show()));
        disposables.add(clicks(proxyCategory).subscribe(o -> proxyDialog.show()));
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
}
