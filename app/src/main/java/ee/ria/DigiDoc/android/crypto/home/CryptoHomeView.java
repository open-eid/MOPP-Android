package ee.ria.DigiDoc.android.crypto.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.main.home.HomeToolbar;
import ee.ria.DigiDoc.android.main.home.HomeView;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.rxjava3.core.Observable;

import static com.jakewharton.rxbinding4.view.RxView.clicks;

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

        createButton.postDelayed(() -> {
            createButton.requestFocus();
            createButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 2000);
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
