package ee.ria.DigiDoc.android.signature.home;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.widget.Button;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.main.home.HomeToolbar;
import ee.ria.DigiDoc.android.main.home.HomeView;
import ee.ria.DigiDoc.android.signature.create.SignatureCreateScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class SignatureHomeView extends CoordinatorLayout implements HomeView.HomeViewChild {

    private final HomeToolbar toolbarView;
    private final Button createButton;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public SignatureHomeView(Context context) {
        this(context, null);
    }

    public SignatureHomeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureHomeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_home, this);
        toolbarView = findViewById(R.id.toolbar);
        createButton = findViewById(R.id.signatureHomeCreateButton);
        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();
        AccessibilityUtils.setAccessibilityPaneTitle(this, R.string.main_home_navigation_signature);
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
        disposables.add(clicks(createButton).subscribe(o ->
                navigator.execute(Transaction.push(SignatureCreateScreen.create(null)))));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
