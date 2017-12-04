package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.crypto.CryptoHomeScreen;
import ee.ria.DigiDoc.android.eid.EIDHomeScreen;
import ee.ria.DigiDoc.android.signature.home.SignatureHomeScreen;
import ee.ria.DigiDoc.android.utils.Predicates;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.navigation.Screen;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import static com.jakewharton.rxbinding2.support.design.widget.RxBottomNavigationView.itemSelections;

public final class HomeView extends LinearLayout implements MviView<HomeIntent, HomeViewState> {

    private final BottomNavigationView navigationView;

    @Nullable private CompositeDisposable disposables;

    private final HomeViewModel viewModel;
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

        viewModel = new HomeViewModel();
        homeNavigator = Application.component(context).navigator()
                .childNavigator(findViewById(R.id.mainHomeNavigationContainer));
    }

    @Override
    public Observable<HomeIntent> intents() {
        return Observable.merge(navigationIntents(), Observable.empty());
    }

    @Override
    public void render(HomeViewState state) {
        Timber.d("render: %s", state);

        Screen screen;
        switch (state.currentScreen()) {
            case R.id.signatureHomeScreen:
                screen = SignatureHomeScreen.create();
                break;
            case R.id.cryptoHomeScreen:
                screen = CryptoHomeScreen.create();
                break;
            case R.id.eidHomeScreen:
                screen = EIDHomeScreen.create();
                break;
            default:
                throw new IllegalArgumentException("Unknown navigation item " + state);
        }
        homeNavigator.setRootScreen(screen);
    }

    private Observable<HomeIntent.NavigationIntent> navigationIntents() {
        return itemSelections(navigationView)
                .filter(Predicates.duplicates())
                .map(item -> HomeIntent.NavigationIntent.create(item.getItemId()));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (disposables != null) {
            disposables.dispose();
        }
        disposables = new CompositeDisposable();

        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
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
