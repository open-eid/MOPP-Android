package ee.ria.DigiDoc.android.crypto.create;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding2.support.v7.widget.SearchViewQueryTextEvent;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.Formatter;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.support.v7.widget.RxSearchView.queryTextChangeEvents;
import static ee.ria.DigiDoc.android.utils.InputMethodUtils.hideSoftKeyboard;

public final class CryptoRecipientsScreen extends Controller implements Screen,
        MviView<Intent, ViewState> {

    private static final String KEY_CRYPTO_CREATE_SCREEN_ID = "cryptoCreateScreenId";

    public static CryptoRecipientsScreen create(String cryptoCreateScreenId) {
        Bundle args = new Bundle();
        args.putString(KEY_CRYPTO_CREATE_SCREEN_ID, cryptoCreateScreenId);
        return new CryptoRecipientsScreen(args);
    }

    private final String cryptoCreateScreenId;

    private final ViewDisposables disposables = new ViewDisposables();
    private CryptoCreateViewModel viewModel;
    private Formatter formatter;

    private SearchView searchView;
    private CryptoRecipientsAdapter adapter;
    private View activityOverlayView;
    private View activityIndicatorView;

    private ImmutableList<Certificate> recipients = ImmutableList.of();

    @SuppressWarnings("WeakerAccess")
    public CryptoRecipientsScreen(Bundle args) {
        super(args);
        cryptoCreateScreenId = args.getString(KEY_CRYPTO_CREATE_SCREEN_ID);
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : View.GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : View.GONE);
    }

    private Observable<Intent.RecipientsSearchIntent> recipientsSearchIntent() {
        return queryTextChangeEvents(searchView)
                .filter(SearchViewQueryTextEvent::isSubmitted)
                .doOnNext(ignored -> hideSoftKeyboard(searchView))
                .map(event -> Intent.RecipientsSearchIntent.create(event.queryText().toString()));
    }

    private Observable<Intent.RecipientAddIntent> recipientAddIntent() {
        return adapter.addButtonClicks()
                .map(recipient -> Intent.RecipientAddIntent.create(recipients, recipient));
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(recipientsSearchIntent(), recipientAddIntent());
    }

    @Override
    public void render(ViewState state) {
        recipients = state.recipients();

        setActivity(state.recipientsSearchState().equals(State.ACTIVE));
        adapter.setData(state.recipients(), state.recipientsSearchResult());
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        viewModel = Application.component(context).navigator()
                .viewModel(cryptoCreateScreenId, CryptoCreateViewModel.class);
        formatter = Application.component(context).formatter();
    }

    @Override
    protected void onContextUnavailable() {
        viewModel = null;
        super.onContextUnavailable();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.crypto_recipients_screen, container, false);
        searchView = view.findViewById(R.id.cryptoRecipientsSearch);
        searchView.setSubmitButtonEnabled(true);
        RecyclerView listView = view.findViewById(R.id.cryptoRecipientsList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(adapter = new CryptoRecipientsAdapter(formatter));
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);
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
