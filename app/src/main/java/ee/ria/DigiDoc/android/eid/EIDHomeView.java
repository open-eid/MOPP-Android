package ee.ria.DigiDoc.android.eid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.widget.FrameLayout;
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
public final class EIDHomeView extends FrameLayout implements MviView<Intent, ViewState>,
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

    private final CoordinatorLayout coordinatorView;
    private final HomeToolbar toolbarView;
    private final TextView progressMessageView;
    private final EIDDataView dataView;
    private final AlertDialog errorDialog;
    private final CodeUpdateView codeUpdateView;

    private final ViewDisposables disposables = new ViewDisposables();
    private final Navigator navigator;

    public EIDHomeView(Context context, String screenId) {
        super(context);
        this.screenId = screenId;
        inflate(context, R.layout.eid_home, this);
        coordinatorView = findViewById(R.id.eidHomeCoordinator);
        toolbarView = findViewById(R.id.toolbar);
        progressMessageView = findViewById(R.id.eidHomeProgressMessage);
        dataView = findViewById(R.id.eidHomeData);
        errorDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.eid_home_error)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                .create();
        codeUpdateView = findViewById(R.id.eidHomeCodeUpdate);
        navigator = Application.component(context).navigator();
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.LoadIntent> loadIntent() {
        return cancels(errorDialog)
                .map(ignored -> Intent.LoadIntent.create());
    }

    private Observable<Intent.CertificatesTitleClickIntent> certificatesTitleClickIntent() {
        return dataView
                .certificateTitleClicks()
                .map(ignored ->
                        Intent.CertificatesTitleClickIntent
                                .create(!dataView.certificateContainerExpanded()));
    }

    private Observable<Intent.CodeUpdateIntent> codeUpdateIntent() {
        return Observable
                .merge(dataView.actions().map(Intent.CodeUpdateIntent::show),
                        codeUpdateView.upClicks().map(ignored -> Intent.CodeUpdateIntent.clear()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), loadIntent(), certificatesTitleClickIntent(),
                codeUpdateIntent());
    }

    @Override
    public void render(ViewState state) {
        progressMessageView.setText(STATUS_MESSAGES.get(state.idCardDataResponse().status()));
        IdCardData data = state.idCardDataResponse().data();
        if (data != null) {
            dataView.setData(data);
        }
        dataView.certificateContainerExpanded(state.certificatesContainerExpanded());
        progressMessageView.setVisibility(data == null ? VISIBLE : GONE);
        dataView.setVisibility(data == null ? GONE : VISIBLE);

        if (state.error() != null) {
            errorDialog.show();
        } else {
            errorDialog.dismiss();
        }

        CodeUpdateAction codeUpdateAction = state.codeUpdateAction();
        coordinatorView.setVisibility(codeUpdateAction == null ? VISIBLE : GONE);
        codeUpdateView.setVisibility(codeUpdateAction == null ? GONE : VISIBLE);
        if (codeUpdateAction != null) {
            codeUpdateView.action(codeUpdateAction);
        }
    }

    @Override
    public HomeToolbar homeToolbar() {
        return toolbarView;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EIDHomeViewModel viewModel = navigator.viewModel(screenId, EIDHomeViewModel.class);
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    public void onDetachedFromWindow() {
        errorDialog.dismiss();
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
