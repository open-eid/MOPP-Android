package ee.ria.DigiDoc.android.main.settings.rights;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class SettingsRightsView extends CoordinatorLayout {

    private final Toolbar toolbarView;

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;

    private final ViewDisposables disposables;

    private final SwitchCompat openAllFileTypesSwitch;
    private final SwitchCompat allowScreenshotsSwitch;

    public SettingsRightsView(Context context) {
        this(context, null);
    }

    public SettingsRightsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsRightsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_settings_rights, this);
        toolbarView = findViewById(R.id.toolbar);
        navigator = ApplicationApp.component(context).navigator();
        settingsDataStore = ApplicationApp.component(context).settingsDataStore();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_settings_rights);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        openAllFileTypesSwitch = findViewById(R.id.mainSettingsOpenAllFileTypes);
        allowScreenshotsSwitch = findViewById(R.id.mainSettingsAllowScreenshots);

        if (settingsDataStore != null) {
            openAllFileTypesSwitch.setChecked(settingsDataStore.getIsOpenAllFileTypesEnabled());
            allowScreenshotsSwitch.setChecked(settingsDataStore.getIsScreenshotAllowed());
        }
    }

    private void restartIntent() {
        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getContext().getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent restartIntent = Intent.makeRestartActivityTask(componentName);
        restartIntent.setAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getContext().startActivity(restartIntent);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
        disposables.add(clicks(allowScreenshotsSwitch)
                .subscribe(o -> {
                    boolean isChecked = allowScreenshotsSwitch.isChecked();
                    settingsDataStore.setIsScreenshotAllowed(isChecked);
                    restartIntent();
                }));
        disposables.add(clicks(openAllFileTypesSwitch)
                .subscribe(o -> {
                    boolean isChecked = openAllFileTypesSwitch.isChecked();
                    settingsDataStore.setIsOpenAllFileTypesEnabled(isChecked);
                    restartIntent();
                }));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    public static void resetSettings(SettingsDataStore settingsDataStore) {
        settingsDataStore.setIsRoleAskingEnabled(false);
    }
}
