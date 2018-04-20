package ee.ria.DigiDoc.android.main.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.BottomNavigationView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.crypto.CryptoHomeScreen;
import ee.ria.DigiDoc.android.crypto.CryptoHomeView;
import ee.ria.DigiDoc.android.eid.EIDHomeScreen;
import ee.ria.DigiDoc.android.eid.EIDHomeView;
import ee.ria.DigiDoc.android.signature.home.SignatureHomeScreen;
import ee.ria.DigiDoc.android.signature.home.SignatureHomeView;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.support.design.widget.RxBottomNavigationView.itemSelections;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

@SuppressLint("ViewConstructor")
public final class HomeView extends LinearLayout implements MviView<Intent, ViewState> {

    private final String eidScreenId;

    private final FrameLayout navigationContainerView;
    private final BottomNavigationView navigationView;
    private final HomeMenuDialog menuDialog;
    private final HomeMenuView menuView;

    private final HomeViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.MenuIntent> menuIntentSubject = PublishSubject.create();

    public HomeView(Context context, String screenId) {
        super(context);
        eidScreenId = screenId + "eid";
        setOrientation(VERTICAL);
        inflate(context, R.layout.main_home, this);
        navigationContainerView = findViewById(R.id.mainHomeNavigationContainer);
        navigationView = findViewById(R.id.mainHomeNavigation);
        menuDialog = new HomeMenuDialog(context);
        menuView = menuDialog.getMenuView();
        viewModel = Application.component(context).navigator().viewModel(screenId,
                HomeViewModel.class);
        viewModel.eidScreenId(eidScreenId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), navigationIntent(), menuIntent());
    }

    @Override
    public void render(ViewState state) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        View view;
        if (state.screen() instanceof SignatureHomeScreen) {
            view = new SignatureHomeView(getContext());
        } else if (state.screen() instanceof CryptoHomeScreen) {
            view = new CryptoHomeView(getContext());
        } else if (state.screen() instanceof EIDHomeScreen) {
            view = new EIDHomeView(getContext(), eidScreenId);
        } else {
            throw new IllegalArgumentException("Unknown screen " + state.screen());
        }
        navigationContainerView.removeAllViews();
        navigationContainerView.addView(view, layoutParams);
        ((HomeToolbar.HomeToolbarAware) view).homeToolbar().overflowButtonClicks()
                .map(ignored -> Intent.MenuIntent.state(true))
                .subscribe(menuIntentSubject);
        if (state.menuOpen()) {
            menuDialog.show();
        } else {
            menuDialog.dismiss();
        }
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.NavigationIntent> navigationIntent() {
        return itemSelections(navigationView)
                .filter(duplicates())
                .map(item -> Intent.NavigationIntent.create(item.getItemId()));
    }

    private Observable<Intent.MenuIntent> menuIntent() {
        return Observable.merge(
                menuIntentSubject,
                cancels(menuDialog).map(ignored -> Intent.MenuIntent.state(false)),
                menuView.closeButtonClicks().map(ignored -> Intent.MenuIntent.state(false)),
                menuView.itemClicks().map(Intent.MenuIntent::navigate));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().filter(duplicates()).subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    protected void onDetachedFromWindow() {
        menuDialog.dismiss();
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
