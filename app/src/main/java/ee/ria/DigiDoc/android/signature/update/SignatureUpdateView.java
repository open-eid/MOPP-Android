package ee.ria.DigiDoc.android.signature.update;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import java.io.File;
import java.time.Instant;
import java.time.Month;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdResponse;
import ee.ria.DigiDoc.android.utils.DateUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.container.NameUpdateDialog;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.android.utils.widget.NotificationDialog;
import ee.ria.DigiDoc.common.ActivityUtil;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.Signature;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

@SuppressLint("ViewConstructor")
public final class SignatureUpdateView extends LinearLayout implements MviView<Intent, ViewState> {

    private final ImmutableList<String> ASICS_TIMESTAMP_CONTAINERS = ImmutableList.of("asics", "scs");
    private static final ImmutableSet<String> UNSIGNABLE_CONTAINER_EXTENSIONS = ImmutableSet.<String>builder().add("asics", "scs", "ddoc").build();

    private static final String EMPTY_CHALLENGE = "";

    private boolean isTimerStarted = false;

    private final boolean isExistingContainer;
    private final boolean isNestedContainer;
    private final File containerFile;
    private ImmutableList<DataFile> dataFiles = ImmutableList.of();
    private final boolean signatureAddVisible;
    private final boolean signatureAddSuccessMessageVisible;
    @Nullable private final File nestedFile;
    private final boolean isSivaConfirmed;

    private final Toolbar toolbarView;
    private final NameUpdateDialog nameUpdateDialog;
    private final RecyclerView listView;
    private final SignatureUpdateAdapter adapter;
    private final View activityIndicatorView;
    private final View activityOverlayView;
    private final View mobileIdContainerView;
    private final TextView mobileIdChallengeView;
    private final TextView mobileIdChallengeTextView;
    private final View smartIdContainerView;
    private final TextView smartIdInfo;
    private final TextView smartIdChallengeView;
    private final Button sendButton;
    private final View buttonSpace;
    private final Button signatureAddButton;
    private final SignatureUpdateErrorDialog errorDialog;
    private final ConfirmationDialog sivaConfirmationDialog;
    private final ConfirmationDialog documentRemoveConfirmationDialog;
    private final ConfirmationDialog signatureRemoveConfirmationDialog;
    private final SignatureUpdateSignatureAddDialog signatureAddDialog;
    private final SignatureUpdateSignatureAddView signatureAddView;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Activity activity = (Activity) getContext();

    private final Subject<Intent.DocumentsAddIntent> documentsAddIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.DocumentSaveIntent> documentSaveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.DocumentRemoveIntent> documentRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureViewIntent> signatureViewIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureRemoveIntent> signatureRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<Intent.SignatureAddIntent> signatureAddIntentSubject =
            PublishSubject.create();

    @Nullable private DataFile sivaConfirmation;
    @Nullable private DataFile documentRemoveConfirmation;
    @Nullable private Signature signatureRemoveConfirmation;

    private boolean signingInfoDelegated = false;
    private ProgressBar documentAddProgressBar;
    ProgressBar progressBar;
    SignatureUpdateProgressBar signatureUpdateProgressBar = new SignatureUpdateProgressBar();
    boolean isTitleViewFocused = false;

