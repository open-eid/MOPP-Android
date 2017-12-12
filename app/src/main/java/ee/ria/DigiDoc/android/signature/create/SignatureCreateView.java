package ee.ria.DigiDoc.android.signature.create;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.Constants.RC_SIGNATURE_CREATE_DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

public final class SignatureCreateView extends FrameLayout implements MviView<Intent, ViewState> {

    private final View progressView;

    private final Navigator navigator;
    private final SignatureCreateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Subject<Intent.CreateContainerIntent> createContainerIntentSubject =
            PublishSubject.create();

    public SignatureCreateView(@NonNull Context context) {
        this(context, null);
    }

    public SignatureCreateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureCreateView(@NonNull Context context, @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SignatureCreateView(@NonNull Context context, @Nullable AttributeSet attrs,
                               int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        inflate(context, R.layout.signature_create, this);
        progressView = findViewById(R.id.signatureCreateProgress);

        navigator = Application.component(context).navigator();
        viewModel = navigator.getViewModelProvider().get(SignatureCreateViewModel.class);
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(initialIntent(), createContainerIntent());
    }

    @Override
    public void render(ViewState state) {
        if (state.chooseFiles()) {
            navigator.getActivityResult(RC_SIGNATURE_CREATE_DOCUMENTS_ADD,
                    createGetContentIntent());
            return;
        }
        File containerFile = state.containerFile();
        Throwable error = state.error();
        progressView.setVisibility(state.createContainerInProgress() ? VISIBLE : GONE);
        if (containerFile != null) {
            navigator.replaceCurrentScreen(SignatureUpdateScreen.create(containerFile));
        } else if (error != null) {
            Toast.makeText(getContext(), R.string.signature_create_error, Toast.LENGTH_LONG)
                    .show();
            navigator.popScreen();
        }
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.CreateContainerIntent> createContainerIntent() {
        return createContainerIntentSubject;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        disposables.add(navigator.activityResults(RC_SIGNATURE_CREATE_DOCUMENTS_ADD).subscribe(
                result -> {
                    if (result.resultCode() == RESULT_OK) {
                        createContainerIntentSubject.onNext(Intent.CreateContainerIntent.create(
                                parseGetContentIntent(getContext().getContentResolver(),
                                        result.data())));
                    } else {
                        navigator.popScreen();
                    }
                }));
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
