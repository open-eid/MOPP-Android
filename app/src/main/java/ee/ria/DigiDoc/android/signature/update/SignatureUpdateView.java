package ee.ria.DigiDoc.android.signature.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdStatusMessages;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse.ProcessStatus;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

@SuppressLint("ViewConstructor")
public final class SignatureUpdateView extends LinearLayout implements MviView<Intent, ViewState> {

    private static final int DEFAULT_SIGN_METHOD = R.id.signatureUpdateSignatureAddMethodMobileId;

    private final boolean isExistingContainer;
    private final boolean isNestedContainer;
    private final File containerFile;
    private final boolean signatureAddVisible;
    private final boolean signatureAddSuccessMessageVisible;

    private final Toolbar toolbarView;
    private final NameUpdateDialog nameUpdateDialog;
    private final RecyclerView listView;
    private final SignatureUpdateAdapter adapter;
    private final View activityIndicatorView;
    private final View activityOverlayView;
    private final View mobileIdContainerView;
    private final TextView mobileIdStatusView;
    private final TextView mobileIdChallengeView;
    private final Button sendButton;
    private final View buttonSpace;
    private final Button signatureAddButton;
    private final ErrorDialog errorDialog;
    private final ConfirmationDialog documentRemoveConfirmationDialog;
    private final ConfirmationDialog signatureRemoveConfirmationDialog;
    private final SignatureUpdateSignatureAddDialog signatureAddDialog;
    private final SignatureUpdateSignatureAddView signatureAddView;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.DocumentsAddIntent> documentsAddIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.DocumentOpenIntent> documentOpenIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.DocumentRemoveIntent> documentRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureRemoveIntent> signatureRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureAddIntent> signatureAddIntentSubject =
            PublishSubject.create();

    @Nullable private DataFile documentRemoveConfirmation;
    @Nullable private Signature signatureRemoveConfirmation;

    public SignatureUpdateView(Context context, String screenId, boolean isExistingContainer,
                               boolean isNestedContainer, File containerFile,
                               boolean signatureAddVisible,
                               boolean signatureAddSuccessMessageVisible) {
        super(context);
        this.isExistingContainer = isExistingContainer;
        this.isNestedContainer = isNestedContainer;
        this.containerFile = containerFile;
        this.signatureAddVisible = signatureAddVisible;
        this.signatureAddSuccessMessageVisible = signatureAddSuccessMessageVisible;

        navigator = Application.component(context).navigator();
        viewModel = navigator.viewModel(screenId, SignatureUpdateViewModel.class);

        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update, this);
        toolbarView = findViewById(R.id.toolbar);
        nameUpdateDialog = new NameUpdateDialog(context);
        listView = findViewById(R.id.signatureUpdateList);
        activityIndicatorView = findViewById(R.id.activityIndicator);
        activityOverlayView = findViewById(R.id.activityOverlay);
        mobileIdContainerView = findViewById(R.id.signatureUpdateMobileIdContainer);
        mobileIdStatusView = findViewById(R.id.signatureUpdateMobileIdStatus);
        mobileIdChallengeView = findViewById(R.id.signatureUpdateMobileIdChallenge);
        sendButton = findViewById(R.id.signatureUpdateSendButton);
        buttonSpace = findViewById(R.id.signatureUpdateButtonSpace);
        signatureAddButton = findViewById(R.id.signatureUpdateSignatureAddButton);

        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(adapter = new SignatureUpdateAdapter());

