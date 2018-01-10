package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.signature.add.SignatureAddDialog;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.mid.MobileSignFaultMessageSource;
import ee.ria.DigiDoc.mid.MobileSignStatusMessageSource;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.app.Activity.RESULT_OK;
import static com.jakewharton.rxbinding2.support.design.widget.RxSnackbar.dismisses;
import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_UPDATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

public final class SignatureUpdateView extends CoordinatorLayout implements
        MviView<Intent, ViewState> {

    private File containerFile;

    private final Toolbar toolbarView;
    private final TextView errorView;
    private final TextView signaturesValidityView;
    private final SignatureUpdateAdapter adapter;
    private final View activityIndicatorView;
    private final View activityOverlayView;
    private final View mobileIdContainerView;
    private final TextView mobileIdStatusView;
    private final TextView mobileIdChallengeView;
    private final Snackbar addDocumentsErrorSnackbar;
    private final Snackbar removeDocumentsErrorSnackbar;
    private final Snackbar signatureAddSuccessSnackbar;
    private final Snackbar signatureRemoveErrorSnackbar;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.AddDocumentsIntent> addDocumentsIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.OpenDocumentIntent> openDocumentIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.DocumentRemoveIntent> documentRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureRemoveIntent> signatureRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureAddIntent> signatureAddIntentSubject =
            PublishSubject.create();

    private final ConfirmationDialog documentRemoveConfirmationDialog;
    private final ConfirmationDialog signatureRemoveConfirmationDialog;
    private final SignatureAddDialog signatureAddDialog;

    @Nullable private Document documentRemoveConfirmation;
    @Nullable private Signature signatureRemoveConfirmation;

    private final MobileSignStatusMessageSource statusMessageSource;
    private final MobileSignFaultMessageSource faultMessageSource;

    public SignatureUpdateView(Context context) {
        this(context, null);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        navigator = Application.component(context).navigator();
        viewModel = navigator.getViewModelProvider().get(SignatureUpdateViewModel.class);

        inflate(context, R.layout.signature_update, this);
        toolbarView = findViewById(R.id.toolbar);
        errorView = findViewById(R.id.signatureUpdateError);
        signaturesValidityView = findViewById(R.id.signatureUpdateSignaturesValidity);
        RecyclerView listView = findViewById(R.id.signatureUpdateList);
        activityIndicatorView = findViewById(R.id.activityIndicator);
        activityOverlayView = findViewById(R.id.activityOverlay);
        mobileIdContainerView = findViewById(R.id.signatureUpdateMobileIdContainer);
        mobileIdStatusView = findViewById(R.id.signatureUpdateMobileIdStatus);
        mobileIdChallengeView = findViewById(R.id.signatureUpdateMobileIdChallenge);
        addDocumentsErrorSnackbar = Snackbar.make(this,
                R.string.signature_update_add_documents_error_exists, Snackbar.LENGTH_LONG);
        removeDocumentsErrorSnackbar = Snackbar.make(this,
                R.string.signature_update_documents_remove_error_container_empty,
                BaseTransientBottomBar.LENGTH_LONG);
        signatureAddSuccessSnackbar = Snackbar.make(this, R.string.signature_added,
                Snackbar.LENGTH_LONG);
        signatureRemoveErrorSnackbar = Snackbar.make(this,
                R.string.signature_update_signature_remove_error, Snackbar.LENGTH_LONG);

        // android:drawableTint is supported API level 23+
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.colorError});
        int errorColor = a.getColor(0, Color.RED);
        a.recycle();
        Drawable errorDrawable = context.getDrawable(R.drawable.ic_error);
        if (errorDrawable != null) {
            errorDrawable.setTint(errorColor);
            errorView.setCompoundDrawablesWithIntrinsicBounds(errorDrawable, null, null, null);
            signaturesValidityView.setCompoundDrawablesRelativeWithIntrinsicBounds(errorDrawable,
                    null, null, null);
        }

        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(adapter = new SignatureUpdateAdapter());

        documentRemoveConfirmationDialog = new ConfirmationDialog(context,
                R.string.signature_update_document_remove_confirmation_message);
        signatureRemoveConfirmationDialog = new ConfirmationDialog(context,
                R.string.signature_update_signature_remove_confirmation_message);
        signatureAddDialog = new SignatureAddDialog(context, viewModel.getPhoneNo(),
                viewModel.getPersonalCode());

        statusMessageSource = new MobileSignStatusMessageSource(context.getResources());
        faultMessageSource = new MobileSignFaultMessageSource(context.getResources());
    }

    public SignatureUpdateView containerFile(File containerFile) {
        this.containerFile = containerFile;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), addDocumentsIntent(), openDocumentIntent(),
                documentRemoveIntent(), signatureRemoveIntent(), signatureAddIntent());
    }

    @Override
    public void render(ViewState state) {
        if (state.loadContainerError() != null) {
            Toast.makeText(getContext(), R.string.signature_update_load_container_error,
                    Toast.LENGTH_LONG).show();
            navigator.popScreen();
            return;
        }
        if (state.pickingDocuments()) {
            navigator.getActivityResult(RC_SIGNATURE_UPDATE_DOCUMENTS_ADD,
                    createGetContentIntent());
            return;
        }
        if (state.openedDocumentFile() != null) {
            getContext().startActivity(createViewIntent(getContext(), state.openedDocumentFile()));
            openDocumentIntentSubject.onNext(Intent.OpenDocumentIntent.clear());
            return;
        }

        setActivity(state.loadContainerInProgress() || state.documentsProgress()
                || state.documentRemoveInProgress() || state.signatureRemoveInProgress()
                || state.signatureAddInProgress());

        SignatureContainer container = state.container();
        String name = container == null ? null : container.name();
        ImmutableList<Document> documents = container == null
                ? ImmutableList.of()
                : container.documents();
        ImmutableList<Signature> signatures = container == null
                ? ImmutableList.of()
                : container.signatures();
        boolean documentAddEnabled = container != null && container.documentAddEnabled();
        boolean documentRemoveEnabled = container != null && container.documentRemoveEnabled();
        adapter.setData(documents, signatures, documentAddEnabled, documentRemoveEnabled);

        boolean allSignaturesValid = true;
        for (Signature signature : signatures) {
            if (!signature.valid()) {
                allSignaturesValid = false;
                break;
            }
        }
        signaturesValidityView.setVisibility(allSignaturesValid ? GONE : VISIBLE);

        toolbarView.setTitle(name);

        if (state.addDocumentsError() == null) {
            addDocumentsErrorSnackbar.dismiss();
        } else {
            addDocumentsErrorSnackbar.show();
        }

        documentRemoveConfirmation = state.documentRemoveConfirmation();
        if (documentRemoveConfirmation != null) {
            documentRemoveConfirmationDialog.show();
        } else {
            documentRemoveConfirmationDialog.dismiss();
        }
        if (state.documentRemoveError() == null) {
            removeDocumentsErrorSnackbar.dismiss();
        } else {
            removeDocumentsErrorSnackbar.show();
        }

        signatureRemoveConfirmation = state.signatureRemoveConfirmation();
        if (signatureRemoveConfirmation != null) {
            signatureRemoveConfirmationDialog.show();
        } else {
            signatureRemoveConfirmationDialog.dismiss();
        }
        if (state.signatureRemoveError() != null) {
            signatureRemoveErrorSnackbar.show();
        } else {
            signatureRemoveErrorSnackbar.dismiss();
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
        if (state.signatureAddSuccessMessageVisible()) {
            signatureAddSuccessSnackbar.show();
        } else {
            signatureAddSuccessSnackbar.dismiss();
        }
        Throwable signatureAddError = state.signatureAddError();
        if (signatureAddError instanceof Processor.SignatureAlreadyExistsException) {
            errorView.setText(R.string.already_signed_by_person);
        } else if (signatureAddError instanceof Processor.MobileIdFaultReasonMessageException) {
            errorView.setText(faultMessageSource.getMessage(
                    ((Processor.MobileIdFaultReasonMessageException) signatureAddError).reason));
        } else if (signatureAddError instanceof Processor.MobileIdMessageException) {
            errorView.setText(statusMessageSource.getMessage(
                    ((Processor.MobileIdMessageException) signatureAddError).processStatus));
        }
        errorView.setVisibility(signatureAddError == null ? GONE : VISIBLE);
    }

    protected void setActivity(boolean activity) {
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(containerFile));
    }

    private Observable<Intent.AddDocumentsIntent> addDocumentsIntent() {
        return adapter.documentAddClicks()
                .map(ignored -> Intent.AddDocumentsIntent.pick(containerFile))
                .mergeWith(addDocumentsIntentSubject);
    }

    private Observable<Intent.OpenDocumentIntent> openDocumentIntent() {
        return openDocumentIntentSubject;
    }

    private Observable<Intent.DocumentRemoveIntent> documentRemoveIntent() {
        return documentRemoveIntentSubject;
    }

    private Observable<Intent.SignatureRemoveIntent> signatureRemoveIntent() {
        return signatureRemoveIntentSubject;
    }

    private Observable<Intent.SignatureAddIntent> signatureAddIntent() {
        return adapter.signatureAddClicks()
                .map(ignored -> Intent.SignatureAddIntent.showIntent(containerFile))
                .mergeWith(signatureAddIntentSubject);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        disposables.add(navigationClicks(toolbarView).subscribe(o -> navigator.popScreen()));
        disposables.add(navigator.activityResults(RC_SIGNATURE_UPDATE_DOCUMENTS_ADD).subscribe(
                result -> {
                    if (result.resultCode() == RESULT_OK) {
                        addDocumentsIntentSubject.onNext(Intent.AddDocumentsIntent.add(
                                containerFile, parseGetContentIntent(
                                        getContext().getContentResolver(), result.data())));
                    } else {
                        addDocumentsIntentSubject.onNext(Intent.AddDocumentsIntent.clear());
                    }
                }));
        disposables.add(dismisses(addDocumentsErrorSnackbar).subscribe(ignored ->
                addDocumentsIntentSubject.onNext(Intent.AddDocumentsIntent.clear())));
        disposables.add(adapter.documentRemoveClicks().subscribe(document ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent
                        .showConfirmation(containerFile, document))));
        disposables.add(documentRemoveConfirmationDialog.positiveButtonClicks().subscribe(ignored ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent
                        .remove(containerFile, documentRemoveConfirmation))));
        disposables.add(documentRemoveConfirmationDialog.cancels().subscribe(ignored ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent.clear())));
        disposables.add(dismisses(removeDocumentsErrorSnackbar).subscribe(ignored ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent.clear())));
        disposables.add(adapter.documentClicks().subscribe(document ->
                openDocumentIntentSubject.onNext(Intent.OpenDocumentIntent
                        .open(containerFile, document))));
        disposables.add(signatureAddDialog.positiveButtonClicks().subscribe(data ->
                signatureAddIntentSubject.onNext(Intent.SignatureAddIntent.addIntent(containerFile,
                        data.phoneNo(), data.personalCode(), data.rememberMe()))));
        disposables.add(adapter.signatureRemoveClicks().subscribe(signature ->
                signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent
                        .showConfirmation(containerFile, signature))));
        disposables.add(signatureRemoveConfirmationDialog.positiveButtonClicks()
                .subscribe(ignored -> signatureRemoveIntentSubject
                        .onNext(Intent.SignatureRemoveIntent
                                .remove(containerFile, signatureRemoveConfirmation))));
        disposables.add(signatureRemoveConfirmationDialog.cancels().subscribe(ignored ->
                signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent.clear())));
        disposables.add(dismisses(signatureRemoveErrorSnackbar).subscribe(ignored ->
                signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent.clear())));
        disposables.add(signatureAddDialog.cancels().subscribe(ignored ->
                signatureAddIntentSubject.onNext(Intent.SignatureAddIntent.clearIntent())));
        disposables.add(dismisses(signatureAddSuccessSnackbar).subscribe(ignored ->
                signatureAddIntentSubject.onNext(Intent.SignatureAddIntent.clearIntent())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        signatureAddDialog.dismiss();
        signatureRemoveConfirmationDialog.dismiss();
        documentRemoveConfirmationDialog.dismiss();
        super.onDetachedFromWindow();
    }
}
