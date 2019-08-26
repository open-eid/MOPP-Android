package ee.ria.DigiDoc.android.main.diagnostics;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.sign.SignLib;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class DiagnosticsView extends CoordinatorLayout {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public DiagnosticsView(Context context) {
        super(context);
        inflate(context, R.layout.main_diagnostics, this);
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

    public void updateViewData(ConfigurationProvider configurationProvider) {
        setData(configurationProvider);
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

        return dateFormatter.format(date);
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