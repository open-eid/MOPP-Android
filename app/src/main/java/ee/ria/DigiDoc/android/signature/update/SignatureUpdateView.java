package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.list.DocumentListContainerView;
import ee.ria.DigiDoc.android.signature.data.SignatureContainer;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class SignatureUpdateView extends CoordinatorLayout implements
        MviView<Intent, ViewState> {

    private File containerFile;

    private final View appBarView;
    private final Toolbar toolbarView;
    private final View contentView;
    private final DocumentListContainerView documentsView;
    private final View loadContainerProgressView;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

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

        navigator = Application.component(context).navigator();
        viewModel = navigator.getViewModelProvider().get(SignatureUpdateViewModel.class);
    }

    public SignatureUpdateView containerFile(File containerFile) {
        this.containerFile = containerFile;
        return this;
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(initialIntent(), Observable.empty());
    }

    @Override
    public void render(ViewState state) {
        appBarView.setVisibility(state.loadContainerInProgress() ? GONE : VISIBLE);
        contentView.setVisibility(state.loadContainerInProgress() ? GONE : VISIBLE);
        loadContainerProgressView.setVisibility(state.loadContainerInProgress() ? VISIBLE : GONE);

        SignatureContainer container = state.container();
        if (container != null) {
            toolbarView.setTitle(container.name());
            documentsView.setDocuments(container.documents());
        }
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(containerFile));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        disposables.add(navigationClicks(toolbarView).subscribe(o -> navigator.popScreen()));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
