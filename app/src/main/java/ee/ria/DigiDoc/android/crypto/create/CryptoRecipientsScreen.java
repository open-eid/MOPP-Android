package ee.ria.DigiDoc.android.crypto.create;

import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static android.view.View.VISIBLE;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxSearchView.queryTextChangeEvents;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.Constants.MAXIMUM_PERSONAL_CODE_LENGTH;
import static ee.ria.DigiDoc.android.Constants.VOID;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding4.widget.SearchViewQueryTextEvent;

import org.apache.commons.lang3.StringUtils;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.validator.PersonalCodeValidator;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.TextUtil;
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

    private String submittedQuery = "";
    private ImmutableList<Certificate> recipients = ImmutableList.of();

    @SuppressWarnings("WeakerAccess")
    public CryptoRecipientsScreen(Bundle args) {
        super(args);
        cryptoCreateScreenId = args.getString(KEY_CRYPTO_CREATE_SCREEN_ID);
    }

    private Observable<Intent.RecipientsScreenUpButtonClickIntent>
            recipientsScreenUpButtonClickIntent() {
        return navigationClicks(toolbarView)
                .map(ignored -> Intent.RecipientsScreenUpButtonClickIntent.create());
    }

    private Observable<Intent.RecipientsScreenDoneButtonClickIntent>
            recipientsScreenDoneButtonClickIntent() {
        return clicks(doneButton)
                .map(ignored -> Intent.RecipientsScreenDoneButtonClickIntent.create());
    }

    private Observable<Intent.RecipientsSearchIntent> recipientsSearchIntent() {
        return Observable.merge(
                queryTextChangeEvents(searchView)
                        .filter(SearchViewQueryTextEvent::isSubmitted)
                        .doOnNext(ignored -> {
                            String query = StringUtils.trim(searchView.getQuery().toString());
                            submittedQuery = query;
                            setDoneSearchQuery(query);
                        })
                        .map(event ->
                                Intent.RecipientsSearchIntent.search(
                                        StringUtils.trim(event.getQueryText().toString()))),
                backButtonClicksSubject.map(ignored -> Intent.RecipientsSearchIntent.clear()),
                clicks(searchView)
                        .map(ignored -> StringUtils.trim(searchView.getQuery().toString()))
                        .filter(trimmedQuery -> (trimmedQuery != null &&
                                !trimmedQuery.isEmpty()) && !submittedQuery.equals(trimmedQuery))
                        .map(query -> Intent.RecipientsSearchIntent.search(
                                setSearchQuery(query))));
    }

    private Observable<Intent.RecipientAddIntent> recipientAddIntent() {
        return adapter.recipientAddClicks()
                .map(recipient -> Intent.RecipientAddIntent.create(recipients, recipient));
    }

    private Observable<Intent.RecipientAddAllIntent> recipientAddAllIntent() {
        return adapter.recipientAddAllClicks()
                .map(addedRecipients -> Intent.RecipientAddAllIntent.create(recipients, addedRecipients));
    }

    private Observable<Intent.RecipientRemoveIntent> recipientRemoveIntent() {
        return adapter.recipientRemoveClicks()
                .map(recipient -> Intent.RecipientRemoveIntent.create(recipients, recipient));
    }

    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(recipientsScreenUpButtonClickIntent(), recipientsScreenDoneButtonClickIntent(),
                recipientsSearchIntent(), recipientAddIntent(), recipientAddAllIntent(), recipientRemoveIntent());
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
    }

    @Override
    public void render(ViewState state) {
        recipients = state.recipients();
        setActivity(state.recipientsSearchState().equals(State.ACTIVE));
        adapter.dataForRecipients(state.recipientsSearchState(), state.recipientsSearchResult(), state.recipientsSearchError(),
                recipients);
        if (doneButton != null) {
            doneButton.setVisibility(recipients.isEmpty() ? GONE : VISIBLE);
            if (getApplicationContext() != null) {
                doneButton.setBackgroundColor(recipients.isEmpty() ? Color.GRAY :
                        ContextCompat.getColor(getApplicationContext(), R.color.bottomNavigation));
            }
        }
    }

    @Override
    public boolean onBackButtonClick() {
        backButtonClicksSubject.onNext(VOID);
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

    private void removeDefaultSearchButton(SearchView searchView) {
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchView.setSubmitButtonEnabled(false);
                if (getResources() != null) {
                    ImageView rightSearchButton = searchView.findViewById(
                            getResources()
                                    .getIdentifier("android:id/search_go_btn", null, null));
                    if (rightSearchButton != null) {
                        rightSearchButton.setVisibility(GONE);
                        rightSearchButton.setImageDrawable(null);
                        rightSearchButton.setEnabled(false);
                    }
                }
            }
        });
    }

    private void setButtonsVisibility(View searchTextView, View searchPlate, int visibility) {
        ((LinearLayout) searchTextView.getParent()).findViewById(getResources()
                .getIdentifier("android:id/search_close_btn", null, null))
                .setVisibility(visibility);
        ((LinearLayout) searchPlate.getParent()).findViewById(getResources()
                .getIdentifier("android:id/search_mag_icon", null, null))
                .setVisibility(visibility);
    }

    private void setMagIconClickable(View searchTextView, View searchPlate, ImageView searchButton) {
        searchTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        searchView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        searchView.requestFocus();

        ((LinearLayout) searchTextView.getParent()).findViewById(getResources()
                .getIdentifier("android:id/search_close_btn", null, null))
                .setVisibility(GONE);

        searchButton.setVisibility(VISIBLE);
        setButtonsVisibility(searchTextView, searchPlate, GONE);
        ((LinearLayout) searchButton.getParent()).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private void setButtonsVisibilityOnLayoutChange(View searchTextView, View searchPlate, ImageView searchButton) {
        searchButton.setVisibility(VISIBLE);
        setButtonsVisibility(searchTextView, searchPlate, GONE);
        ((LinearLayout) searchButton.getParent()).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private void hideSearchCloseButton(View searchTextView) {
        ((LinearLayout) searchTextView.getParent()).findViewById(getResources()
                .getIdentifier("android:id/search_close_btn", null, null))
                .setVisibility(GONE);
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        View view = inflater.inflate(R.layout.crypto_recipients_screen, container, false);

        AccessibilityUtils.setViewAccessibilityPaneTitle(view, R.string.crypto_recipients_title);

        toolbarView = view.findViewById(R.id.toolbar);
        toolbarView.setTitle(R.string.crypto_recipients_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        searchView = view.findViewById(R.id.cryptoRecipientsSearch);
        searchView.setIconifiedByDefault(false);
        View searchTextView = searchView.findViewById(getResources().getIdentifier("android:id/search_src_text", null, null));
        searchTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        searchView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        if (searchTextView instanceof TextView) {
            AccessibilityUtils.setSingleCharactersContentDescription((TextView) searchTextView);
        }

        hideSearchCloseButton(searchTextView);

        View searchPlate = searchView.findViewById(getResources().getIdentifier("android:id/search_plate", null, null));
        ImageView searchButton = searchView.findViewById(getResources().getIdentifier("android:id/search_button", null, null));

        searchButton.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            setupContentDescriptions(searchTextView, submittedQuery);
            setMagIconClickable(searchTextView, searchPlate, searchButton);
        });

        searchButton.setOnClickListener(v -> {
            setupContentDescriptions(searchTextView, submittedQuery);
            searchView.performClick();
            setMagIconClickable(searchTextView, searchPlate, searchButton);
        });

        if (getResources() != null) {
            searchView.setQueryHint(getResources().getString(R.string.crypto_recipients_search));

            // Remove ">" search button from the right side of the Search Bar
            removeDefaultSearchButton(searchView);

            searchViewInnerText = searchView.findViewById(getResources().getIdentifier("android:id/search_src_text", null, null));
            searchViewInnerText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            searchViewInnerText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    searchViewInnerText.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    setButtonsVisibilityOnLayoutChange(searchTextView, searchPlate, searchButton);

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
                                    searchViewInnerText.setSingleLine(false);
                                } else {
                                    searchViewInnerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50);
                                    searchViewInnerText.setSingleLine(true);
                                    // Validate personal codes only. Allow company registry numbers and names
                                    if (searchViewInnerText.getText() != null &&
                                            searchViewInnerText.getText().length() >= MAXIMUM_PERSONAL_CODE_LENGTH &&
                                            StringUtils.isNumeric(searchViewInnerText.getText())) {
                                        PersonalCodeValidator.validatePersonalCode(searchViewInnerText);
                                    }
                                }
                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                if (s.length() == 1) {
                                    searchViewInnerText.setSelection(searchViewInnerText.getText().length());
                                }
                            }
                        });
                    }
                }
            });
        }

        searchView.setSubmitButtonEnabled(true);
        searchView.setEnabled(true);
        RecyclerView listView = view.findViewById(R.id.cryptoRecipientsList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());
        listView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        doneButton = view.findViewById(R.id.cryptoRecipientsDoneButton);
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);
        return view;
    }

    private void setDoneSearchQuery(String text) {
        searchView.setQuery(StringUtils.trim(text), false);
        searchView.clearFocus();
    }

    private String setSearchQuery(String text) {
        if (text != null && !text.isEmpty()) {
            String trimmed = StringUtils.trim(text);
            submittedQuery = trimmed;
            setDoneSearchQuery(trimmed);
            return trimmed;
        }
        return "";
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
        searchView.setOnCloseListener(null);
        super.onDetach(view);
    }

    private void setupContentDescriptions(View view, CharSequence contentDescription) {
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                view.setContentDescription(TextUtil.splitTextAndJoin(submittedQuery, "", " "));
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String splitContentDescription = TextUtil.splitTextAndJoin(contentDescription.toString(), "", " ");
                info.setContentDescription(splitContentDescription);
                info.setCheckable(false);
                info.setClickable(false);
                info.setClassName("");
                info.setPackageName("");
                info.setText(splitContentDescription);
                info.setViewIdResourceName("");
                info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION);
            }
        });
    }
}
