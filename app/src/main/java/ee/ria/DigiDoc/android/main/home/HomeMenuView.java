package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import ee.ria.DigiDoc.R;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;

public final class HomeMenuView extends NestedScrollView {

    private final View closeButton;

    private final TextView helpView;
    private final TextView recentView;
    private final TextView settingsView;
    private final TextView aboutView;
    private final TextView diagnosticsView;

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
        helpView = findViewById(R.id.mainHomeMenuHelp);
        recentView = findViewById(R.id.mainHomeMenuRecent);
        settingsView = findViewById(R.id.mainHomeMenuSettings);
        aboutView = findViewById(R.id.mainHomeMenuAbout);
        diagnosticsView = findViewById(R.id.mainHomeMenuDiagnostics);

        tintCompoundDrawables(helpView);
        tintCompoundDrawables(recentView);
        tintCompoundDrawables(settingsView);
        tintCompoundDrawables(aboutView);
        tintCompoundDrawables(diagnosticsView);
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
}
