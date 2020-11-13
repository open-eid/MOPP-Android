package ee.ria.DigiDoc.android;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.common.util.CollectionUtils;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.ImmutableList;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.crashlytics.internal.common.CommonUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.main.home.HomeScreen;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.main.sharing.SharingScreen;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.utils.IntentUtils;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;
import timber.log.Timber;

public final class Activity extends AppCompatActivity {

    private Navigator navigator;
    private RootScreenFactory rootScreenFactory;
    private SettingsDataStore settingsDataStore;

    private static WeakReference<Context> mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        handleRootedDevice();

        setTheme(R.style.Theme_Application);
        setTitle(""); // ACCESSIBILITY: prevents application name read during each activity launch
        super.onCreate(savedInstanceState);

        handleCrashOnPreviousExecution();

        if (!BuildConfig.BUILD_TYPE.contentEquals("debug")) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        Intent intent = getIntent();

        if ((Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) && intent.getType() != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(intent.getAction());
            handleIncomingFiles(intent);
        } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
            getIntent().setAction(Intent.ACTION_MAIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            restartAppWithIntent(intent);
        } else if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
            rootScreenFactory.intent(intent);
        }
        else {
          // Avoid blank screen on language change
          if (savedInstanceState != null) {
              restartAppWithIntent(intent);
          }
          rootScreenFactory.intent(intent);
        }

        mContext = new WeakReference<>(this);

        initializeApplicationFileTypesAssociation();

        navigator.onCreate(this, findViewById(android.R.id.content), savedInstanceState);
    }

    private void handleRootedDevice() {
        if (CommonUtils.isRooted(getApplicationContext())) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.setMessage(getResources().getString(R.string.rooted_device));
            errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), (dialog, which) -> dialog.cancel());
            errorDialog.show();
        }
    }

    private void handleCrashOnPreviousExecution() {
        if (FirebaseCrashlytics.getInstance().didCrashOnPreviousExecution()) {
            if (settingsDataStore.getAlwaysSendCrashReport()) {
                sendUnsentCrashReports();
                return;
            }
            Dialog crashReportDialog = new Dialog(this);
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
            dontSendButton.setOnClickListener(v -> {
                crashReportDialog.dismiss();
            });

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void restartAppWithIntent(Intent intent) {
        finish();
        startActivity(intent);
        overridePendingTransition (0, 0);
    }

    private void handleIncomingFiles(Intent intent) {
        try {
            intent.setDataAndType(intent.getData(), "*/*");
            rootScreenFactory.intent(intent);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, "Handling incoming file intent");
        }
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

    @Override
    protected void attachBaseContext(Context newBase) {
        Application.ApplicationComponent component = Application.component(newBase);
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
        navigator.onActivityResult(requestCode, resultCode, data);
    }

    public static WeakReference<Context> getContext() {
        return mContext;
    }

    @Singleton
    static final class RootScreenFactory implements Callable<Screen> {

        @Nullable private Intent intent;

        @Inject RootScreenFactory() {}

        void intent(Intent intent) {
            this.intent = intent;
        }


        @Override
        public Screen call() {
            if ((intent.getAction() != null && Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) && intent.getType() != null) {
                return chooseScreen(intent);
            } else if (intent.getAction() != null && Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
                return SharingScreen.create();
            }
            return HomeScreen.create(intent);
        }

        private Screen chooseScreen(Intent intent) {
            ImmutableList<FileStream> fileStreams = IntentUtils.parseGetContentIntent(getContext().get().getContentResolver(), intent);
            if (!CollectionUtils.isEmpty(fileStreams)) {
                String fileName = fileStreams.get(0).displayName();
                String extension = fileName.substring(fileName.lastIndexOf("."));
                if (".cdoc".equals(extension)) {
                    return CryptoCreateScreen.open(intent);
                }
            }
            return SignatureCreateScreen.create(intent);
        }
    }


}
