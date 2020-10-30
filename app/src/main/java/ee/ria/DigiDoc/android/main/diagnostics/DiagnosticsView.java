package ee.ria.DigiDoc.android.main.diagnostics;

import android.content.Context;
import android.os.Build;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.widget.Toolbar;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.configuration.ConfigurationDateUtil;
import ee.ria.DigiDoc.configuration.ConfigurationManagerService;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.sign.SignLib;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class DiagnosticsView extends CoordinatorLayout {

    private final SimpleDateFormat dateFormat;
    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public DiagnosticsView(Context context) {
        super(context);
        dateFormat = ConfigurationDateUtil.getDateFormat();
        inflate(context, R.layout.main_diagnostics, this);
        AccessibilityUtils.setAccessibilityPaneTitle(this, R.string.main_diagnostics_title);
        toolbarView = findViewById(R.id.toolbar);
        navigator = Application.component(context).navigator();

        ConfigurationProvider configurationProvider = ((Application) context.getApplicationContext()).getConfigurationProvider();
        disposables = new ViewDisposables();

        findViewById(R.id.configurationUpdateButton).setOnClickListener(view -> updateConfiguration());
        setData(configurationProvider);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    public void updateViewData(ConfigurationProvider configurationProvider, int resultCode) {
        setData(configurationProvider);
        int messageResId;
        if (resultCode == ConfigurationManagerService.NEW_CONFIGURATION_LOADED) {
            messageResId = R.string.configuration_updated;
        } else {
            messageResId = R.string.configuration_is_already_up_to_date;
        }
        AccessibilityUtils.sendAccessibilityEvent(getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, messageResId);
    }

    private void updateConfiguration() {
        Application application = (Application) getContext().getApplicationContext();
        application.updateConfiguration(this);
    }

    private void setData(ConfigurationProvider configurationProvider) {
        TextView applicationVersion = findViewById(R.id.mainDiagnosticsApplicationVersion);
        TextView androidVersion = findViewById(R.id.mainDiagnosticsAndroidVersion);
        TextView libDocVersion = findViewById(R.id.mainDiagnosticsLibdigidocppVersion);
        TextView configUrl = findViewById(R.id.mainDiagnosticsConfigUrl);
        TextView tslUrl = findViewById(R.id.mainDiagnosticsTslUrl);
        TextView sivaUrl = findViewById(R.id.mainDiagnosticsSivaUrl);
        TextView tsaUrl = findViewById(R.id.mainDiagnosticsTsaUrl);
        TextView midSignUrl = findViewById(R.id.mainDiagnosticsMidSignUrl);
        TextView ldapPersonUrl = findViewById(R.id.mainDiagnosticsLdapPersonUrl);
        TextView ldapCorpUrl = findViewById(R.id.mainDiagnosticsLdapCorpUrl);
        TextView mobileIDUrl = findViewById(R.id.mainDiagnosticsMobileIDUrl);
        TextView mobileIDSKUrl = findViewById(R.id.mainDiagnosticsMobileIDSKUrl);
        TextView smartIDUrl = findViewById(R.id.mainDiagnosticsSmartIDUrl);
        TextView smartIDSKUrl = findViewById(R.id.mainDiagnosticsSmartIDSKUrl);
        TextView centralConfigurationDate = findViewById(R.id.mainDiagnosticsCentralConfigurationDate);
        TextView centralConfigurationSerial = findViewById(R.id.mainDiagnosticsCentralConfigurationSerial);
        TextView centralConfigurationUrl = findViewById(R.id.mainDiagnosticsCentralConfigurationUrl);
        TextView centralConfigurationVersion = findViewById(R.id.mainDiagnosticsCentralConfigurationVersion);
        TextView centralConfigurationLastCheck = findViewById(R.id.mainDiagnosticsCentralConfigurationLastCheck);
        TextView centralConfigurationUpdateDate = findViewById(R.id.mainDiagnosticsCentralConfigurationUpdateDate);

        applicationVersion.setText(getAppVersion());
        androidVersion.setText(getAndroidVersion());
        libDocVersion.setText(getResources().getString(R.string.main_about_libdigidocpp_title, getLibDigiDocVersion()));

        configUrl.setText(configurationProvider.getConfigUrl());
        tslUrl.setText(configurationProvider.getTslUrl());
        sivaUrl.setText(configurationProvider.getSivaUrl());
        tsaUrl.setText(configurationProvider.getTsaUrl());
        midSignUrl.setText(configurationProvider.getMidSignUrl());
        ldapPersonUrl.setText(configurationProvider.getLdapPersonUrl());
        ldapCorpUrl.setText(configurationProvider.getLdapCorpUrl());
        mobileIDUrl.setText(configurationProvider.getMidRestUrl());
        mobileIDSKUrl.setText(configurationProvider.getMidSkRestUrl());
        smartIDUrl.setText(configurationProvider.getSidRestUrl());
        smartIDSKUrl.setText(configurationProvider.getSidSkRestUrl());

        centralConfigurationDate.setText(configurationProvider.getMetaInf().getDate());
        centralConfigurationSerial.setText(String.valueOf(configurationProvider.getMetaInf().getSerial()));
        centralConfigurationUrl.setText(configurationProvider.getMetaInf().getUrl());
        centralConfigurationVersion.setText(String.valueOf(configurationProvider.getMetaInf().getVersion()));
        centralConfigurationLastCheck.setText(displayDate(configurationProvider.getConfigurationLastUpdateCheckDate()));
        centralConfigurationUpdateDate.setText(displayDate(configurationProvider.getConfigurationUpdateDate()));
    }

    private String displayDate(Date date) {
        if (date == null) {
            return "";
        }

        return dateFormat.format(date);
    }

    private static String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private static String getAndroidVersion() {
        return "Android " + Build.VERSION.RELEASE;
    }

    private static String getLibDigiDocVersion() {
        return SignLib.libdigidocppVersion();
    }
}