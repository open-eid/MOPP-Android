package ee.ria.DigiDoc.android.crypto.create;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import io.reactivex.Observable;

public final class CryptoCreateScreen extends Controller implements Screen, MviView<Intent, ViewState> {

    public static CryptoCreateScreen create() {
        return new CryptoCreateScreen();
    }

    private final ViewDisposables disposables = new ViewDisposables();
    private CryptoCreateViewModel viewModel;

    private CryptoCreateAdapter adapter;
    private View activityOverlayView;
    private View activityIndicatorView;

    private ImmutableList<Certificate> recipients = ImmutableList.of();

    @SuppressWarnings("WeakerAccess")
    public CryptoCreateScreen() {}

    private Observable<Intent.RecipientsAddButtonClickIntent> recipientsAddButtonClickIntent() {
        return adapter.recipientsAddButtonClicks()
                .map(ignored -> Intent.RecipientsAddButtonClickIntent.create(getInstanceId()));
    }

    private Observable<Intent.RecipientRemoveIntent> recipientRemoveIntent() {
        return adapter.recipientRemoveClicks()
                .map(recipient -> Intent.RecipientRemoveIntent.create(recipients, recipient));
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(recipientsAddButtonClickIntent(), recipientRemoveIntent());
    }

    @Override
    public void render(ViewState state) {
        recipients = state.recipients();

        adapter.setData(state.recipients());
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : View.GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        viewModel = Application.component(context).navigator()
                .viewModel(getInstanceId(), CryptoCreateViewModel.class);
    }

    @Override
    protected void onContextUnavailable() {
        viewModel = null;
        super.onContextUnavailable();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.crypto_create_screen, container, false);
        RecyclerView listView = view.findViewById(R.id.cryptoCreateList);
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);

        listView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());

        setActivity(false);

        return view;
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    protected void onDetach(@NonNull View view) {
        disposables.detach();
        super.onDetach(view);
    }
}
