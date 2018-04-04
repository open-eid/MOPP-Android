package ee.ria.DigiDoc.android.eid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.main.home.HomeToolbar;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import io.reactivex.Observable;

@SuppressLint("ViewConstructor")
public final class EIDHomeView extends CoordinatorLayout implements MviView<Intent, ViewState>,
        HomeToolbar.HomeToolbarAware {

    private final HomeToolbar toolbarView;

    private final ViewDisposables disposables = new ViewDisposables();
    private final EIDHomeViewModel viewModel;

    public EIDHomeView(Context context, String screenId) {
        super(context);
        inflate(context, R.layout.eid_home, this);
        toolbarView = findViewById(R.id.toolbar);
        viewModel = Application.component(context).navigator().viewModel(screenId,
                EIDHomeViewModel.class);
    }

    public Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent());
    }

    @Override
    public void render(ViewState state) {
    }

    @Override
    public HomeToolbar homeToolbar() {
        return toolbarView;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
