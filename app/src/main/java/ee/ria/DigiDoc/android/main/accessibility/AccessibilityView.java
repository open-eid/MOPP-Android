package ee.ria.DigiDoc.android.main.accessibility;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarImageButton;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarTextView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public class AccessibilityView extends CoordinatorLayout implements ContentView {

    private final AppBarLayout appBarLayout;
    private final NestedScrollView scrollView;
    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public AccessibilityView(Context context) {
        this(context, null);
    }

    public AccessibilityView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AccessibilityView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_accessibility, this);
        toolbarView = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBar);
        scrollView = findViewById(R.id.scrollView);
        navigator = ApplicationApp.component(context).navigator();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_accessibility_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        ContentView.addInvisibleElement(getContext(), this);
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
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
