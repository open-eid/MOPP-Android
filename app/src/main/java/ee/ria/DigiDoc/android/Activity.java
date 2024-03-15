package ee.ria.DigiDoc.android;

import static ee.ria.DigiDoc.android.Constants.DIR_EXTERNALLY_OPENED_FILES;
import static ee.ria.DigiDoc.android.Constants.VIEW_TYPE;
import static ee.ria.DigiDoc.android.utils.IntentUtils.setIntentData;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;
import androidx.work.WorkManager;

import com.google.android.gms.common.util.CollectionUtils;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.ImmutableList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.crashlytics.internal.common.CommonUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsScreen;
import ee.ria.DigiDoc.android.main.home.HomeScreen;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.main.settings.rights.SettingsRightsScreen;
import ee.ria.DigiDoc.android.main.sharing.SharingScreen;
import ee.ria.DigiDoc.android.root.RootCreateScreen;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.utils.ContainerMimeTypeUtil;
import ee.ria.DigiDoc.android.utils.IntentUtils;
import ee.ria.DigiDoc.android.utils.RootUtil;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.ViewType;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.sign.SignedContainer;
import timber.log.Timber;

public final class Activity extends AppCompatActivity {

    private static final String rootedKey = "IS_ROOTED";

    private Navigator navigator;
    private RootScreenFactory rootScreenFactory;
    private SettingsDataStore settingsDataStore;

    private static WeakReference<Context> mContext;

