package ee.ria.DigiDoc.android.main.diagnostics;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsScreen.diagnosticsFileLogsSaveClicksSubject;
import static ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsScreen.diagnosticsFileSaveClicksSubject;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ClickableDialogUtil;
import ee.ria.DigiDoc.android.utils.TSLException;
import ee.ria.DigiDoc.android.utils.TSLUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
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
    private final ConfirmationDialog diagnosticsRestartConfirmationDialog = new ConfirmationDialog(Activity.getContext().get(),
            R.string.main_diagnostics_restart_message, R.id.mainDiagnosticsRestartConfirmationDialog);

    private final Navigator navigator;

    private final ViewDisposables disposables;

    private Disposable tslVersionDisposable;

    private final String DIAGNOSTICS_FILE_NAME = "ria_digidoc_" + getAppVersion() + "_diagnostics.txt";
    private final String DIAGNOSTICS_FILE_PATH = getContext().getFilesDir().getPath()
            + File.separator + "diagnostics" + File.separator;
    private final String DIAGNOSTICS_LOGS_FILE_NAME = "ria_digidoc_" + getAppVersion() + "_logs.txt";

    public DiagnosticsView(Context context) {
        super(context);
        dateFormat = ConfigurationDateUtil.getDateFormat();
        inflate(context, R.layout.main_diagnostics, this);
        AccessibilityUtils.setViewAccessibilityPaneTitle(this, R.string.main_diagnostics_title);
        toolbarView = findViewById(R.id.toolbar);
        View saveDiagnosticsButton = findViewById(R.id.configurationSaveButton);
        navigator = Application.component(context).navigator();

        SwitchCompat activateLogFileGenerating = findViewById(R.id.mainDiagnosticsLogging);
        activateLogFileGenerating.setChecked(((Activity) this.getContext()).getSettingsDataStore().getIsLogFileGenerationEnabled());
        Button saveLogFileButton = findViewById(R.id.mainDiagnosticsSaveLoggingButton);
        saveLogFileButton.setVisibility(FileUtil.logsExist(FileUtil.getLogsDirectory(getContext())) ? VISIBLE : GONE);

        ConfigurationProvider configurationProvider = ((Application) context.getApplicationContext()).getConfigurationProvider();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_diagnostics_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        findViewById(R.id.configurationUpdateButton).setOnClickListener(view -> updateConfiguration());

        fileLogToggleListener(activateLogFileGenerating);

        clicks(saveDiagnosticsButton).map(ignored ->
                (saveDiagnostics()))
                .subscribe(diagnosticsFileSaveClicksSubject);

        clicks(saveLogFileButton).map(ignored ->
                (saveLogFile()))
                .subscribe(diagnosticsFileLogsSaveClicksSubject);

        setData(configurationProvider);
    }

    private void fileLogToggleListener(SwitchCompat activateLogFileGenerating) {
        activateLogFileGenerating.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Activity activityContext = ((Activity) this.getContext());
            boolean isLogFileGenerationEnabled = activityContext.getSettingsDataStore().getIsLogFileGenerationEnabled();
            if (isChecked) {
                diagnosticsRestartConfirmationDialog.show();
                ClickableDialogUtil.makeLinksInDialogClickable(diagnosticsRestartConfirmationDialog);
                diagnosticsRestartConfirmationDialog.positiveButtonClicks()
                        .doOnNext(next -> {
                            diagnosticsRestartConfirmationDialog.dismiss();
                            activityContext.getSettingsDataStore().setIsLogFileGenerationEnabled(true);
                            activityContext.restartAppWithIntent(activityContext.getIntent(), true);
                        })
                        .subscribe();
                diagnosticsRestartConfirmationDialog.cancels()
                        .doOnNext(next -> {
                            diagnosticsRestartConfirmationDialog.dismiss();
                            activateLogFileGenerating.setChecked(false);
                            activityContext.getSettingsDataStore().setIsLogFileGenerationEnabled(false);
                        })
                        .subscribe();
            } else {
                activityContext.getSettingsDataStore().setIsLogFileGenerationEnabled(false);
                activityContext.getSettingsDataStore().setIsLogFileGenerationRunning(false);
                if (isLogFileGenerationEnabled) {
                    activityContext.restartAppWithIntent(activityContext.getIntent(), true);
                }
            }
        });
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
        List<TextView> textViews = new ArrayList<>();
        findAllTextViews(this, textViews);

        File root = new File(DIAGNOSTICS_FILE_PATH);
        if (!root.exists()) {
            boolean isDirectoryCreated = root.mkdirs();
            if (!isDirectoryCreated) {
                Timber.log(Log.ERROR, "Unable to create directory for diagnostics files");
                throw new NoSuchFileException(root.getAbsolutePath(), null,
                        "Unable to create directory for diagnostics files");
            }
        }

        File diagnosticsFileLocation = new File(DIAGNOSTICS_FILE_PATH + DIAGNOSTICS_FILE_NAME);
        try (FileOutputStream fileStream = new FileOutputStream(diagnosticsFileLocation);
             OutputStreamWriter writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8.name())) {
            writer.append(formatDiagnosticsText(textViews));
            writer.flush();
            return diagnosticsFileLocation;
        } catch (IOException ex) {
            Timber.log(Log.ERROR, ex, "Unable to get diagnostics file location");
            throw ex;
        } finally {
            textViews.clear();
        }
    }

    private static void findAllTextViews(View view, List<TextView> textViews) {
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                final View child = viewGroup.getChildAt(i);
                if (child instanceof TextView) {
                    textViews.add((TextView) child);
                } else {
                    findAllTextViews(child, textViews);
                }
            }
        }
    }

    private String formatDiagnosticsText(List<TextView> textViews) {
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

    private File saveLogFile() throws IOException {
        if (FileUtil.logsExist(FileUtil.getLogsDirectory(getContext()))) {
            return FileUtil.combineLogFiles(FileUtil.getLogsDirectory(getContext()), DIAGNOSTICS_LOGS_FILE_NAME);
        }
        throw new FileNotFoundException("Unable to get directory with logs");
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
        TextView ldapPersonUrl = findViewById(R.id.mainDiagnosticsLdapPersonUrl);
        TextView ldapCorpUrl = findViewById(R.id.mainDiagnosticsLdapCorpUrl);
        TextView mobileIDUrl = findViewById(R.id.mainDiagnosticsMobileIDUrl);
        TextView mobileIDSKUrl = findViewById(R.id.mainDiagnosticsMobileIDSKUrl);
        TextView smartIDUrlV1 = findViewById(R.id.mainDiagnosticsSmartIDUrlV1);
        TextView smartIDSKUrlV1 = findViewById(R.id.mainDiagnosticsSmartIDSKUrlV1);
        TextView smartIDUrlV2 = findViewById(R.id.mainDiagnosticsSmartIDUrlV2);
        TextView smartIDSKUrlV2 = findViewById(R.id.mainDiagnosticsSmartIDSKUrlV2);
        TextView rpUuid = findViewById(R.id.mainDiagnosticsRpUuid);
        TextView centralConfigurationDate = findViewById(R.id.mainDiagnosticsCentralConfigurationDate);
        TextView centralConfigurationSerial = findViewById(R.id.mainDiagnosticsCentralConfigurationSerial);
        TextView centralConfigurationUrl = findViewById(R.id.mainDiagnosticsCentralConfigurationUrl);
        TextView centralConfigurationVersion = findViewById(R.id.mainDiagnosticsCentralConfigurationVersion);
        TextView centralConfigurationLastCheck = findViewById(R.id.mainDiagnosticsCentralConfigurationLastCheck);
        TextView centralConfigurationUpdateDate = findViewById(R.id.mainDiagnosticsCentralConfigurationUpdateDate);

        applicationVersion.setText(setDisplayTextWithTitle(R.string.main_diagnostics_application_version_title,
                getAppVersion(), Typeface.DEFAULT_BOLD));
        androidVersion.setText(setDisplayTextWithTitle(R.string.main_diagnostics_operating_system_title,
                getAndroidVersion(), Typeface.DEFAULT_BOLD));
        libDocVersion.setText(setDisplayTextWithTitle(R.string.main_diagnostics_libdigidocpp_title,
                getLibDigiDocVersion(), Typeface.DEFAULT_BOLD));

        configUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_config_url_title,
                configurationProvider.getConfigUrl(), Typeface.DEFAULT));
        tslUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_tsl_url_title,
                configurationProvider.getTslUrl(), Typeface.DEFAULT));
        appendTslVersion(tslUrl, configurationProvider.getTslUrl());
        sivaUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_siva_url_title,
                configurationProvider.getSivaUrl(), Typeface.DEFAULT));
        tsaUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_tsa_url_title,
                (getTsaUrlText() != null && !getTsaUrlText().isEmpty()) ?
                        getTsaUrlText() : configurationProvider.getTsaUrl(), Typeface.DEFAULT));
        ldapPersonUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_ldap_person_url_title,
                configurationProvider.getLdapPersonUrl(), Typeface.DEFAULT));
        ldapCorpUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_ldap_corp_url_title,
                configurationProvider.getLdapCorpUrl(), Typeface.DEFAULT));
        mobileIDUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_mid_proxy_url_title,
                configurationProvider.getMidRestUrl(), Typeface.DEFAULT));
        mobileIDSKUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_mid_sk_url_title,
                configurationProvider.getMidSkRestUrl(), Typeface.DEFAULT));
        smartIDUrlV1.setText(setDisplayTextWithTitle(R.string.main_diagnostics_sid_v1_proxy_url_title,
                configurationProvider.getSidRestUrl(), Typeface.DEFAULT));
        smartIDSKUrlV1.setText(setDisplayTextWithTitle(R.string.main_diagnostics_sid_v1_sk_url_title,
                configurationProvider.getSidSkRestUrl(), Typeface.DEFAULT));
        smartIDUrlV2.setText(setDisplayTextWithTitle(R.string.main_diagnostics_sid_v2_proxy_url_title,
                configurationProvider.getSidV2RestUrl(), Typeface.DEFAULT));
        smartIDSKUrlV2.setText(setDisplayTextWithTitle(R.string.main_diagnostics_sid_v2_sk_url_title,
                configurationProvider.getSidV2SkRestUrl(), Typeface.DEFAULT));
        rpUuid.setText(setDisplayTextWithTitle(R.string.main_diagnostics_rpuuid_title,
                getRpUuidText(), Typeface.DEFAULT));

        setTslCacheData();

        centralConfigurationDate.setText(setDisplayTextWithTitle(R.string.main_diagnostics_date_title,
                configurationProvider.getMetaInf().getDate(), Typeface.DEFAULT));
        centralConfigurationSerial.setText(setDisplayTextWithTitle(R.string.main_diagnostics_serial_title,
                String.valueOf(configurationProvider.getMetaInf().getSerial()), Typeface.DEFAULT));
        centralConfigurationUrl.setText(setDisplayTextWithTitle(R.string.main_diagnostics_url_title,
                configurationProvider.getMetaInf().getUrl(), Typeface.DEFAULT));
        centralConfigurationVersion.setText(setDisplayTextWithTitle(R.string.main_diagnostics_version_title,
                String.valueOf(configurationProvider.getMetaInf().getVersion()), Typeface.DEFAULT));
        centralConfigurationUpdateDate.setText(setDisplayTextWithTitle(R.string.main_diagnostics_configuration_update_date,
                displayDate(configurationProvider.getConfigurationUpdateDate()), Typeface.DEFAULT));
        centralConfigurationLastCheck.setText(setDisplayTextWithTitle(R.string.main_diagnostics_configuration_last_check_date,
                displayDate(configurationProvider.getConfigurationLastUpdateCheckDate()), Typeface.DEFAULT));
    }

    private void appendTslVersion(TextView tslUrlTextView, String tslUrl) {
        tslVersionDisposable = getObservableTslVersion(tslUrl )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (tslVersion) -> tslUrlTextView.append(" ("+ tslVersion + ")"),
                        (error) -> Timber.log(Log.ERROR, error, "Error reading TSL version")
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
                Timber.log(Log.ERROR, message);
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

    private String getTsaUrlText() {
        return ((Activity) this.getContext()).getSettingsDataStore().getTsaUrl();
    }

    private void setTslCacheData() {
        LinearLayout tslCacheLayout = findViewById(R.id.mainDiagnosticsTslCacheLayout);

        File tslCacheDir = new File(getContext().getApplicationContext().getCacheDir().getAbsolutePath() + "/schema");
        File[] tslFiles = tslCacheDir.listFiles((directory, fileName) -> fileName.endsWith(".xml"));

        getDisplayedNonExistentTSLCacheFiles(tslCacheLayout).forEach(tslCacheLayout::removeViewInLayout);

        if (tslFiles != null && tslFiles.length > 0) {
            Arrays.sort(tslFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File tslFile : tslFiles) {
                try (InputStream tslInputStream = new FileInputStream(tslFile)) {
                    int version = TSLUtil.readSequenceNumber(tslInputStream);
                    TextView tslEntry = new TextView(tslCacheLayout.getContext());
                    tslEntry.setTextAppearance(R.style.MaterialTypography_Dense_Body1);
                    tslEntry.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    String tslEntryText = tslFile.getName() + " (" + version + ")";
                    tslEntry.setText(FileUtil.normalizeText(tslEntryText));
                    tslCacheLayout.addView(tslEntry);
                } catch (Exception e) {
                    Timber.log(Log.ERROR, e, "Error displaying TSL version for: %s", tslFile.getAbsolutePath());
                }
            }
        }
    }

    private ArrayList<View> getDisplayedNonExistentTSLCacheFiles(LinearLayout tslCacheLayout) {
        ArrayList<View> removeViews = new ArrayList<>();
        for (int i = 0; i < tslCacheLayout.getChildCount(); i++) {
            View tslCacheLayoutChild = tslCacheLayout.getChildAt(i);
            String fileName = ((TextView)tslCacheLayoutChild).getText().toString();
            String tslCacheTitle = getResources().getString(R.string.main_diagnostics_tsl_cache_title);
            if (!fileName.equals(tslCacheTitle)) {
                removeViews.add(tslCacheLayoutChild);
            }
        }
        return removeViews;
    }

    private String displayDate(Date date) {
        if (date == null) {
            return "";
        }

        return dateFormat.format(date);
    }

    private Spannable setDisplayTextWithTitle(int titleId, String text, Typeface typeface) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(getResources().getString(titleId),
                new StyleSpan(typeface.getStyle()), SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(text);
        return ssb;
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
