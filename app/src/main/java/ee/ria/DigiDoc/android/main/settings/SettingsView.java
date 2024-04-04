package ee.ria.DigiDoc.android.main.settings;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarImageButton;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarTextView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.main.settings.proxy.SettingsProxyDialog;
import ee.ria.DigiDoc.android.main.settings.rights.SettingsRightsScreen;
import ee.ria.DigiDoc.android.main.settings.rights.SettingsRightsView;
import ee.ria.DigiDoc.android.main.settings.signing.SettingsSigningScreen;
import ee.ria.DigiDoc.android.main.settings.signing.SettingsSigningView;
import ee.ria.DigiDoc.android.main.settings.signing.siva.SettingsSivaDialog;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class SettingsView extends CoordinatorLayout implements ContentView  {

    private final AppBarLayout appBarLayout;
    private final ScrollView scrollView;
    private final Toolbar toolbarView;

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;

    private final ViewDisposables disposables;

    private final Button defaultSettingsButton;

    private final Button accessCategory;
    private final Button rightsCategory;

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
        appBarLayout = findViewById(R.id.appBar);
        scrollView = findViewById(R.id.scrollView);
        navigator = ApplicationApp.component(context).navigator();
        settingsDataStore = ApplicationApp.component(context).settingsDataStore();
        disposables = new ViewDisposables();

        defaultSettingsButton = findViewById(R.id.mainSettingsUseDefaultSettings);

        defaultSettingsButton.setContentDescription(
                defaultSettingsButton.getText().toString().toLowerCase());
        accessCategory = findViewById(R.id.mainSettingsSigningCategory);
        rightsCategory = findViewById(R.id.mainSettingsRightsCategory);

        toolbarView.setTitle(R.string.main_settings_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);
    }

    private void resetToDefaultSettings(SettingsDataStore settingsDataStore) {
        SettingsSigningView.resetSettings(getContext(), settingsDataStore);
        SettingsRightsView.resetSettings(settingsDataStore);
        SettingsSivaDialog.resetSettings(getContext(), settingsDataStore);
        SettingsProxyDialog.resetSettings(getContext(), settingsDataStore);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        scrollView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        TextView toolbarTextView = getToolbarTextView(toolbarView);
        if (toolbarTextView != null) {
            toolbarTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        ImageButton toolbarImageButton = getToolbarImageButton(toolbarView);
        if (toolbarImageButton != null) {
            toolbarImageButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        appBarLayout.postDelayed(() -> {
            scrollView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            if (toolbarImageButton != null) {
                toolbarImageButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }, 1000);
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
        disposables.add(clicks(accessCategory).subscribe(o ->
                navigator.execute(
                        Transaction.push(SettingsSigningScreen.create()))));
        disposables.add(clicks(rightsCategory).subscribe(o ->
                navigator.execute(
                        Transaction.push(SettingsRightsScreen.create()))));
        disposables.add(clicks(defaultSettingsButton).subscribe(o -> {
            resetToDefaultSettings(settingsDataStore);
                    ToastUtil.showError(navigator.activity(),
                            R.string.main_settings_use_default_settings_message);
        }
        ));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}