    public SignatureUpdateView(Context context, String screenId, boolean isExistingContainer,
                               boolean isNestedContainer, File containerFile,
                               boolean signatureAddVisible,
                               boolean signatureAddSuccessMessageVisible,
                               @Nullable File nestedFile,
                               boolean isSivaConfirmed) {
        super(context);

        this.isExistingContainer = isExistingContainer;
        this.isNestedContainer = isNestedContainer;
        this.containerFile = containerFile;
        this.signatureAddVisible = signatureAddVisible;
        this.signatureAddSuccessMessageVisible = signatureAddSuccessMessageVisible;
        this.nestedFile = nestedFile;
        this.isSivaConfirmed = isSivaConfirmed;

        navigator = Application.component(context).navigator();
        viewModel = navigator.viewModel(screenId, SignatureUpdateViewModel.class);

        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update, this);
        toolbarView = findViewById(R.id.toolbar);
        nameUpdateDialog = new NameUpdateDialog(context);
        listView = findViewById(R.id.signatureUpdateList);
        activityIndicatorView = findViewById(R.id.activityIndicator);
        activityOverlayView = findViewById(R.id.activityOverlay);
        mobileIdContainerView = findViewById(R.id.signatureUpdateMobileIdContainer);
        mobileIdChallengeView = findViewById(R.id.signatureUpdateMobileIdChallenge);
        smartIdContainerView = findViewById(R.id.signatureUpdateSmartIdContainer);
        smartIdInfo = findViewById(R.id.signatureUpdateSmartIdInfo);
        smartIdChallengeView = findViewById(R.id.signatureUpdateSmartIdChallenge);
        sendButton = findViewById(R.id.signatureUpdateSendButton);
        sendButton.setContentDescription(getResources().getString(R.string.share_container));
        buttonSpace = findViewById(R.id.signatureUpdateButtonSpace);
        signatureAddButton = findViewById(R.id.signatureUpdateSignatureAddButton);
        mobileIdChallengeTextView = findViewById(R.id.signatureUpdateMobileIdChallengeText);
        documentAddProgressBar = findViewById(R.id.signatureAddDocumentProgress);

        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(adapter = new SignatureUpdateAdapter());

        documentRemoveConfirmationDialog = new ConfirmationDialog(context,
                R.string.signature_update_remove_document_confirmation_message, R.id.documentRemovalDialog);
        signatureRemoveConfirmationDialog = new ConfirmationDialog(context,
                R.string.signature_update_signature_remove_confirmation_message, R.id.signatureRemovalDialog);
        sivaConfirmationDialog = new ConfirmationDialog(Activity.getContext().get(),
                R.string.siva_send_message_dialog, R.id.sivaConfirmationDialog);
        signatureAddDialog = new SignatureUpdateSignatureAddDialog(context);
        signatureAddView = signatureAddDialog.view();
        resetSignatureAddDialog();

        errorDialog = new SignatureUpdateErrorDialog(context, documentsAddIntentSubject,
                documentRemoveIntentSubject, signatureAddIntentSubject,
                signatureRemoveIntentSubject, signatureAddDialog, this);

        progressBar = (ProgressBar) activityIndicatorView;
        documentAddProgressBar.setVisibility(GONE);

