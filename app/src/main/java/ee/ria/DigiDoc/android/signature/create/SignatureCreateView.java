package ee.ria.DigiDoc.android.signature.create;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import io.reactivex.Observable;

public final class SignatureCreateView extends FrameLayout implements MviView<Intent, ViewState> {

    private final ViewDisposables disposables = new ViewDisposables();
    private final SignatureCreateViewModel viewModel;

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
        viewModel = Application.component(context).navigator()
                .viewModel(SignatureCreateViewModel.class);
        inflate(context, R.layout.signature_create, this);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent());
    }

    @Override
    public void render(ViewState state) {
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
