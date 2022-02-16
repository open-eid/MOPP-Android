package ee.ria.DigiDoc.android.main.diagnostics;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsScreen.diagnosticsFileSaveClicksSubject;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.TSLException;
import ee.ria.DigiDoc.android.utils.TSLUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.configuration.ConfigurationDateUtil;
import ee.ria.DigiDoc.configuration.ConfigurationManagerService;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.sign.SignLib;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public final class DiagnosticsView extends CoordinatorLayout {

    private final SimpleDateFormat dateFormat;
    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    private Disposable tslVersionDisposable;

    private final ArrayList<TextView> textViews = new ArrayList<>();

    private final String DIAGNOSTICS_FILE_NAME = "ria_digidoc_" + getAppVersion() + "_diagnostics.txt";
    private final String DIAGNOSTICS_FILE_PATH = getContext().getFilesDir().getPath()
            + File.separator + "diagnostics" + File.separator;

    public DiagnosticsView(Context context) {
        super(context);
        dateFormat = ConfigurationDateUtil.getDateFormat();
        inflate(context, R.layout.main_diagnostics, this);
        AccessibilityUtils.setAccessibilityPaneTitle(this, R.string.main_diagnostics_title);
        toolbarView = findViewById(R.id.toolbar);
        View saveDiagnosticsButton = findViewById(R.id.configurationSaveButton);
        navigator = Application.component(context).navigator();

        ConfigurationProvider configurationProvider = ((Application) context.getApplicationContext()).getConfigurationProvider();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_diagnostics_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        findViewById(R.id.configurationUpdateButton).setOnClickListener(view -> updateConfiguration());

        clicks(saveDiagnosticsButton).map(ignored ->
                (saveDiagnostics()))
                .subscribe(diagnosticsFileSaveClicksSubject);

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
        if (tslVersionDisposable != null) {
            tslVersionDisposable.dispose();
        }
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

    private File saveDiagnostics() throws IOException {
        getAllTextViews(this);

        File root = new File(DIAGNOSTICS_FILE_PATH);
        if (!root.exists()) {
            boolean isDirectoryCreated = root.mkdirs();
            if (!isDirectoryCreated) {
                Timber.log(Log.ERROR, "Unable to create directory for diagnostics files");
            }
        }

        File diagnosticsFileLocation = new File(DIAGNOSTICS_FILE_PATH + DIAGNOSTICS_FILE_NAME);
        try (FileWriter fileWriter = new FileWriter(diagnosticsFileLocation)) {
            fileWriter.append(formatDiagnosticsText(textViews));
            fileWriter.flush();
            return diagnosticsFileLocation;
        } catch (IOException ex) {
            Timber.log(Log.ERROR, ex, "Unable to get diagnostics file location");
            throw ex;
        } finally {
            textViews.clear();
        }

    }

    private void getAllTextViews(View view) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                final View child = ((ViewGroup) view).getChildAt(i);
                if (child instanceof TextView) {
                    textViews.add((TextView) child);
                } else {
                    getAllTextViews(child);
                }
            }
        }
    }

    private String formatDiagnosticsText(ArrayList<TextView> textViews) {
        StringBuilder diagnosticsText = new StringBuilder();

        for (int i = 0; i < textViews.size(); i++) {
            TextView textView = textViews.get(i);
            String text = textView.getText().toString();
            if (!isTitleOrButtonText(text)) {
                if (isCategoryLabel(text)) {
                    diagnosticsText.append("\n\n").append(text).append("\n");
                } else {
                    if (!isTSLFile(text) && textView.getId() == -1) {
                        diagnosticsText.append(text);
                    } else {
                        diagnosticsText.append(text).append("\n");
                    }
                }
            }
        }

        return diagnosticsText.toString();
    }

    private boolean isTitleOrButtonText(String text) {
        return text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_title)) ||
                text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_configuration_check_for_update_button)) ||
                text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_configuration_save_diagnostics_button));
    }

    private boolean isCategoryLabel(String text) {
        return text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_libraries_title)) ||
                text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_urls_title)) ||
                text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_central_configuration_title)) ||
                text.equalsIgnoreCase(getResources().getString(R.string.main_diagnostics_tsl_cache_title));
    }

    private boolean isTSLFile(String text) {
        String diagnosticFileName = text.split(" ")[0];
        if (diagnosticFileName.contains(".xml")) {
            File tslCacheDir = new File(getContext().getApplicationContext().getCacheDir().getAbsolutePath() + "/schema");
            File[] tslFiles = tslCacheDir.listFiles((directory, fileName) -> fileName.endsWith(".xml"));
            if (tslFiles != null) {
                Object[] fileNames = Arrays.stream(Arrays.stream(tslFiles)
                        .filter(File::isFile).map(File::getName).toArray(String[]::new)).toArray();
                return Arrays.asList(fileNames).contains(diagnosticFileName);
            }
        }
         return false;
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
        TextView rpUuid = findViewById(R.id.mainDiagnosticsRpUuid);
        TextView centralConfigurationDate = findViewById(R.id.mainDiagnosticsCentralConfigurationDate);
        TextView centralConfigurationSerial = findViewById(R.id.mainDiagnosticsCentralConfigurationSerial);
        TextView centralConfigurationUrl = findViewById(R.id.mainDiagnosticsCentralConfigurationUrl);
        TextView centralConfigurationVersion = findViewById(R.id.mainDiagnosticsCentralConfigurationVersion);
        TextView centralConfigurationLastCheck = findViewById(R.id.mainDiagnosticsCentralConfigurationLastCheck);
        TextView centralConfigurationUpdateDate = findViewById(R.id.mainDiagnosticsCentralConfigurationUpdateDate);

        applicationVersion.setText(getAppVersion());
        androidVersion.setText(getAndroidVersion());
        libDocVersion.setText(getResources().getString(R.string.main_diagnostics_libdigidocpp_title, getLibDigiDocVersion()));

        configUrl.setText(configurationProvider.getConfigUrl());
        tslUrl.setText(configurationProvider.getTslUrl());
        appendTslVersion(tslUrl, configurationProvider.getTslUrl());
        sivaUrl.setText(configurationProvider.getSivaUrl());
        tsaUrl.setText(configurationProvider.getTsaUrl());
        midSignUrl.setText(configurationProvider.getMidSignUrl());
        ldapPersonUrl.setText(configurationProvider.getLdapPersonUrl());
        ldapCorpUrl.setText(configurationProvider.getLdapCorpUrl());
        mobileIDUrl.setText(configurationProvider.getMidRestUrl());
        mobileIDSKUrl.setText(configurationProvider.getMidSkRestUrl());
        smartIDUrl.setText(configurationProvider.getSidRestUrl());
        smartIDSKUrl.setText(configurationProvider.getSidSkRestUrl());
        rpUuid.setText(getRpUuidText());

        setTslCacheData();

        centralConfigurationDate.setText(configurationProvider.getMetaInf().getDate());
        centralConfigurationSerial.setText(String.valueOf(configurationProvider.getMetaInf().getSerial()));
        centralConfigurationUrl.setText(configurationProvider.getMetaInf().getUrl());
        centralConfigurationVersion.setText(String.valueOf(configurationProvider.getMetaInf().getVersion()));
        centralConfigurationLastCheck.setText(displayDate(configurationProvider.getConfigurationLastUpdateCheckDate()));
        centralConfigurationUpdateDate.setText(displayDate(configurationProvider.getConfigurationUpdateDate()));
    }

    private void appendTslVersion(TextView tslUrlTextView, String tslUrl) {
        tslVersionDisposable = getObservableTslVersion(tslUrl )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (tslVersion) -> tslUrlTextView.append(" ("+ tslVersion + ")"),
                        (error) -> Timber.e(error, "Error reading TSL version")
                );
    }

    private Observable<Integer> getObservableTslVersion(String tslUrl) {
        return Observable.fromCallable(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(tslUrl).build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                 try (InputStream responseBody = response.body().byteStream()) {
                     return TSLUtil.readSequenceNumber(responseBody);
                 }
            } else {
                String message = "Error fetching TSL, response code: " + response.code();
                Timber.e(message);
                throw new TSLException(message);
            }
        });
    }

    private String getRpUuidText() {
        String rpUuid = ((Activity) this.getContext()).getSettingsDataStore().getUuid();
        int uuid = rpUuid == null || rpUuid.isEmpty()
                ? R.string.main_diagnostics_rpuuid_default
                : R.string.main_diagnostics_rpuuid_custom;
        return getResources().getString(uuid);
    }

    private void setTslCacheData() {
        LinearLayout tslCacheLayout = findViewById(R.id.mainDiagnosticsTslCacheLayout);

        File tslCacheDir = new File(getContext().getApplicationContext().getCacheDir().getAbsolutePath() + "/schema");
        File[] tslFiles = tslCacheDir.listFiles((directory, fileName) -> fileName.endsWith(".xml"));
        if (tslFiles != null && tslFiles.length > 0) {
            Arrays.sort(tslFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File tslFile : tslFiles) {
                try (InputStream tslInputStream = new FileInputStream(tslFile)) {
                    int version = TSLUtil.readSequenceNumber(tslInputStream);
                    TextView tslEntry = new TextView(tslCacheLayout.getContext());
                    tslEntry.setTextAppearance(R.style.MaterialTypography_Dense_Body1);
                    tslEntry.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    String tslEntryText = tslFile.getName() + " (" + version + ")";
                    tslEntry.setText(tslEntryText);
                    tslCacheLayout.addView(tslEntry);
                } catch (Exception e) {
                    Timber.e(e, "Error displaying TSL version for: %s", tslFile.getAbsolutePath());
                }
            }
        }

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