        setupAccessibilityTabs();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), nameUpdateIntent(), addDocumentsIntent(),
                documentViewIntent(), documentSaveIntent(), documentRemoveIntent(), signatureRemoveIntent(),
                signatureAddIntent(), signatureViewIntent(), sendIntent());
    }

    private void checkIfDdocParentContainerIsTimestamped(ImmutableList<Signature> signatures, ImmutableList<DataFile> dataFiles) {
        if (!isNestedContainer && ASICS_TIMESTAMP_CONTAINERS.contains(Files.getFileExtension(containerFile.getName()).toLowerCase()) &&
                dataFiles.size() == 1 && signatures.size() == 1 &&
                Files.getFileExtension(dataFiles.get(0).name()).equalsIgnoreCase("ddoc")) {
            Instant dateTimeInstant = DateUtil.toEpochSecond(2018, Month.JULY, 1, 0, 0, 0);
            activity.getSettingsDataStore().setIsDdocParentContainerTimestamped(
                    !signatures.get(0).createdAt().isAfter(dateTimeInstant));
            return;
        }

        activity.getSettingsDataStore().setIsDdocParentContainerTimestamped(
                !Files.getFileExtension(containerFile.getName()).equalsIgnoreCase("ddoc") && !isNestedContainer);
    }

    @Override
    public void render(ViewState state) {
        if (state.containerLoadError() != null) {
            int messageId = state.containerLoadError() instanceof NoInternetConnectionException
                    ? R.string.no_internet_connection : R.string.signature_update_container_load_error;
            Toast.makeText(getContext(), messageId, Toast.LENGTH_LONG).show();
            navigator.execute(Transaction.pop());
            signatureUpdateProgressBar.stopProgressBar(progressBar, isTimerStarted);
            isTimerStarted = false;
            return;
        }


        nameUpdateDialog.render(showNameUpdate(state), state.nameUpdateName(), state.nameUpdateError());

        int titleResId = isExistingContainer ? R.string.signature_update_title_existing
                : R.string.signature_update_title_created;
        toolbarView.setTitle(titleResId);
        toolbarView.setNavigationIcon(R.drawable.ic_clear);
        toolbarView.setNavigationContentDescription(R.string.close);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setAccessibilityPaneTitle(isExistingContainer ? getResources().getString(titleResId) : "Container signing");
        }

        listView.clearFocus();

        TextView titleView = getTitleView(toolbarView);
        AccessibilityUtils.disableDoubleTapToActivateFeedback(titleView);
        if (titleView != null && !isTitleViewFocused && isNestedContainer) {
            titleView.postDelayed(() -> {
                titleView.requestFocus();
                titleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 1000);
            isTitleViewFocused = true;
        }

        if (isNestedContainer) {
            sendButton.setVisibility(GONE);
            buttonSpace.setVisibility(GONE);
            signatureAddButton.setVisibility(GONE);
        } else {
            sendButton.setVisibility(isExistingContainer ? VISIBLE : GONE);
            buttonSpace.setVisibility(isExistingContainer ? VISIBLE : GONE);
            if (UNSIGNABLE_CONTAINER_EXTENSIONS.contains(
                    Files.getFileExtension(containerFile.getName()).toLowerCase()) ||
                    FileSystem.isEmptyDataFileInContainer(containerFile)) {
                signatureAddButton.setVisibility(GONE);
            } else {
                signatureAddButton.setVisibility(VISIBLE);
            }

            if (state.container() != null) {
                checkIfDdocParentContainerIsTimestamped(state.container().signatures(), state.container().dataFiles());
            }
        }

        if (state.container() != null) {
            dataFiles = state.container().dataFiles();
        }
        signatureAddButton.setContentDescription(getResources().getString(R.string.sign_container_button_description));

        if (state.containerLoadInProgress() || state.documentsAddInProgress() ||
                state.documentRemoveInProgress() || state.signatureRemoveInProgress()) {
            documentAddProgressBar.setVisibility(VISIBLE);
        } else {
            documentAddProgressBar.setVisibility(GONE);
        }

        setActivity(state.containerLoadInProgress() || state.documentsAddInProgress()
                || state.documentViewState().equals(State.ACTIVE)
                || state.documentRemoveInProgress() || state.signatureRemoveInProgress()
                || state.signatureAddActivity());
        adapter.setData(state.signatureAddSuccessMessageVisible(), isExistingContainer,
                isNestedContainer, state.container(), nestedFile, isSivaConfirmed);

        if (state.signatureAddSuccessMessageVisible()) {
            showSuccessNotification();
            if (AccessibilityUtils.isAccessibilityEnabled()) {
                AccessibilityUtils.interrupt(getContext());
                AccessibilityUtils.sendAccessibilityEvent(getContext(),
                        TYPE_ANNOUNCEMENT, R.string.container_signature_added);
            }
        }

        errorDialog.show(state.documentsAddError(), state.documentRemoveError(),
                state.signatureAddError(), state.signatureRemoveError());

        documentRemoveConfirmation = state.documentRemoveConfirmation();
        if (documentRemoveConfirmation != null) {
            if (dataFiles.size() == 1) {
                documentRemoveConfirmationDialog.setMessage(getResources().getString(R.string.signature_update_remove_last_document_confirmation_message));
            } else {
                documentRemoveConfirmationDialog.setMessage(getResources().getString(R.string.signature_update_remove_document_confirmation_message));
            }
            documentRemoveConfirmationDialog.show();
        } else {
            documentRemoveConfirmationDialog.dismiss();
        }

        signatureRemoveConfirmation = state.signatureRemoveConfirmation();
        if (signatureRemoveConfirmation != null) {
            signatureRemoveConfirmationDialog.show();
        } else {
            signatureRemoveConfirmationDialog.dismiss();
        }

        Integer signatureAddMethod = state.signatureAddMethod();
        if (signatureAddMethod == null) {
            signatureAddDialog.dismiss();
        } else {
            signatureAddDialog.show();
            signatureAddView.method(signatureAddMethod);
        }

        SignatureAddResponse signatureAddResponse = state.signatureAddResponse();
        signatureAddView.response(signatureAddResponse);

        // should be in the MobileIdView in dialog
        mobileIdContainerView.setVisibility(
                signatureAddResponse instanceof MobileIdResponse
                        ? VISIBLE
                        : GONE);
        if (signatureAddResponse instanceof MobileIdResponse) {
            MobileIdResponse mobileIdResponse = (MobileIdResponse) signatureAddResponse;
            String mobileIdChallenge = mobileIdResponse.challenge();
            if (mobileIdChallenge != null) {
                mobileIdChallengeView.setText(mobileIdChallenge);
                mobileIdChallengeTextView.setText(getResources().getString(R.string.signature_update_mobile_id_info));
            } else {
                mobileIdChallengeView.setText(EMPTY_CHALLENGE);
            }
        }

        sivaConfirmation = state.sivaConfirmation();

        if (sivaConfirmation != null) {
            sivaConfirmationDialog.show();
        } else {
            sivaConfirmationDialog.dismiss();
        }

        tintCompoundDrawables(sendButton);
        tintCompoundDrawables(signatureAddButton);

        if (mobileIdContainerView.getVisibility() == VISIBLE) {
            if (Build.VERSION.SDK_INT >= 26) {
                mobileIdContainerView.setFocusedByDefault(true);
            }
            mobileIdContainerView.setFocusable(true);
            mobileIdContainerView.setFocusableInTouchMode(true);

            if (isTimerStarted) {
                signatureUpdateProgressBar.startProgressBar(progressBar);
            }

            isTimerStarted = true;

            if (!signingInfoDelegated) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_status_request_sent);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_info);
                signingInfoDelegated = true;
            }

            if (!mobileIdChallengeView.getText().equals(EMPTY_CHALLENGE)) {
                String mobileIdChallengeDescription = getResources().getString(R.string.mobile_id_challenge) + mobileIdChallengeView.getText();
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, mobileIdChallengeDescription);
            }
        }

        smartIdContainerView.setVisibility(
                signatureAddResponse instanceof SmartIdResponse ? VISIBLE : GONE);
        if (smartIdContainerView.getVisibility() == VISIBLE) {
            if (Build.VERSION.SDK_INT >= 26) {
                smartIdContainerView.setFocusedByDefault(true);
            }
            smartIdContainerView.setFocusable(true);
            smartIdContainerView.setFocusableInTouchMode(true);

            if (isTimerStarted) {
                signatureUpdateProgressBar.startProgressBar(progressBar);
            }
            isTimerStarted = true;

            if (!signingInfoDelegated) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_status_request_sent);
                signingInfoDelegated = true;
            }

            SmartIdResponse smartIdResponse = (SmartIdResponse) signatureAddResponse;
            if (smartIdResponse.selectDevice()) {
                smartIdInfo.setText(R.string.signature_update_smart_id_select_device);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, smartIdInfo.getText());
            }
            String smartIdChallenge = smartIdResponse.challenge();
            if (smartIdChallenge != null) {
                smartIdChallengeView.setText(smartIdChallenge);
                smartIdInfo.setText(R.string.signature_update_smart_id_info);
                String smartIdChallengeDescription = getResources().getString(R.string.smart_id_challenge) + smartIdChallenge;
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, smartIdChallengeDescription);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_smart_id_info);
            } else {
                smartIdChallengeView.setText(EMPTY_CHALLENGE);
            }
        }

        setupAccessibilityTabs();
    }

    private void showSuccessNotification() {
        Boolean showNotification = ((Activity) getContext()).getSettingsDataStore().getShowSuccessNotification();
        if (showNotification) {
            NotificationDialog successNotificationDialog = new NotificationDialog((Activity) getContext());
            if (AccessibilityUtils.isAccessibilityEnabled()) {
                new Handler(Looper.getMainLooper()).postDelayed(successNotificationDialog::show, 2500);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(successNotificationDialog::show, 1000);
            }
        }
    }

    private boolean showNameUpdate(ViewState state) {
        return state.container() != null && state.container().signatures().isEmpty() && state.nameUpdateShowing();
    }

    private void setActivity(boolean activity) {
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
        sendButton.setEnabled(!activity);
        signatureAddButton.setEnabled(!activity);
        if (!activity && isTimerStarted) {
            signatureUpdateProgressBar.stopProgressBar(progressBar, isTimerStarted);
            isTimerStarted = false;
        }
    }

    private void resetSignatureAddDialog() {
        signatureAddView.reset(viewModel);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(isExistingContainer, containerFile,
                signatureAddVisible ? viewModel.signatureAddMethod() : null,
                signatureAddSuccessMessageVisible));
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.NameUpdateIntent> nameUpdateIntent() {
        return Observable.mergeArray(
                adapter.nameUpdateClicks().map(ignored -> Intent.NameUpdateIntent.show(containerFile)),
                cancels(nameUpdateDialog).map(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.container_name_change_cancelled);
                    return Intent.NameUpdateIntent.clear();
                }),
                nameUpdateDialog.updates().map(name -> Intent.NameUpdateIntent.update(containerFile, name)));
    }

    private Observable<Intent.DocumentsAddIntent> addDocumentsIntent() {
        return adapter.documentAddClicks()
                .map(ignored -> Intent.DocumentsAddIntent.create(containerFile))
                .mergeWith(documentsAddIntentSubject);
    }

    private Observable<Intent.DocumentViewIntent> documentViewIntent() {
        return Observable.mergeArray(adapter.documentClicks()
                .map(document -> Intent.DocumentViewIntent.confirmation((nestedFile != null && isSivaConfirmed) ? nestedFile : containerFile, document)),
                        sivaConfirmationDialog.positiveButtonClicks()
                                .map(ignored -> Intent.DocumentViewIntent.open((nestedFile != null && isSivaConfirmed) ? nestedFile : containerFile, sivaConfirmation)),
                        sivaConfirmationDialog.cancels()
                                .map(ignored -> Intent.DocumentViewIntent.cancel()));
    }

    private Observable<Intent.DocumentSaveIntent> documentSaveIntent() {
        return documentSaveIntentSubject;
    }

    private Observable<Intent.DocumentRemoveIntent> documentRemoveIntent() {
        return documentRemoveIntentSubject;
    }

    private Observable<Intent.SignatureViewIntent> signatureViewIntent() {
        return adapter.signatureClicks()
                .map(document -> Intent.SignatureViewIntent.create(containerFile, document));
    }

    private Observable<Intent.SignatureRemoveIntent> signatureRemoveIntent() {
        return signatureRemoveIntentSubject;
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.SignatureAddIntent> signatureAddIntent() {
        return Observable.mergeArray(
                clicks(signatureAddButton)
                        .doOnNext(ignored -> resetSignatureAddDialog())
                        .map(ignored -> {
                            int method = viewModel.signatureAddMethod();
                            return Intent.SignatureAddIntent.show(method, isExistingContainer, containerFile);
                        }),
                cancels(signatureAddDialog)
                        .doOnNext(ignored -> resetSignatureAddDialog())
                        .map(ignored -> Intent.SignatureAddIntent.clear()),
                signatureAddView.methodChanges().map(method -> {
                        viewModel.setSignatureAddMethod(method);
                        sendMethodSelectionAccessibilityEvent(method);
                        return Intent.SignatureAddIntent.show(method, isExistingContainer, containerFile);
                }),
                signatureAddDialog.positiveButtonClicks().map(ignored -> Intent.SignatureAddIntent.sign(signatureAddView.method(),
                        isExistingContainer, containerFile, signatureAddView.request())),
                signatureAddIntentSubject
        );
    }

    private Observable<Intent.SendIntent> sendIntent() {
        return clicks(sendButton)
                .map(ignored -> Intent.SendIntent.create(containerFile));
    }

    private void sendMethodSelectionAccessibilityEvent(int method) {
        String signatureMethod = getMethod(method);
        if (signatureMethod != null) {
            if (signatureMethod.equalsIgnoreCase(getResources().getString(R.string.signature_update_signature_add_method_mobile_id))) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_WINDOW_STATE_CHANGED,
                        getResources().getString(R.string.signature_update_signature_chosen_method_mobile_id));
            } else if (signatureMethod.equalsIgnoreCase(getResources().getString(R.string.signature_update_signature_add_method_smart_id))) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_WINDOW_STATE_CHANGED,
                        getResources().getString(R.string.signature_update_signature_chosen_method_smart_id));
            } else if (signatureMethod.equalsIgnoreCase(getResources().getString(R.string.signature_update_signature_add_method_id_card))) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_WINDOW_STATE_CHANGED,
                        getResources().getString(R.string.signature_update_signature_chosen_method_id_card));
            }
        }
    }

    private String getMethod(int method) {
        if (method == R.id.signatureUpdateSignatureAddMethodMobileId) {
            return getResources().getString(R.string.signature_update_signature_add_method_mobile_id);
        } else if (method == R.id.signatureUpdateSignatureAddMethodSmartId) {
            return getResources().getString(R.string.signature_update_signature_add_method_smart_id);
        } else if (method == R.id.signatureUpdateSignatureAddMethodIdCard) {
            return getResources().getString(R.string.signature_update_signature_add_method_id_card);
        }
        return null;
    }

    private TextView getTitleView(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            if (toolbar.getChildAt(i) instanceof TextView) {
                return ((TextView) toolbar.getChildAt(i));
            }
        }
        return null;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());
        disposables.add(navigationClicks(toolbarView).subscribe(o -> {
            if (ActivityUtil.isExternalFileOpened(activity)) {
                ActivityUtil.restartActivity(getContext(), activity);
            } else {
                navigator.execute(Transaction.pop());
            }
        }));
        disposables.add(adapter.scrollToTop().subscribe(ignored -> listView.scrollToPosition(0)));
        disposables.add(adapter.documentSaveClicks().subscribe(document ->
                documentSaveIntentSubject.onNext(Intent.DocumentSaveIntent
                        .create((nestedFile != null && isSivaConfirmed) ? nestedFile : containerFile, document))));
        disposables.add(adapter.signatureClicks().subscribe(signature ->
                signatureViewIntentSubject.onNext(Intent.SignatureViewIntent
                        .create(containerFile, signature))));
        disposables.add(adapter.documentRemoveClicks().subscribe(document ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent
                        .showConfirmation(containerFile, dataFiles, document))));
        disposables.add(documentRemoveConfirmationDialog.positiveButtonClicks().subscribe(ignored ->
                documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent
                        .remove(containerFile, dataFiles, documentRemoveConfirmation))));
        disposables.add(documentRemoveConfirmationDialog.cancels().subscribe(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.document_removal_cancelled);
                    documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent.clear());
                }));
        disposables.add(adapter.signatureRemoveClicks().subscribe(signature ->
                signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent
                        .showConfirmation(containerFile, signature))));
        disposables.add(signatureRemoveConfirmationDialog.positiveButtonClicks()
                .subscribe(ignored -> signatureRemoveIntentSubject
                        .onNext(Intent.SignatureRemoveIntent
                                .remove(containerFile, signatureRemoveConfirmation))));
        disposables.add(signatureRemoveConfirmationDialog.cancels().subscribe(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_removal_cancelled);
                    signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent.clear());
                }));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        signatureAddDialog.dismiss();
        sivaConfirmationDialog.dismiss();
        signatureRemoveConfirmationDialog.dismiss();
        documentRemoveConfirmationDialog.dismiss();
        errorDialog.setOnDismissListener(null);
        errorDialog.dismiss();
        nameUpdateDialog.dismiss();
        isTitleViewFocused = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel -> {
            parcel.writeBundle(signatureAddDialog.onSaveInstanceState());
            parcel.writeBundle(nameUpdateDialog.onSaveInstanceState());
        });
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel -> {
            signatureAddDialog.onRestoreInstanceState(
                    parcel.readBundle(getClass().getClassLoader()));
            nameUpdateDialog.onRestoreInstanceState(parcel.readBundle(getClass().getClassLoader()));
        }));
    }

    private void setupAccessibilityTabs() {
        if (sendButton.getVisibility() == VISIBLE && signatureAddButton.getVisibility() == VISIBLE) {
            sendButton.setContentDescription(getResources().getString(R.string.decrypt_send_content_description, 1, 2));
            signatureAddButton.setContentDescription(getResources().getString(R.string.sign_send_content_description, 2, 2));
        } else {
            sendButton.setContentDescription(getResources().getString(R.string.signature_update_send_button));
            signatureAddButton.setContentDescription(getResources().getString(R.string.signature_update_signature_add_button));
        }
    }
}
