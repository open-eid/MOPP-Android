package ee.ria.DigiDoc.android.main.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

public final class SettingsView extends CoordinatorLayout implements ContentView  {

    private final Toolbar toolbarView;
    private final TextView toolbarTitleView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public SettingsView(Context context) {
        this(context, null);
    }

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_settings, this);
        toolbarView = findViewById(R.id.toolbar);
        toolbarTitleView = getToolbarViewTitle();
        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_settings_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        if (toolbarTitleView != null) {
            toolbarTitleView.setContentDescription("\u202F");
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

        addInvisibleElement(getContext(), this);
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
}
