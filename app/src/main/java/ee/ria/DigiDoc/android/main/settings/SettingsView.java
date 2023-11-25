package ee.ria.DigiDoc.android.main.settings;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarViewTitle;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.main.settings.access.SettingsAccessScreen;
import ee.ria.DigiDoc.android.main.settings.role.SettingsRoleAndAddressScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class SettingsView extends CoordinatorLayout implements ContentView  {

    private final Toolbar toolbarView;
    private final AppBarLayout appBarLayout;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    private final Button accessCategory;
    private final Button roleAndAddressCategory;

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
        navigator = ApplicationApp.component(context).navigator();
        disposables = new ViewDisposables();

        accessCategory = findViewById(R.id.mainSettingsAccessCategory);
        roleAndAddressCategory = findViewById(R.id.mainSettingsRoleAndAddressCategory);

        toolbarView.setTitle(R.string.main_settings_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        TextView toolbarTitleView = getToolbarViewTitle(toolbarView);
        if (toolbarTitleView != null) {
            toolbarTitleView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        appBarLayout.postDelayed(() -> {
            appBarLayout.requestFocus();
            appBarLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            appBarLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        }, 1000);
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
        disposables.add(clicks(accessCategory).subscribe(o ->
                navigator.execute(
                        Transaction.push(SettingsAccessScreen.create()))));
        disposables.add(clicks(roleAndAddressCategory).subscribe(o ->
                navigator.execute(
                        Transaction.push(SettingsRoleAndAddressScreen.create()))));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
