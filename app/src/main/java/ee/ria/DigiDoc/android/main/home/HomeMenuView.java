package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import ee.ria.DigiDoc.R;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static com.jakewharton.rxbinding2.widget.RxRadioGroup.checkedChanges;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;

public final class HomeMenuView extends NestedScrollView {

    private final View closeButton;

    private final TextView helpView;
    private final TextView recentView;
    private final TextView settingsView;
    private final TextView aboutView;
    private final TextView diagnosticsView;
    private final RadioGroup localeView;

    public HomeMenuView(@NonNull Context context) {
        this(context, null);
    }

    public HomeMenuView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeMenuView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_home_menu, this);
        closeButton = findViewById(R.id.mainHomeMenuCloseButton);
        closeButton.setContentDescription(getResources().getString(R.string.close_menu));
        helpView = findViewById(R.id.mainHomeMenuHelp);
        helpView.setContentDescription(
                getResources().getString(R.string.main_home_menu_help) +
                " link to www.id.ee");
        recentView = findViewById(R.id.mainHomeMenuRecent);
        settingsView = findViewById(R.id.mainHomeMenuSettings);
        aboutView = findViewById(R.id.mainHomeMenuAbout);
        diagnosticsView = findViewById(R.id.mainHomeMenuDiagnostics);
        localeView = findViewById(R.id.mainHomeMenuLocale);

        tintCompoundDrawables(helpView);
        tintCompoundDrawables(recentView);
        tintCompoundDrawables(settingsView);
        tintCompoundDrawables(aboutView);
        tintCompoundDrawables(diagnosticsView);

        helpView.postDelayed(() -> {
            helpView.requestFocus();
            helpView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 2000);
    }

    public Observable<Object> closeButtonClicks() {
        return clicks(closeButton);
    }

    @SuppressWarnings("unchecked")
    public Observable<Integer> itemClicks() {
        return Observable.mergeArray(
                clicks(helpView).map(ignored -> R.id.mainHomeMenuHelp),
                clicks(recentView).map(ignored -> R.id.mainHomeMenuRecent),
                clicks(settingsView).map(ignored -> R.id.mainHomeMenuSettings),
                clicks(aboutView).map(ignored -> R.id.mainHomeMenuAbout),
                clicks(diagnosticsView).map(ignored -> R.id.mainHomeMenuDiagnostics));
    }

    public void locale(@Nullable Integer locale) {
        if (locale == null) {
            localeView.clearCheck();
        } else {
            localeView.check(locale);
        }
    }

    public Observable<Integer> localeChecks() {
        return checkedChanges(localeView).skipInitialValue();
    }
}
