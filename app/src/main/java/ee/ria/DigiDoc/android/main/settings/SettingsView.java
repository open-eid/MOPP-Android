package ee.ria.DigiDoc.android.main.settings;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarViewTitle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.main.settings.access.SettingsAccessScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class SettingsView extends CoordinatorLayout {

    private final Toolbar toolbarView;

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
        TextView toolbarTitleView = getToolbarViewTitle(toolbarView);
        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();

        Button accessCategory = findViewById(R.id.mainSettingsAccessCategory);

        accessCategory.setOnClickListener(view ->
                navigator.execute(
                        Transaction.push(SettingsAccessScreen.create()))
        );

        toolbarView.setTitle(R.string.main_settings_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        if (toolbarTitleView != null) {
            toolbarTitleView.setContentDescription("\u202F");
        }
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
