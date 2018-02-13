package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.signature.add.SignatureAddDialog;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.mid.MobileSignStatusMessageSource;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignatureStatus;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;

public final class SignatureUpdateView extends LinearLayout implements MviView<Intent, ViewState> {

    private boolean isExistingContainer;
    private File containerFile;
    private boolean signatureAddVisible;
    private boolean signatureAddSuccessMessageVisible;

    private final Toolbar toolbarView;
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

    private final ErrorDialog errorDialog;
    private final ConfirmationDialog documentRemoveConfirmationDialog;
    private final ConfirmationDialog signatureRemoveConfirmationDialog;
    private final SignatureAddDialog signatureAddDialog;

    @Nullable private DataFile documentRemoveConfirmation;
    @Nullable private Signature signatureRemoveConfirmation;

    private final MobileSignStatusMessageSource statusMessageSource;

    public SignatureUpdateView(Context context) {
        this(context, null);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SignatureUpdateView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        navigator = Application.component(context).navigator();
        viewModel = navigator.viewModel(SignatureUpdateViewModel.class);

        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update, this);
        toolbarView = findViewById(R.id.toolbar);
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
        signatureAddDialog = new SignatureAddDialog(context);
        resetSignatureAddDialog();

        statusMessageSource = new MobileSignStatusMessageSource(context.getResources());
    }

    SignatureUpdateView isExistingContainer(boolean isExistingContainer) {
        this.isExistingContainer = isExistingContainer;
        return this;
    }

    SignatureUpdateView containerFile(File containerFile) {
        this.containerFile = containerFile;
        return this;
    }

    SignatureUpdateView signatureAddVisible(boolean signatureAddVisible) {
        this.signatureAddVisible = signatureAddVisible;
        return this;
    }

    SignatureUpdateView signatureAddSuccessMessageVisible(
            boolean signatureAddSuccessMessageVisible) {
        this.signatureAddSuccessMessageVisible = signatureAddSuccessMessageVisible;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), addDocumentsIntent(), openDocumentIntent(),
                documentRemoveIntent(), signatureRemoveIntent(), signatureAddIntent(),
                sendIntent());
    }

    @Override
    public void render(ViewState state) {
        if (state.containerLoadError() != null) {
            Toast.makeText(getContext(), R.string.signature_update_load_container_error,
                    Toast.LENGTH_LONG).show();
            navigator.execute(Transaction.pop());
            return;
        }
        if (state.documentOpenFile() != null) {
            getContext().startActivity(createViewIntent(getContext(), state.documentOpenFile()));
            documentOpenIntentSubject.onNext(Intent.DocumentOpenIntent.clear());
            return;
        }

        toolbarView.setTitle(isExistingContainer
                ? R.string.signature_update_title_existing
                : R.string.signature_update_title_created);
        sendButton.setVisibility(isExistingContainer ? VISIBLE : GONE);
        buttonSpace.setVisibility(isExistingContainer ? VISIBLE : GONE);

        setActivity(state.containerLoadInProgress() || state.documentsAddInProgress()
                || state.documentOpenInProgress() || state.documentRemoveInProgress()
                || state.signatureRemoveInProgress() || state.signatureAddInProgress());

        SignedContainer container = state.container();
        String name = container == null ? null : container.name();
        ImmutableList<DataFile> documents = container == null
                ? ImmutableList.of()
                : container.dataFiles();
        ImmutableList<Signature> signatures = container == null
                ? ImmutableList.of()
                : container.signatures();
        boolean documentAddEnabled = container != null && container.dataFileAddEnabled();
        boolean documentRemoveEnabled = container != null && container.dataFileRemoveEnabled();
        boolean allSignaturesValid = true;
        for (Signature signature : signatures) {
            if (!signature.status().equals(SignatureStatus.VALID)) {
                allSignaturesValid = false;
                break;
            }
        }
        adapter.setData(state.signatureAddSuccessMessageVisible(), !allSignaturesValid, name,
                documents, signatures, documentAddEnabled, documentRemoveEnabled);

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

        if (state.signatureAddVisible()) {
            signatureAddDialog.show();
        } else {
            signatureAddDialog.dismiss();
        }
        mobileIdContainerView.setVisibility(state.signatureAddInProgress() ? VISIBLE : GONE);
        GetMobileCreateSignatureStatusResponse.ProcessStatus signatureAddStatus =
                state.signatureAddStatus();
        if (signatureAddStatus != null) {
            mobileIdStatusView.setText(statusMessageSource.getMessage(signatureAddStatus));
        } else {
            mobileIdStatusView.setText(statusMessageSource.getInitialStatusMessage());
        }
        String signatureAddChallenge = state.signatureAddChallenge();
        if (signatureAddChallenge != null) {
            mobileIdChallengeView.setText(signatureAddChallenge);
        } else {
            mobileIdChallengeView.setText(R.string.signature_add_mobile_id_challenge_placeholder);
        }

        sendButton.getCompoundDrawables()[1].setTintList(sendButton.getTextColors());
        signatureAddButton.getCompoundDrawables()[1].setTintList(
                signatureAddButton.getTextColors());
    }

    private void setActivity(boolean activity) {
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
        sendButton.setEnabled(!activity);
        signatureAddButton.setEnabled(!activity);
    }

    private void resetSignatureAddDialog() {
        signatureAddDialog.setPhoneNo(viewModel.getPhoneNo());
        signatureAddDialog.setPersonalCode(viewModel.getPersonalCode());
        signatureAddDialog.setRememberMe(true);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(isExistingContainer, containerFile,
                signatureAddVisible, signatureAddSuccessMessageVisible));
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

    private Observable<Intent.SignatureAddIntent> signatureAddIntent() {
        return clicks(signatureAddButton)
                .map(ignored ->
                        Intent.SignatureAddIntent.showIntent(isExistingContainer, containerFile))
                .mergeWith(signatureAddIntentSubject);
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
        disposables.add(signatureAddDialog.positiveButtonClicks().subscribe(data ->
                signatureAddIntentSubject.onNext(Intent.SignatureAddIntent.addIntent(
                        isExistingContainer, containerFile, data.phoneNo(), data.personalCode(),
                        data.rememberMe()))));
        disposables.add(signatureAddDialog.cancels().subscribe(ignored -> {
            resetSignatureAddDialog();
            signatureAddIntentSubject.onNext(Intent.SignatureAddIntent.clearIntent());
        }));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        signatureAddDialog.dismiss();
        signatureRemoveConfirmationDialog.dismiss();
        documentRemoveConfirmationDialog.dismiss();
        errorDialog.setOnDismissListener(null);
        errorDialog.dismiss();
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel ->
                parcel.writeBundle(signatureAddDialog.onSaveInstanceState()));
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel ->
                signatureAddDialog.onRestoreInstanceState(
                        parcel.readBundle(getClass().getClassLoader()))));
    }
}
