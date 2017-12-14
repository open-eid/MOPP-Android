package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Locale;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.document.list.DocumentsAdapter;
import ee.ria.DigiDoc.android.signature.data.Signature;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.res.ResourcesCompat.getColor;
import static com.jakewharton.rxbinding2.support.design.widget.RxSnackbar.dismisses;
import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_UPDATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.Immutables.with;
import static ee.ria.DigiDoc.android.utils.Immutables.without;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

public final class SignatureUpdateView extends CoordinatorLayout implements
        MviView<Intent, ViewState> {

    private File containerFile;

    private final Resources resources;

    private final ColorStateList successTint;
    private final ColorStateList errorTint;

    private final Toolbar toolbarView;
    private final View activityIndicatorView;
    private final View activityOverlayView;
    private final DocumentsAdapter documentsAdapter;
    private final View documentsAddButton;
    private final Snackbar addDocumentsErrorSnackbar;
    private final Snackbar removeDocumentsErrorSnackbar;
    private final ActionMode.Callback documentsSelectionActionModeCallback =
            new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.signature_update_documents_action_mode, menu);
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(String.format(Locale.US, "%d",
                    selectedDocuments == null ? 0 : selectedDocuments.size()));
            return true;
        }
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.signatureUpdateDocumentsRemoveButton) {
                removeDocumentsIntentSubject.onNext(Intent.RemoveDocumentsIntent
                        .create(containerFile, selectedDocuments));
                return true;
            }
            return false;
        }
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            documentsSelectionIntentSubject.onNext(Intent.DocumentsSelectionIntent.clear());
        }
    };
    @Nullable private ActionMode documentsSelectionActionMode;
    private final View signatureSummaryView;
    private final ImageView signatureSummaryValidityView;
    private final TextView signatureSummaryPrimaryTextView;
    private final TextView signatureSummarySecondaryTextView;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.AddDocumentsIntent> addDocumentsIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.OpenDocumentIntent> openDocumentIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.DocumentsSelectionIntent> documentsSelectionIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.RemoveDocumentsIntent> removeDocumentsIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureListVisibilityIntent>
            signatureListVisibilityIntentSubject = PublishSubject.create();
    private final BottomSheetDialog signatureListDialog;
    private final RecyclerView signatureListView;
    private final SignatureAdapter signatureAdapter;

    private boolean documentsLocked = true;
    @Nullable private ImmutableSet<Document> selectedDocuments;
    private int signatureCount = 0;

    public SignatureUpdateView(Context context) {
        this(context, null);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_update, this);
        resources = context.getResources();
        successTint = ColorStateList.valueOf(getColor(resources, R.color.success, null));
        errorTint = ColorStateList.valueOf(getColor(resources, R.color.error, null));
        toolbarView = findViewById(R.id.toolbar);
        activityIndicatorView = findViewById(R.id.activityIndicator);
        activityOverlayView = findViewById(R.id.activityOverlay);
        RecyclerView documentsView = findViewById(R.id.signatureUpdateDocuments);
        documentsAddButton = findViewById(R.id.signatureUpdateDocumentsAddButton);
        addDocumentsErrorSnackbar = Snackbar.make(this,
                R.string.signature_update_add_documents_error_exists, Snackbar.LENGTH_LONG);
        removeDocumentsErrorSnackbar = Snackbar.make(this,
                R.string.signature_update_documents_remove_error_container_empty,
                BaseTransientBottomBar.LENGTH_LONG);
        signatureSummaryView = findViewById(R.id.signatureUpdateSignatureSummary);
        signatureSummaryValidityView = findViewById(R.id.signatureUpdateSignatureSummaryValidity);
        signatureSummaryPrimaryTextView = findViewById(
                R.id.signatureUpdateSignatureSummaryPrimaryText);
        signatureSummarySecondaryTextView = findViewById(
                R.id.signatureUpdateSignatureSummarySecondaryText);

        signatureListDialog = new BottomSheetDialog(context);
        Context signatureListContext = signatureListDialog.getContext();
        signatureListView = new RecyclerView(signatureListContext);
        signatureListView.setLayoutManager(new LinearLayoutManager(signatureListContext));
        signatureListView.setAdapter(signatureAdapter = new SignatureAdapter(signatureListContext));
        signatureListDialog.setContentView(signatureListView);

        documentsView.setLayoutManager(new LinearLayoutManager(context));
        documentsView.setAdapter(documentsAdapter = new DocumentsAdapter());

        navigator = Application.component(context).navigator();
        viewModel = navigator.getViewModelProvider().get(SignatureUpdateViewModel.class);
    }

    public SignatureUpdateView containerFile(File containerFile) {
        this.containerFile = containerFile;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), addDocumentsIntent(), openDocumentIntent(),
                documentsSelectionIntent(), removeDocumentsIntent(),
                signatureListVisibilityIntent(), signatureRemoveSelectionIntent(),
                signatureRemoveIntent());
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

        setActivity(state.loadContainerInProgress() || state.documentsProgress());

        SignatureContainer container = state.container();
        documentsLocked = container == null || container.documentsLocked();
        String name = container == null ? null : container.name();
        ImmutableList<Document> documents = container == null
                ? ImmutableList.of()
                : container.documents();
        ImmutableList<Signature> signatures = container == null
                ? ImmutableList.of()
                : container.signatures();
        signatureCount = signatures.size();
        int invalidSignatureCount = container == null ? 0 : container.invalidSignatureCount();

        toolbarView.setTitle(name);
        documentsAdapter.setDocuments(documents, selectedDocuments = state.selectedDocuments());
        documentsAddButton.setVisibility(documentsLocked || selectedDocuments != null
                ? GONE : VISIBLE);

        if (state.selectedDocuments() == null && documentsSelectionActionMode != null) {
            documentsSelectionActionMode.finish();
            documentsSelectionActionMode = null;
        } else if (!documentsLocked && state.selectedDocuments() != null
                && documentsSelectionActionMode == null) {
            documentsSelectionActionMode = navigator
                    .startActionMode(documentsSelectionActionModeCallback);
        }
        if (documentsSelectionActionMode != null) {
            documentsSelectionActionMode.invalidate();
        }

        if (state.addDocumentsError() == null) {
            addDocumentsErrorSnackbar.dismiss();
        } else {
            addDocumentsErrorSnackbar.show();
        }
        if (state.removeDocumentsError() == null) {
            removeDocumentsErrorSnackbar.dismiss();
        } else {
            removeDocumentsErrorSnackbar.show();
        }

        if (state.signatureListVisible() && !signatureListDialog.isShowing()) {
            signatureListView.scrollToPosition(0);
            signatureListDialog.show();
        } else if (!state.signatureListVisible()) {
            signatureListDialog.dismiss();
        }

        signatureSummaryValidityView.setVisibility(signatureCount > 0 ? VISIBLE : GONE);
        signatureSummaryPrimaryTextView.setVisibility(signatureCount > 0 ? VISIBLE : GONE);
        if (signatureCount == 0) {
            signatureSummarySecondaryTextView.setText(
                    R.string.signature_update_signature_summary_secondary_empty);
        } else {
            signatureSummaryPrimaryTextView.setText(resources.getString(
                    R.string.signature_update_signature_summary_primary, signatureCount));
            if (invalidSignatureCount == 0) {
                signatureSummaryValidityView.setImageResource(R.drawable.ic_check_circle);
                signatureSummaryValidityView.setImageTintList(successTint);
                signatureSummarySecondaryTextView.setText(
                        R.string.signature_update_signature_summary_secondary_valid);
            } else {
                signatureSummaryValidityView.setImageResource(R.drawable.ic_error);
                signatureSummaryValidityView.setImageTintList(errorTint);
                signatureSummarySecondaryTextView.setText(resources.getString(
                        R.string.signature_update_signature_summary_secondary_invalid,
                        invalidSignatureCount));
            }
        }
        signatureAdapter.setSignatures(signatures);
        signatureAdapter.setRemoveSelection(state.signatureRemoveSelection());
    }

    protected void setActivity(boolean activity) {
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(containerFile));
    }

    private Observable<Intent.AddDocumentsIntent> addDocumentsIntent() {
        return clicks(documentsAddButton)
                .map(ignored -> Intent.AddDocumentsIntent.pick(containerFile))
                .mergeWith(addDocumentsIntentSubject);
    }

    private Observable<Intent.OpenDocumentIntent> openDocumentIntent() {
        return openDocumentIntentSubject;
    }

    private Observable<Intent.DocumentsSelectionIntent> documentsSelectionIntent() {
        return documentsSelectionIntentSubject.mergeWith(documentsAdapter.itemLongClicks()
                .filter(ignored -> !documentsLocked && selectedDocuments == null)
                .map(document ->
                        Intent.DocumentsSelectionIntent.create(ImmutableSet.of(document))));
    }

    private Observable<Intent.RemoveDocumentsIntent> removeDocumentsIntent() {
        return removeDocumentsIntentSubject;
    }

    private Observable<Intent.SignatureListVisibilityIntent> signatureListVisibilityIntent() {
        return clicks(signatureSummaryView)
                .filter(ignored -> signatureCount > 0)
                .map(o -> Intent.SignatureListVisibilityIntent.create(true))
                .mergeWith(signatureListVisibilityIntentSubject);
    }

    private Observable<Intent.SignatureRemoveSelectionIntent> signatureRemoveSelectionIntent() {
        //noinspection Guava
        return signatureAdapter.removeSelections()
                .map(signature -> Intent.SignatureRemoveSelectionIntent.create(signature.orNull()));
    }

    private Observable<Intent.SignatureRemoveIntent> signatureRemoveIntent() {
        return signatureAdapter.removes()
                .map(signature -> Intent.SignatureRemoveIntent.create(containerFile, signature));
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
        disposables.add(dismisses(removeDocumentsErrorSnackbar).subscribe(ignored ->
                removeDocumentsIntentSubject.onNext(Intent.RemoveDocumentsIntent.clear())));
        disposables.add(documentsAdapter.itemClicks().subscribe(document -> {
            if (selectedDocuments == null) {
                openDocumentIntentSubject.onNext(Intent.OpenDocumentIntent
                        .open(containerFile, document));
            } else {
                Intent.DocumentsSelectionIntent intent = Intent.DocumentsSelectionIntent.create(
                        selectedDocuments.contains(document)
                                ? without(selectedDocuments, document)
                                : with(selectedDocuments, document));
                documentsSelectionIntentSubject.onNext(intent);
            }
        }));
        signatureListDialog.setOnDismissListener(ignored ->
                signatureListVisibilityIntentSubject
                        .onNext(Intent.SignatureListVisibilityIntent.create(false)));
    }

    @Override
    public void onDetachedFromWindow() {
        signatureListDialog.setOnDismissListener(null);
        signatureListDialog.dismiss();
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
