package ee.ria.DigiDoc.android.eid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.main.home.HomeToolbar;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardStatus;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import io.reactivex.Observable;

import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

@SuppressLint("ViewConstructor")
public final class EIDHomeView extends CoordinatorLayout implements MviView<Intent, ViewState>,
        HomeToolbar.HomeToolbarAware {

    private static final ImmutableMap<String, Integer> STATUS_MESSAGES =
            ImmutableMap.<String, Integer>builder()
                    .put(IdCardStatus.INITIAL, R.string.eid_home_id_card_status_initial_message)
                    .put(IdCardStatus.READER_DETECTED,
                            R.string.eid_home_id_card_status_reader_detected_message)
                    .put(IdCardStatus.CARD_DETECTED,
                            R.string.eid_home_id_card_status_card_detected_message)
                    .build();

    private final String screenId;

    private final HomeToolbar toolbarView;
    private final TextView progressMessageView;
    private final EIDDataView dataView;
    private final AlertDialog errorDialog;

    private final ViewDisposables disposables = new ViewDisposables();
    private final Navigator navigator;
    private EIDHomeViewModel viewModel;

    public EIDHomeView(Context context, String screenId) {
        super(context);
        this.screenId = screenId;
        inflate(context, R.layout.eid_home, this);
        toolbarView = findViewById(R.id.toolbar);
        progressMessageView = findViewById(R.id.eidHomeProgressMessage);
        dataView = findViewById(R.id.eidHomeData);
        errorDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.eid_home_error)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                .create();
        navigator = Application.component(context).navigator();
    }

    public Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    public Observable<Intent.LoadIntent> loadIntent() {
        return cancels(errorDialog)
                .map(ignored -> Intent.LoadIntent.create());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), loadIntent());
    }

    @Override
    public void render(ViewState state) {
        progressMessageView.setText(STATUS_MESSAGES.get(state.idCardDataResponse().status()));
        IdCardData data = state.idCardDataResponse().data();
        if (data != null) {
            dataView.setData(data);
        }
        progressMessageView.setVisibility(data == null ? VISIBLE : GONE);
        dataView.setVisibility(data == null ? GONE : VISIBLE);

        if (state.error() != null) {
            errorDialog.show();
        } else {
            errorDialog.dismiss();
        }
    }

    @Override
    public HomeToolbar homeToolbar() {
        return toolbarView;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        viewModel = navigator.viewModel(screenId, EIDHomeViewModel.class);
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    public void onDetachedFromWindow() {
        errorDialog.dismiss();
        disposables.detach();
        navigator.clearViewModel(screenId);
        super.onDetachedFromWindow();
    }
}