        errorDialog = new ErrorDialog(context, documentsAddIntentSubject,
                documentRemoveIntentSubject, signatureAddIntentSubject,
                signatureRemoveIntentSubject);
        documentRemoveConfirmationDialog = new ConfirmationDialog(context,
                R.string.signature_update_document_remove_confirmation_message);
        signatureRemoveConfirmationDialog = new ConfirmationDialog(context,
                R.string.signature_update_signature_remove_confirmation_message);
        signatureAddDialog = new SignatureUpdateSignatureAddDialog(context);
        signatureAddView = signatureAddDialog.view();
        resetSignatureAddDialog();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), nameUpdateIntent(), addDocumentsIntent(),
                openDocumentIntent(), documentRemoveIntent(), signatureRemoveIntent(),
                signatureAddIntent(), sendIntent());
    }

    @Override
    public void render(ViewState state) {
        if (state.containerLoadError() != null) {
            Toast.makeText(getContext(), R.string.signature_update_container_load_error,
                    Toast.LENGTH_LONG).show();
            navigator.execute(Transaction.pop());
            return;
        }
        if (state.documentOpenFile() != null) {
            getContext().startActivity(createViewIntent(getContext(), state.documentOpenFile(),
                    SignedContainer.mimeType(state.documentOpenFile())));
            documentOpenIntentSubject.onNext(Intent.DocumentOpenIntent.clear());
            return;
        }

        nameUpdateDialog.render(state.nameUpdateShowing(), state.nameUpdateName(),
                state.nameUpdateError(), state.container());

        toolbarView.setTitle(isExistingContainer
                ? R.string.signature_update_title_existing
                : R.string.signature_update_title_created);
        if (isNestedContainer) {
            sendButton.setVisibility(GONE);
            buttonSpace.setVisibility(GONE);
            signatureAddButton.setVisibility(GONE);
        } else {
            sendButton.setVisibility(isExistingContainer ? VISIBLE : GONE);
            buttonSpace.setVisibility(isExistingContainer ? VISIBLE : GONE);
            signatureAddButton.setVisibility(VISIBLE);
        }

        setActivity(state.containerLoadInProgress() || state.documentsAddInProgress()
                || state.documentOpenInProgress() || state.documentRemoveInProgress()
                || state.signatureRemoveInProgress() || state.signatureAddActivity());
        adapter.setData(state.signatureAddSuccessMessageVisible(), isExistingContainer,
                isNestedContainer, state.container());

        errorDialog.show(state.documentsAddError(), state.documentRemoveError(),
                state.signatureAddError(), state.signatureRemoveError());

        documentRemoveConfirmation = state.documentRemoveConfirmation();
        if (documentRemoveConfirmation != null) {
            documentRemoveConfirmationDialog.show();
        } else {
            documentRemoveConfirmationDialog.dismiss();
        }

        signatureRemoveConfirmation = state.signatureRemoveConfirmation();
        if (signatureRemoveConfirmation != null) {
            signatureRemoveConfirmationDialog.show();
        } else {
            signatureRemoveConfirmationDialog.dismiss();
        }

        Integer signatureAddMethod = state.signatureAddMethod();
        if (signatureAddMethod == null) {
            signatureAddDialog.dismiss();
        } else {
            signatureAddDialog.show();
            signatureAddView.method(signatureAddMethod);
        }

        SignatureAddResponse signatureAddResponse = state.signatureAddResponse();
        signatureAddView.response(signatureAddResponse);
        // should be in the MobileIdView in dialog
        mobileIdContainerView.setVisibility(
                signatureAddResponse != null && signatureAddResponse instanceof MobileIdResponse
                        ? VISIBLE
                        : GONE);
        if (signatureAddResponse instanceof MobileIdResponse) {
            MobileIdResponse mobileIdResponse = (MobileIdResponse) signatureAddResponse;
            ProcessStatus mobileIdStatus = mobileIdResponse.status() == null
                    ? ProcessStatus.DEFAULT
                    : mobileIdResponse.status();
            mobileIdStatusView.setText(
                    MobileIdStatusMessages.message(getContext(), mobileIdStatus));
            String mobileIdChallenge = mobileIdResponse.challenge();
            if (mobileIdChallenge != null) {
                mobileIdChallengeView.setText(mobileIdChallenge);
            } else {
                mobileIdChallengeView.setText(
                        R.string.signature_add_mobile_id_challenge_placeholder);
            }
        }

        tintCompoundDrawables(sendButton);
        tintCompoundDrawables(signatureAddButton);
    }

    private void setActivity(boolean activity) {
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
        sendButton.setEnabled(!activity);
        signatureAddButton.setEnabled(!activity);
    }

    private void resetSignatureAddDialog() {
        signatureAddView.reset(viewModel);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(isExistingContainer, containerFile,
                signatureAddVisible ? DEFAULT_SIGN_METHOD : null,
                signatureAddSuccessMessageVisible));
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.NameUpdateIntent> nameUpdateIntent() {
        return Observable.mergeArray(
                adapter.nameUpdateClicks()
                        .map(ignored -> Intent.NameUpdateIntent.show(containerFile)),
                cancels(nameUpdateDialog).map(ignored -> Intent.NameUpdateIntent.clear()),
                nameUpdateDialog.updates()
                        .map(name -> Intent.NameUpdateIntent.update(containerFile, name)));
    }

    private Observable<Intent.DocumentsAddIntent> addDocumentsIntent() {
        return adapter.documentAddClicks()
                .map(ignored -> Intent.DocumentsAddIntent.create(containerFile))
                .mergeWith(documentsAddIntentSubject);
    }

    private Observable<Intent.DocumentOpenIntent> openDocumentIntent() {
        return documentOpenIntentSubject;
    }

    private Observable<Intent.DocumentRemoveIntent> documentRemoveIntent() {
        return documentRemoveIntentSubject;
    }

    private Observable<Intent.SignatureRemoveIntent> signatureRemoveIntent() {
        return signatureRemoveIntentSubject;
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.SignatureAddIntent> signatureAddIntent() {
        return Observable.mergeArray(
                clicks(signatureAddButton)
                        .doOnNext(ignored -> resetSignatureAddDialog())
                        .map(ignored -> Intent.SignatureAddIntent
                                .show(DEFAULT_SIGN_METHOD, isExistingContainer, containerFile)),
                cancels(signatureAddDialog)
                        .doOnNext(ignored -> resetSignatureAddDialog())
                        .map(ignored -> Intent.SignatureAddIntent.clear()),
                signatureAddView.methodChanges().map(method ->
                        Intent.SignatureAddIntent.show(method, isExistingContainer, containerFile)),
                signatureAddDialog.positiveButtonClicks().map(ignored ->
                        Intent.SignatureAddIntent.sign(signatureAddView.method(),
                                isExistingContainer, containerFile, signatureAddView.request())),
                signatureAddIntentSubject);
    }

    private Observable<Intent.SendIntent> sendIntent() {
        return clicks(sendButton)
                .map(ignored -> Intent.SendIntent.create(containerFile));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
        disposables.add(adapter.scrollToTop().subscribe(ignored -> listView.scrollToPosition(0)));
        disposables.add(adapter.documentRemoveClicks().subscribe(document ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent
                        .showConfirmation(containerFile, document))));
        disposables.add(documentRemoveConfirmationDialog.positiveButtonClicks().subscribe(ignored ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent
                        .remove(containerFile, documentRemoveConfirmation))));
        disposables.add(documentRemoveConfirmationDialog.cancels().subscribe(ignored ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent.clear())));
        disposables.add(adapter.documentClicks().subscribe(document ->
                documentOpenIntentSubject.onNext(Intent.DocumentOpenIntent
                        .open(containerFile, document))));
        disposables.add(adapter.signatureRemoveClicks().subscribe(signature ->
                signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent
                        .showConfirmation(containerFile, signature))));
        disposables.add(signatureRemoveConfirmationDialog.positiveButtonClicks()
                .subscribe(ignored -> signatureRemoveIntentSubject
                        .onNext(Intent.SignatureRemoveIntent
                                .remove(containerFile, signatureRemoveConfirmation))));
        disposables.add(signatureRemoveConfirmationDialog.cancels().subscribe(ignored ->
                signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent.clear())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        signatureAddDialog.dismiss();
        signatureRemoveConfirmationDialog.dismiss();
        documentRemoveConfirmationDialog.dismiss();
        errorDialog.setOnDismissListener(null);
        errorDialog.dismiss();
        nameUpdateDialog.dismiss();
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel -> {
            parcel.writeBundle(signatureAddDialog.onSaveInstanceState());
            parcel.writeBundle(nameUpdateDialog.onSaveInstanceState());
        });
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel -> {
            signatureAddDialog.onRestoreInstanceState(
                    parcel.readBundle(getClass().getClassLoader()));
            nameUpdateDialog.onRestoreInstanceState(parcel.readBundle(getClass().getClassLoader()));
        }));
    }
}
