package ee.ria.DigiDoc.android.main;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.crypto.CryptoHomeScreen;
import ee.ria.DigiDoc.android.eid.EIDHomeScreen;
import ee.ria.DigiDoc.android.signature.SignatureHomeScreen;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.navigation.Screen;
import io.reactivex.disposables.CompositeDisposable;

import static com.jakewharton.rxbinding2.support.design.widget.RxBottomNavigationView.itemSelections;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;

public final class HomeView extends LinearLayout {

    private final BottomNavigationView navigationView;

    @Nullable private CompositeDisposable disposables;

    private final Navigator homeNavigator;

    public HomeView(Context context) {
        this(context, null);
    }

    public HomeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HomeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.main_home, this);
        navigationView = findViewById(R.id.mainHomeNavigation);

        homeNavigator = Application.component(context).navigator()
                .childNavigator(findViewById(R.id.mainHomeNavigationContainer));
    }

    void navigationItemChanged(MenuItem item) {
        Screen screen;
        switch (item.getItemId()) {
            case R.id.mainHomeNavigationSignature:
                screen = SignatureHomeScreen.create();
                break;
            case R.id.mainHomeNavigationCrypto:
                screen = CryptoHomeScreen.create();
                break;
            case R.id.mainHomeNavigationEID:
                screen = EIDHomeScreen.create();
                break;
            default:
                throw new IllegalArgumentException("Unknown navigation item " + item);
        }
        homeNavigator.setRootScreen(screen);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (disposables != null) {
            disposables.dispose();
        }
        disposables = new CompositeDisposable();

        disposables.add(itemSelections(navigationView)
                .filter(duplicates())
                .subscribe(this::navigationItemChanged));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (disposables != null) {
            disposables.dispose();
            disposables = null;
        }
        super.onDetachedFromWindow();
    }
}
