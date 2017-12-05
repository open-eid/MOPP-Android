package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.list.DocumentListContainerView;
import ee.ria.DigiDoc.android.document.list.DocumentListScreen;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.app.Activity.RESULT_OK;
import static com.jakewharton.rxbinding2.support.design.widget.RxSnackbar.dismisses;
import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_UPDATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

public final class SignatureUpdateView extends CoordinatorLayout implements
        MviView<Intent, ViewState> {

    private File containerFile;

    private final View appBarView;
    private final Toolbar toolbarView;
    private final View contentView;
    private final DocumentListContainerView documentsView;
    private final View loadContainerProgressView;
    private final Snackbar addDocumentsErrorSnackbar;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.AddDocumentsIntent> addDocumentsIntentSubject =
            PublishSubject.create();

    public SignatureUpdateView(Context context) {
        this(context, null);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureUpdateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_update, this);
        appBarView = findViewById(R.id.appBar);
        toolbarView = findViewById(R.id.toolbar);
        contentView = findViewById(R.id.signatureUpdateContent);
        documentsView = findViewById(R.id.signatureUpdateDocuments);
        loadContainerProgressView = findViewById(R.id.signatureUpdateLoadContainerProgress);
        addDocumentsErrorSnackbar = Snackbar.make(this,
                R.string.signature_update_add_documents_error_exists, Snackbar.LENGTH_LONG);

        navigator = Application.component(context).navigator();
        viewModel = navigator.getViewModelProvider().get(SignatureUpdateViewModel.class);
    }

    public SignatureUpdateView containerFile(File containerFile) {
        this.containerFile = containerFile;
        return this;
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(initialIntent(), addDocumentsIntent());
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

        appBarView.setVisibility(state.loadContainerInProgress() ? GONE : VISIBLE);
        contentView.setVisibility(state.loadContainerInProgress() ? GONE : VISIBLE);
        loadContainerProgressView.setVisibility(state.loadContainerInProgress() ? VISIBLE : GONE);

        documentsView.setProgress(state.addingDocuments());

        SignatureContainer container = state.container();
        if (container != null) {
            toolbarView.setTitle(container.name());
            documentsView.setAddButtonVisible(!container.documentsLocked());
            documentsView.setDocuments(container.documents());
        }

        if (state.addDocumentsError() == null) {
            addDocumentsErrorSnackbar.dismiss();
        } else {
            addDocumentsErrorSnackbar.show();
        }
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(containerFile));
    }

    private Observable<Intent.AddDocumentsIntent> addDocumentsIntent() {
        return documentsView.addButtonClicks()
                .map(ignored -> Intent.AddDocumentsIntent.pick(containerFile))
                .mergeWith(addDocumentsIntentSubject);
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
        disposables.add(documentsView.expandButtonClicks().subscribe(ignored ->
                navigator.pushScreen(DocumentListScreen.create(documentsView.getDocuments()))));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
