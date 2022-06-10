package ee.ria.DigiDoc.android.crypto.create;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxSearchView.queryTextChangeEvents;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.Constants.VOID;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding4.widget.SearchViewQueryTextEvent;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.common.Certificate;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class CryptoRecipientsScreen extends Controller implements Screen,
        MviView<Intent, ViewState>, Navigator.BackButtonClickListener {

    private static final String KEY_CRYPTO_CREATE_SCREEN_ID = "cryptoCreateScreenId";

    public static CryptoRecipientsScreen create(String cryptoCreateScreenId) {
        Bundle args = new Bundle();
        args.putString(KEY_CRYPTO_CREATE_SCREEN_ID, cryptoCreateScreenId);
        return new CryptoRecipientsScreen(args);
    }

    private final String cryptoCreateScreenId;

    private final Subject<Object> backButtonClicksSubject = PublishSubject.create();

    private final ViewDisposables disposables = new ViewDisposables();
    private CryptoCreateViewModel viewModel;
    private Navigator navigator;

    private Toolbar toolbarView;
    private SearchView searchView;
    private EditText searchViewInnerText;
    private CryptoCreateAdapter adapter;
    private View doneButton;
    private View activityOverlayView;
    private View activityIndicatorView;

    private ImmutableList<Certificate> recipients = ImmutableList.of();

    @SuppressWarnings("WeakerAccess")
    public CryptoRecipientsScreen(Bundle args) {
        super(args);
        cryptoCreateScreenId = args.getString(KEY_CRYPTO_CREATE_SCREEN_ID);
    }

    private Observable<Intent.RecipientsScreenUpButtonClickIntent>
            recipientsScreenUpButtonClickIntent() {
        return Observable.merge(
                navigationClicks(toolbarView)
                        .map(ignored -> Intent.RecipientsScreenUpButtonClickIntent.create()),
                clicks(doneButton)
                        .map(ignored -> Intent.RecipientsScreenUpButtonClickIntent.create()));
    }

    private Observable<Intent.RecipientsSearchIntent> recipientsSearchIntent() {
        return Observable.merge(
                queryTextChangeEvents(searchView)
                        .filter(SearchViewQueryTextEvent::isSubmitted)
                        .doOnNext(ignored -> searchView.clearFocus())
                        .map(event ->
                                Intent.RecipientsSearchIntent.search(event.getQueryText().toString())),
                backButtonClicksSubject.map(ignored -> Intent.RecipientsSearchIntent.clear()));
    }

    private Observable<Intent.RecipientAddIntent> recipientAddIntent() {
        return adapter.recipientAddClicks()
                .map(recipient -> Intent.RecipientAddIntent.create(recipients, recipient));
    }

    private Observable<Intent.RecipientRemoveIntent> recipientRemoveIntent() {
        return adapter.recipientRemoveClicks()
                .map(recipient -> Intent.RecipientRemoveIntent.create(recipients, recipient));
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.merge(recipientsScreenUpButtonClickIntent(), recipientsSearchIntent(),
                recipientAddIntent(), recipientRemoveIntent());
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : View.GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : View.GONE);
    }

    @Override
    public void render(ViewState state) {
        recipients = state.recipients();
        setActivity(state.recipientsSearchState().equals(State.ACTIVE));
        adapter.dataForRecipients(state.recipientsSearchState(), state.recipientsSearchResult(), state.recipientsSearchError(),
                recipients);
    }

    @Override
    public boolean onBackButtonClick() {
        backButtonClicksSubject.onNext(VOID);
        if (getApplicationContext() != null) {
            AccessibilityUtils.sendAccessibilityEvent(getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.recipient_addition_cancelled);
        }
        return false;
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        viewModel = Application.component(context).navigator()
                .viewModel(cryptoCreateScreenId, CryptoCreateViewModel.class);
        navigator = Application.component(context).navigator();
    }

    @Override
    protected void onContextUnavailable() {
        viewModel = null;
        super.onContextUnavailable();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        View view = inflater.inflate(R.layout.crypto_recipients_screen, container, false);

        AccessibilityUtils.setAccessibilityPaneTitle(view, R.string.crypto_recipients_title);

        toolbarView = view.findViewById(R.id.toolbar);
        toolbarView.setTitle(R.string.crypto_recipients_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        searchView = view.findViewById(R.id.cryptoRecipientsSearch);
        if (getResources() != null) {
            searchView.setQueryHint(getResources().getString(R.string.crypto_recipients_search));
            searchView.setIconifiedByDefault(false);
            searchViewInnerText = searchView.findViewById(getResources().getIdentifier("android:id/search_src_text", null, null));
            searchViewInnerText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    searchViewInnerText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (getResources() != null) {
                        if (searchViewInnerText.getTextSize() > 40) {
                            searchViewInnerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40);
                        }
                        searchViewInnerText.setLayoutParams(
                                new LinearLayout.LayoutParams((int) (searchView.getWidth() / 1.5), DisplayUtil.getDisplayMetricsDpToInt(getResources(), 70))
                        );

                        searchViewInnerText.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                if (s.length() == 0) {
                                    searchViewInnerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40);
                                } else {
                                    searchViewInnerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50);
                                }
                            }

                            @Override
                            public void afterTextChanged(Editable s) {}
                        });
                    }
                }
            });
        }

        searchView.setSubmitButtonEnabled(true);
        RecyclerView listView = view.findViewById(R.id.cryptoRecipientsList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());
        listView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        doneButton = view.findViewById(R.id.cryptoRecipientsDoneButton);
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);
        return view;
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        navigator.addBackButtonClickListener(this);
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
    }

    @Override
    protected void onDetach(@NonNull View view) {
        disposables.detach();
        navigator.removeBackButtonClickListener(this);
        super.onDetach(view);
    }
}
