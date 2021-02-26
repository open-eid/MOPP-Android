package ee.ria.DigiDoc.android.main.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class SettingsView extends CoordinatorLayout {

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

        if (toolbarTitleView != null) {
            toolbarTitleView.setContentDescription("\u202F");
            toolbarTitleView.postDelayed(() -> {
                toolbarTitleView.requestFocus();
                toolbarTitleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 2500);
        }
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
