package ee.ria.DigiDoc.android.root;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.settings.create.Intent;
import ee.ria.DigiDoc.android.main.settings.create.ViewState;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import io.reactivex.rxjava3.core.Observable;

@SuppressLint("ViewConstructor")
public final class RootCreateView extends FrameLayout implements ContentView, MviView<Intent, ViewState> {

    private final ViewDisposables disposables = new ViewDisposables();

    public RootCreateView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.root_screen, this);
    }

    @Override
    public Observable<Intent> intents() {
        return null;
    }

    @Override
    public void render(ViewState state) {}

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
    }

    @Override
    protected void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
