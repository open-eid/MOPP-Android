package ee.ria.DigiDoc.android.eid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.main.home.HomeToolbar;
import ee.ria.DigiDoc.android.main.home.HomeView;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;
import ee.ria.DigiDoc.idcard.CodeVerificationException;
import ee.ria.DigiDoc.idcard.Token;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderStatus;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import static ee.ria.DigiDoc.android.utils.InputMethodUtils.hideSoftKeyboard;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

@SuppressLint("ViewConstructor")
public final class EIDHomeView extends FrameLayout implements MviView<Intent, ViewState>,
        HomeView.HomeViewChild, Navigator.BackButtonClickListener {

    private static final ImmutableMap<SmartCardReaderStatus, Integer> STATUS_MESSAGES =
            ImmutableMap.<SmartCardReaderStatus, Integer>builder()
                    .put(SmartCardReaderStatus.IDLE,
                            R.string.eid_home_id_card_status_initial_message)
                    .put(SmartCardReaderStatus.READER_DETECTED,
                            R.string.eid_home_id_card_status_reader_detected_message)
                    .put(SmartCardReaderStatus.CARD_DETECTED,
                            R.string.eid_home_id_card_status_card_detected_message)
                    .build();

    private final String screenId;

    private final CoordinatorLayout coordinatorView;
    private final HomeToolbar toolbarView;
    private final TextView progressMessageView;
    private final EIDDataView dataView;
    private final ErrorDialog errorDialog;
    private final CodeUpdateView codeUpdateView;
    private final ErrorDialog codeUpdateErrorDialog;

    private final ViewDisposables disposables = new ViewDisposables();
    private final Navigator navigator;

    private final Subject<Intent.CodeUpdateIntent> codeUpdateIntentSubject =
            PublishSubject.create();
    private final Subject<Boolean> navigationViewVisibilitySubject = PublishSubject.create();

    @Nullable private Token token;
    @Nullable private IdCardData data;
    @Nullable private CodeUpdateAction codeUpdateAction;

    public EIDHomeView(Context context, String screenId) {
        super(context);
        this.screenId = screenId;
        inflate(context, R.layout.eid_home, this);
        coordinatorView = findViewById(R.id.eidHomeCoordinator);
        toolbarView = findViewById(R.id.toolbar);
        progressMessageView = findViewById(R.id.eidHomeProgressMessage);
        dataView = findViewById(R.id.eidHomeData);

        errorDialog = new ErrorDialog(context);
        errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), (dialog, which) -> dialog.cancel());
        errorDialog.setMessage(getResources().getString(R.string.eid_home_error));

        codeUpdateView = findViewById(R.id.eidHomeCodeUpdate);
        codeUpdateErrorDialog = new ErrorDialog(context);
        codeUpdateErrorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), (dialog, which) -> dialog.cancel());
        
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
        return dataView.certificateContainerStates()
                .map(Intent.CertificatesTitleClickIntent::create);
    }

    private Observable<Intent.CodeUpdateIntent> codeUpdateIntent() {
        //noinspection unchecked
        return Observable
                .mergeArray(dataView.actions().map(Intent.CodeUpdateIntent::show),
                        codeUpdateView.closeButtonClick().map(ignored -> Intent.CodeUpdateIntent.clear(codeUpdateAction)),
                        codeUpdateView.backButtonClick().map(ignored -> Intent.CodeUpdateIntent.clear()),
                        codeUpdateView.requests()
                                .filter(ignored ->
                                        codeUpdateAction != null && data != null && token != null)
                                .map(request ->
                                        Intent.CodeUpdateIntent
                                                .request(codeUpdateAction, request, data, token)),
                        cancels(codeUpdateErrorDialog)
                                .doOnNext(ignored -> hideSoftKeyboard(this))
                                .map(ignored -> Intent.CodeUpdateIntent.clear()),
                        codeUpdateIntentSubject);
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
        token = state.idCardDataResponse().token();
        data = state.idCardDataResponse().data();
        if (data != null) {
            dataView.render(data, state.certificatesContainerExpanded());
        }
        progressMessageView.setVisibility(data == null ? VISIBLE : GONE);
        dataView.setVisibility(data == null ? GONE : VISIBLE);

        codeUpdateAction = state.codeUpdateAction();
        CodeUpdateResponse codeUpdateResponse = state.codeUpdateResponse();
        navigationViewVisibilitySubject.onNext(codeUpdateAction == null);
        coordinatorView.setVisibility(codeUpdateAction == null ? VISIBLE : GONE);
        codeUpdateView.setVisibility(codeUpdateAction == null ? GONE : VISIBLE);
        if (codeUpdateAction != null) {
            codeUpdateView.render(state.codeUpdateState(), codeUpdateAction, codeUpdateResponse,
                    state.codeUpdateSuccessMessageVisible());
        } else {
            codeUpdateView.clear();
        }

        Throwable error = state.error();
        Throwable codeUpdateError = codeUpdateResponse == null ? null : codeUpdateResponse.error();
        if (error != null) {
            codeUpdateErrorDialog.dismiss();
            errorDialog.show();
        } else if (codeUpdateAction != null && codeUpdateError != null) {
            errorDialog.dismiss();
            if (codeUpdateError instanceof CodeVerificationException) {
                codeUpdateErrorDialog.setMessage(getResources()
                        .getString(codeUpdateAction.currentBlockedErrorRes()));
            } else {
                codeUpdateErrorDialog.setMessage(getResources().getString(R.string.eid_home_error));
            }
            codeUpdateErrorDialog.show();
            TextView codeUpdateErrorDialogMessage = codeUpdateErrorDialog.findViewById(android.R.id.message);
            AccessibilityUtils.setContentDescription(codeUpdateErrorDialogMessage, getResources()
                    .getString(codeUpdateAction.currentBlockedErrorRes()));
        } else {
            errorDialog.dismiss();
            codeUpdateErrorDialog.dismiss();
        }

        if (progressMessageView.getVisibility() == VISIBLE) {
            AccessibilityUtils.sendAccessibilityEvent(getContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, progressMessageView.getText());
        }
    }

    @Override
    public boolean onBackButtonClick() {
        if (codeUpdateView.getVisibility() == VISIBLE) {
            codeUpdateIntentSubject.onNext(Intent.CodeUpdateIntent.clear());
            return true;
        }
        return false;
    }

    @Override
    public HomeToolbar homeToolbar() {
        return toolbarView;
    }

    @Override
    public Observable<Boolean> navigationViewVisibility() {
        return navigationViewVisibilitySubject;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        navigator.addBackButtonClickListener(this);
        EIDHomeViewModel viewModel = navigator.viewModel(screenId, EIDHomeViewModel.class);
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    public void onDetachedFromWindow() {
        errorDialog.dismiss();
        codeUpdateErrorDialog.dismiss();
        disposables.detach();
        navigator.removeBackButtonClickListener(this);
        super.onDetachedFromWindow();
    }
}