    public SettingsDataStore getSettingsDataStore() {
        return settingsDataStore;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Application);
        setTitle(""); // ACCESSIBILITY: prevents application name read during each activity launch
        if (isRooted()) {
            super.onCreate(null);
            Intent intent = new Intent();
            intent.putExtra(rootedKey, true);
            rootScreenFactory.intent(intent, this);
        } else {
            super.onCreate(savedInstanceState);

            // Prevent screen recording
            SecureUtil.markAsSecure(this, getWindow());

            handleCrashOnPreviousExecution();

            WorkManager.getInstance(this).cancelAllWork();

            Intent intent = sanitizeIntent(getIntent());

            ViewType viewType = settingsDataStore.getViewType();

            if (viewType != null) {
                intent.putExtra(VIEW_TYPE, viewType.name());
                rootScreenFactory.intent(intent, this);
            } else {
                if ((Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) && intent.getType() != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(intent.getAction());
                    handleIncomingFiles(intent, this);
                } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
                    getIntent().setAction(Intent.ACTION_MAIN);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    restartAppWithIntent(intent, false);
                } else if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
                    rootScreenFactory.intent(intent, this);
                } else if (Intent.ACTION_MAIN.equals(intent.getAction()) && savedInstanceState != null) {
                    savedInstanceState = null;
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    restartAppWithIntent(intent, false);
                } else {
                    rootScreenFactory.intent(intent, this);
                }
            }

            mContext = new WeakReference<>(this);

            initializeApplicationFileTypesAssociation();
            initializeRoleAndAddressAsking();
        }

        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
        setTitle("\u202F");
        ViewCompat.setImportantForAccessibility(getWindow().getDecorView(), ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private boolean isRooted() {
        return CommonUtils.isRooted() || RootUtil.isDeviceRooted();
    }

    private void handleCrashOnPreviousExecution() {
        if (FirebaseCrashlytics.getInstance().didCrashOnPreviousExecution()) {
            if (settingsDataStore.getAlwaysSendCrashReport()) {
                sendUnsentCrashReports();
                return;
            }
            Dialog crashReportDialog = new Dialog(this);
            SecureUtil.markAsSecure(this, crashReportDialog.getWindow());
            crashReportDialog.setContentView(R.layout.crash_report_dialog);

            Button sendButton = crashReportDialog.findViewById(R.id.sendButton);
            sendButton.setOnClickListener(v -> {
                sendUnsentCrashReports();
                crashReportDialog.dismiss();
            });
            Button alwaysSendButton = crashReportDialog.findViewById(R.id.alwaysSendButton);
            alwaysSendButton.setOnClickListener(v -> {
                settingsDataStore.setAlwaysSendCrashReport(true);
                sendUnsentCrashReports();
                crashReportDialog.dismiss();
            });
            Button dontSendButton = crashReportDialog.findViewById(R.id.dontSendButton);
            dontSendButton.setOnClickListener(v -> crashReportDialog.dismiss());

            crashReportDialog.show();
        }
    }

    private void sendUnsentCrashReports() {
        Task<Boolean> task = FirebaseCrashlytics.getInstance().checkForUnsentReports();
        task.addOnSuccessListener(hasUnsentReports -> {
            if (hasUnsentReports) {
                FirebaseCrashlytics.getInstance().sendUnsentReports();
            } else {
                FirebaseCrashlytics.getInstance().deleteUnsentReports();
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void restartAppWithIntent(Intent intent, boolean withExit) {
        finish();
        startActivity(intent);
        overridePendingTransition (0, 0);
        if (withExit) {
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
        }
    }

    private void handleIncomingFiles(Intent intent, Activity activity) {
        try {
            intent.setDataAndType(FileUtil.normalizeUri(intent.getData()), "*/*");
            rootScreenFactory.intent(intent, activity);
        } catch (ActivityNotFoundException e) {
            Timber.log(Log.ERROR, e, "Handling incoming file intent");
        }
    }

    private Intent sanitizeIntent(Intent intent) {
        if (intent != null && intent.getDataString() != null) {
            Uri normalizedUri = FileUtil.normalizeUri(Uri.parse(intent.getDataString()));
            intent.setDataAndNormalize(normalizedUri);
        }
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().containsKey(Intent.EXTRA_REFERRER)) {
                intent.getExtras().getString(Intent.EXTRA_REFERRER);
            }
            intent.replaceExtras(new Bundle());
        }
        return intent;
    }

    private void initializeApplicationFileTypesAssociation() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isOpenAllTypesEnabled = sharedPreferences.getBoolean(getString(R.string.main_settings_open_all_filetypes_key), true);

        PackageManager pm = getApplicationContext().getPackageManager();
        ComponentName openAllTypesComponent = new ComponentName(getPackageName(), getClass().getName() + ".OPEN_ALL_FILE_TYPES");
        ComponentName openCustomTypesComponent = new ComponentName(getPackageName(), getClass().getName() + ".OPEN_CUSTOM_TYPES");

        if (isOpenAllTypesEnabled) {
            pm.setComponentEnabledSetting(openAllTypesComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(openCustomTypesComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } else {
            pm.setComponentEnabledSetting(openCustomTypesComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(openAllTypesComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    private void initializeRoleAndAddressAsking() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!sharedPreferences.contains(getString(R.string.main_settings_ask_role_and_address_key))) {
            sharedPreferences.edit().putBoolean(getString(R.string.main_settings_ask_role_and_address_key), false)
                    .apply();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        ApplicationApp.ApplicationComponent component = ApplicationApp.component(newBase);
        navigator = component.navigator();
        rootScreenFactory = component.rootScreenFactory();
        settingsDataStore = component.settingsDataStore();
        super.attachBaseContext(component.localeService().attachBaseContext(newBase));
    }

    @Override
    public void onBackPressed() {
        if (!navigator.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If user selects a file from provider menu (Open from -> RIA DigiDoc), it starts a new activity
        // Replace the main activity after the new file has been selected
        if (Optional.ofNullable(data)
                .map(Intent::getAction)
                .filter(action -> action.equals(Intent.ACTION_GET_CONTENT))
                .isPresent()) {
            navigator.onCreate(this, findViewById(android.R.id.content), null);
        }
        navigator.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        navigator.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static WeakReference<Context> getContext() {
        return mContext;
    }

    void resetScreen() {
        settingsDataStore.setViewType(ViewType.MAIN);
    }

    @Singleton
    static final class RootScreenFactory implements Callable<List<Screen>> {

        @Nullable private Intent intent;

        private android.app.Activity activity;

        @Inject RootScreenFactory() {}

        void intent(Intent intent, android.app.Activity activity) {
            this.intent = intent;
            this.activity = activity;
        }

        @Override
        public List<Screen> call() {
            if (intent != null && intent.getBooleanExtra(rootedKey, false)) {
                return List.of(RootCreateScreen.create());
            }

            if (intent == null || intent.getAction() == null) {
                Timber.log(Log.DEBUG, "Creating HomeScreen");
                return List.of(HomeScreen.create(intent));
            }

            String action = intent.getAction();
            Optional<String> viewTypeExtra = Optional.ofNullable(intent.getStringExtra(VIEW_TYPE));

            String viewType = viewTypeExtra.orElse(ViewType.MAIN.name());

            if (!viewType.equals(ViewType.MAIN.name())) {
                resetScreen();

                if (viewType.equals(ViewType.MENU.name())) {
                    Timber.log(Log.DEBUG, "Creating Diagnostics Screen");
                    intent.putExtra(VIEW_TYPE, ViewType.MENU.name());
                    return List.of(
                            HomeScreen.create(intent)
                    );
                } else if (viewType.equals(ViewType.DIAGNOSTICS.name())) {
                    Timber.log(Log.DEBUG, "Creating Diagnostics Screen");
                    intent.putExtra(VIEW_TYPE, ViewType.DIAGNOSTICS.name());
                    return List.of(
                            HomeScreen.create(intent),
                            DiagnosticsScreen.create()
                    );
                } else if (viewType.equals(ViewType.SETTINGS.name())) {
                    Timber.log(Log.DEBUG, "Creating Setting screen");
                    intent.putExtra(VIEW_TYPE, ViewType.SETTINGS.name());
                    return List.of(
                            HomeScreen.create(intent),
                            SettingsRightsScreen.create()
                    );
                }

                Timber.log(Log.DEBUG, String.format("Unknown view type %s. Creating HomeScreen", viewType));
                return List.of(HomeScreen.create(intent));
            }

            if ((Intent.ACTION_SEND.equals(action) ||
                    Intent.ACTION_SEND_MULTIPLE.equals(action) ||
                    Intent.ACTION_VIEW.equals(action)) &&
                    intent.getType() != null) {
                Timber.log(Log.DEBUG, "Choosing screen...");
                return List.of(chooseScreen(intent, activity));
            } else if (Intent.ACTION_GET_CONTENT.equals(action)) {
                return List.of(SharingScreen.create());
            }
            Timber.log(Log.DEBUG, "Creating default HomeScreen");
            return List.of(HomeScreen.create(intent));
        }

        private Screen chooseScreen(Intent intent, android.app.Activity activity) {
            ImmutableList<FileStream> fileStreams;
            File externallyOpenedFilesDir = new File(activity.getFilesDir(), DIR_EXTERNALLY_OPENED_FILES);
            try {
                fileStreams = IntentUtils.parseGetContentIntent(getContext().get(),
                        activity.getContentResolver(), intent, externallyOpenedFilesDir);
            } catch (Exception e) {
                Timber.log(Log.ERROR, e, "Unable to open file. Creating HomeScreen");
                ToastUtil.showError(getContext().get(), R.string.signature_create_error);
                return HomeScreen.create(
                        new Intent(Intent.ACTION_MAIN)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)
                );
            }
            if (!CollectionUtils.isEmpty(fileStreams) && fileStreams.size() == 1) {
                String fileName = fileStreams.get(0).displayName();
                int extensionPart = fileName.lastIndexOf(".");
                if (extensionPart != -1) {
                    String extension = fileName.substring(fileName.lastIndexOf("."));
                    if (".cdoc".equalsIgnoreCase(extension)) {
                        Timber.log(Log.DEBUG, "Creating CryptoCreateScreen");
                        return CryptoCreateScreen.open(intent);
                    }
                } else if (intent.getClipData() != null || intent.getData() != null) {
                    File file = IntentUtils.parseGetContentIntent(getContext().get(),
                            activity.getContentResolver(), intent.getClipData() != null ?
                                    intent.getClipData().getItemAt(0).getUri() :
                                    intent.getData(),
                            externallyOpenedFilesDir);
                    try {
                        String newFileName = "container";
                        if (SignedContainer.isCdoc(file)) {
                            Path renamedFile = FileUtil.renameFile(file.toPath(),
                                    newFileName + ".cdoc");
                            CryptoContainer.open(renamedFile.toFile());
                            Intent updatedIntent = setIntentData(intent, renamedFile, activity);
                            Timber.log(Log.DEBUG, "Creating CryptoCreateScreen");
                            return CryptoCreateScreen.open(updatedIntent);
                        } else {
                            String externalFileName = getFileName(file);
                            if (!externalFileName.isEmpty()) {
                                Path renamedFile = FileUtil.renameFile(file.toPath(),
                                        newFileName);
                                SignedContainer.open(renamedFile.toFile(), false);
                                Intent updatedIntent = setIntentData(intent, renamedFile, activity);
                                Timber.log(Log.DEBUG, String.format("Creating SignatureCreateScreen with filename %s", renamedFile));
                                return SignatureCreateScreen.create(updatedIntent);
                            } else {
                                Timber.log(Log.DEBUG, "No filename. Creating SignatureCreateScreen");
                                return SignatureCreateScreen.create(intent);
                            }
                        }
                    } catch (Exception e) {
                        Timber.log(Log.ERROR, e, "Unable to open container. Opening as file");
                        return SignatureCreateScreen.create(intent);
                    }
                }
            }
            Timber.log(Log.DEBUG, "Filestream is empty or has multiple elements. Creating SignatureCreateScreen");
            return SignatureCreateScreen.create(intent);
        }

        private static String getFileName(File file) {
            if (SignedContainer.isDdoc(file)) {
                return "container." + "ddoc";
            } else if (FileUtil.isPDF(file)) {
                return "file.pdf";
            } else {
                String extension = ContainerMimeTypeUtil.getContainerExtension(file);
                if (!extension.isEmpty()) {
                    return "container." + extension;
                } else {
                    return file.getName();
                }
            }
        }

        private void resetScreen() {
            ((Activity) activity).resetScreen();
        }
    }


}
