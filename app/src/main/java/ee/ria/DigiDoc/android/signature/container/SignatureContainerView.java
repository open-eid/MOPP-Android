package ee.ria.DigiDoc.android.signature.container;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.document.list.DocumentListContainerView;
import ee.ria.DigiDoc.android.utils.IntentUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_CONTAINER_DOCUMENT_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

public final class SignatureContainerView extends CoordinatorLayout implements
        MviView<Intent, ViewState> {

    private final Toolbar toolbarView;
    private final DocumentListContainerView documentsView;

    private final Navigator navigator;
    private final SignatureContainerViewModel viewModel;

    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.ChooseDocumentsIntent> chooseDocumentsSubject =
            PublishSubject.create();
    private final Subject<Intent.AddDocumentsIntent> addDocumentsIntentSubject =
            PublishSubject.create();

    public SignatureContainerView(Context context) {
        this(context, null);
    }

    public SignatureContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureContainerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.signature_container, this);
        toolbarView = findViewById(R.id.toolbar);
        documentsView = findViewById(R.id.signatureContainerDocuments);

        Application.ApplicationComponent applicationComponent = Application.component(context);
        navigator = applicationComponent.navigator();
        viewModel = navigator.getViewModelProvider().get(SignatureContainerViewModel.class);
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(initialIntent(), chooseDocumentsIntent(), addDocumentsIntent());
    }

    @Override
    public void render(ViewState state) {
        Timber.d("render: %s", state);

        if (state.chooseDocuments()) {
            navigator.getActivityResult(RC_SIGNATURE_CONTAINER_DOCUMENT_ADD,
                    createGetContentIntent());
            return;
        }
        documentsView.setProgress(state.addingDocuments());
        if (state.documents() != null) {
            documentsView.setDocuments(state.documents());
        }
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.ChooseDocumentsIntent> chooseDocumentsIntent() {
        return documentsView.addButtonClicks()
                .map(ignored -> Intent.ChooseDocumentsIntent.create())
                .mergeWith(chooseDocumentsSubject);
    }

    private Observable<Intent.AddDocumentsIntent> addDocumentsIntent() {
        return addDocumentsIntentSubject;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigator.activityResults(RC_SIGNATURE_CONTAINER_DOCUMENT_ADD).subscribe(
                result -> {
                    if (result.resultCode() == RESULT_OK) {
                        ImmutableList<IntentUtils.FileStream> fileStreams = parseGetContentIntent(
                                getContext().getContentResolver(), result.data());
                        addDocumentsIntentSubject.onNext(Intent.AddDocumentsIntent
                                .create(fileStreams));
                    } else if (documentsView.isEmpty()) {
                        navigator.popScreen();
                    }
                }));
        disposables.add(viewModel.states().subscribe(this::render));
        viewModel.process(intents());
        disposables.add(navigationClicks(toolbarView).subscribe(o -> navigator.popScreen()));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
