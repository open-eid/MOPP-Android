package ee.ria.DigiDoc.android.crypto.home;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.widget.Button;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.main.home.HomeToolbar;
import ee.ria.DigiDoc.android.main.home.HomeView;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class CryptoHomeView extends CoordinatorLayout implements HomeView.HomeViewChild {

    private final HomeToolbar toolbarView;
    private final Button createButton;

    private final Navigator navigator;

    private final ViewDisposables disposables = new ViewDisposables();

    public CryptoHomeView(Context context) {
        this(context, null);
    }

    public CryptoHomeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CryptoHomeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.crypto_home, this);
        toolbarView = findViewById(R.id.toolbar);
        createButton = findViewById(R.id.cryptoHomeCreateButton);
        navigator = Application.component(context).navigator();
        AccessibilityUtils.setAccessibilityPaneTitle(this, R.string.main_home_navigation_crypto);
    }

    @Override
    public HomeToolbar homeToolbar() {
        return toolbarView;
    }

    @Override
    public Observable<Boolean> navigationViewVisibility() {
        return Observable.never();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(clicks(createButton).subscribe(ignored ->
                navigator.execute(Transaction.push(CryptoCreateScreen.create()))));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
