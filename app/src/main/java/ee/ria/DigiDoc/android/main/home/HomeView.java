package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.support.design.widget.RxBottomNavigationView.itemSelections;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;

public final class HomeView extends LinearLayout implements MviView<Intent, ViewState> {

    private final BottomNavigationView navigationView;

    private final Navigator homeNavigator;
    private final HomeViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.NavigationIntent> navigationIntentSubject =
            PublishSubject.create();

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
        Navigator navigator = Application.component(context).navigator();
        viewModel = navigator.getViewModelProvider().get(HomeViewModel.class);
        homeNavigator = navigator.childNavigator(findViewById(R.id.mainHomeNavigationContainer));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), navigationIntent());
    }

    @Override
    public void render(ViewState state) {
        homeNavigator.setRootScreen(state.screen());
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.NavigationIntent> navigationIntent() {
        return navigationIntentSubject;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().filter(duplicates()).subscribe(this::render));
        viewModel.process(intents());
        disposables.add(itemSelections(navigationView).filter(duplicates()).subscribe(item ->
                navigationIntentSubject.onNext(Intent.NavigationIntent.create(item.getItemId()))));
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
