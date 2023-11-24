package ee.ria.DigiDoc.android.signature.update;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.accessibility.AccessibilityUtils.isLargeFontEnabled;
import static ee.ria.DigiDoc.android.accessibility.AccessibilityUtils.isSmallFontEnabled;
import static ee.ria.DigiDoc.android.utils.TextUtil.convertPxToDp;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;
import static ee.ria.DigiDoc.android.utils.display.DisplayUtil.getDisplayMetricsDpToInt;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import java.io.File;
import java.time.Instant;
import java.time.Month;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.signature.update.mobileid.MobileIdResponse;
import ee.ria.DigiDoc.android.signature.update.smartid.SmartIdResponse;
import ee.ria.DigiDoc.android.utils.DateUtil;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.container.NameUpdateDialog;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.android.utils.widget.NotificationDialog;
import ee.ria.DigiDoc.common.ActivityUtil;
import ee.ria.DigiDoc.mobileid.service.MobileSignService;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.SSLHandshakeException;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.smartid.service.SmartSignService;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

@SuppressLint("ViewConstructor")
public final class SignatureUpdateView extends LinearLayout implements ContentView, MviView<Intent, ViewState> {

    private final ImmutableList<String> ASICS_TIMESTAMP_CONTAINERS = ImmutableList.of("asics", "scs");
    private static final ImmutableSet<String> UNSIGNABLE_CONTAINER_EXTENSIONS = ImmutableSet.<String>builder().add("adoc", "asics", "scs", "ddoc").build();

    private static final String EMPTY_CHALLENGE = "";

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
    private final View mobileIdActivityIndicatorView;
    private final View smartIdActivityIndicatorView;
    private final View activityOverlayView;
    private final View mobileIdContainerView;
    private final TextView mobileIdChallengeView;
    private final TextView mobileIdChallengeTextView;
    private final Button mobileIdCancelButton;
    private final View smartIdContainerView;
    private final TextView smartIdInfo;
    private final TextView smartIdChallengeView;
    private final Button smartIdCancelButton;
    private final Button sendButton;
    private final View signatureButtonSpace;
    private final View encryptButtonSpace;
    private final Button signatureAddButton;
    private final Button signatureEncryptButton;
    private final SignatureUpdateErrorDialog errorDialog;
    private final ConfirmationDialog sivaConfirmationDialog;
    private final ConfirmationDialog documentRemoveConfirmationDialog;
    private final ConfirmationDialog signatureRemoveConfirmationDialog;
    private final SignatureUpdateSignatureAddDialog signatureAddDialog;
    private final RoleAddDialog roleAddDialog;
    private final SignatureUpdateSignatureAddView signatureAddView;
    private final RoleAddView roleAddView;

    private final Navigator navigator;
    private final SignatureUpdateViewModel viewModel;
    private final RoleViewModel roleViewModel;
    private final ViewDisposables disposables = new ViewDisposables();

    private final Activity activity = (Activity) getContext();

    private final Subject<DocumentsAddIntent> documentsAddIntentSubject =
            PublishSubject.create();
    private final Subject<DocumentSaveIntent> documentSaveIntentSubject =
            PublishSubject.create();
    private final Subject<DocumentRemoveIntent> documentRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<SignatureViewIntent> signatureViewIntentSubject =
            PublishSubject.create();
    private final Subject<SignatureRemoveIntent> signatureRemoveIntentSubject =
            PublishSubject.create();
    private final Subject<SignatureRoleViewIntent> signatureRoleDetailsIntentSubject =
            PublishSubject.create();    
    private final Subject<SignatureAddIntent> signatureAddIntentSubject =
            PublishSubject.create();

    @Nullable private DataFile sivaConfirmation;
    @Nullable private DataFile documentRemoveConfirmation;
    @Nullable private Signature signatureRemoveConfirmation;

    private boolean isRoleViewShown = false;
    private boolean signingInfoDelegated = false;
    private final ProgressBar documentAddProgressBar;
    private final ProgressBar mobileIdProgressBar;
    private final ProgressBar smartIdProgressBar;
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

        navigator = ApplicationApp.component(context).navigator();
        viewModel = navigator.viewModel(screenId, SignatureUpdateViewModel.class);
        roleViewModel = navigator.viewModel(screenId, RoleViewModel.class);

        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update, this);
        toolbarView = findViewById(R.id.toolbar);
        nameUpdateDialog = new NameUpdateDialog(context);
        listView = findViewById(R.id.signatureUpdateList);
        mobileIdActivityIndicatorView = findViewById(R.id.activityIndicatorMobileId);
        smartIdActivityIndicatorView = findViewById(R.id.activityIndicatorSmartId);
        activityOverlayView = findViewById(R.id.activityOverlay);
        mobileIdContainerView = findViewById(R.id.signatureUpdateMobileIdContainer);
        mobileIdChallengeView = findViewById(R.id.signatureUpdateMobileIdChallenge);
        smartIdContainerView = findViewById(R.id.signatureUpdateSmartIdContainer);
        smartIdInfo = findViewById(R.id.signatureUpdateSmartIdInfo);
        smartIdChallengeView = findViewById(R.id.signatureUpdateSmartIdChallenge);
        smartIdCancelButton = findViewById(R.id.signatureUpdateSmartIdCancelButton);
        smartIdCancelButton.setContentDescription(getResources().getString(R.string.cancel_button_accessibility));
        sendButton = findViewById(R.id.signatureUpdateSendButton);
        sendButton.setContentDescription(getResources().getString(R.string.share_container));
        signatureButtonSpace = findViewById(R.id.signatureUpdateSignatureButtonSpace);
        encryptButtonSpace = findViewById(R.id.signatureUpdateEncryptButtonSpace);
        signatureAddButton = findViewById(R.id.signatureUpdateSignatureAddButton);
        signatureEncryptButton = findViewById(R.id.signatureUpdateSignatureEncryptButton);
        mobileIdChallengeTextView = findViewById(R.id.signatureUpdateMobileIdChallengeText);
        mobileIdCancelButton = findViewById(R.id.signatureUpdateMobileIdCancelButton);
        mobileIdCancelButton.setContentDescription(getResources().getString(R.string.cancel_button_accessibility));
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

        roleAddDialog = new RoleAddDialog(context);
        roleAddView = roleAddDialog.view();
        resetRoleAddDialog();

        errorDialog = new SignatureUpdateErrorDialog(context, documentsAddIntentSubject,
                documentRemoveIntentSubject, signatureAddIntentSubject,
                signatureRoleDetailsIntentSubject, signatureRemoveIntentSubject,
                signatureAddDialog, this);

        mobileIdProgressBar = (ProgressBar) mobileIdActivityIndicatorView;
        smartIdProgressBar = (ProgressBar) smartIdActivityIndicatorView;
        documentAddProgressBar.setVisibility(GONE);

        setupAccessibilityTabs();

        setActionButtonsTextSize();

        ContentView.addInvisibleElement(getContext(), this);

        View lastElementView = findViewById(R.id.lastInvisibleElement);
        if (lastElementView != null) {
            lastElementView.setVisibility(VISIBLE);

            ContentView.removeInvisibleElementScrollListener(listView);
            ContentView.addInvisibleElementScrollListener(listView, lastElementView);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setActionButtonsTextSize();
    }

    private void setActionButtonsTextSize() {

        signatureAddButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        signatureEncryptButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        sendButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        mobileIdCancelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        smartIdCancelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);

        if (isLargeFontEnabled(getResources()) || isSmallFontEnabled(getResources())) {
            signatureAddButton.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, COMPLEX_UNIT_SP);
            signatureEncryptButton.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, COMPLEX_UNIT_SP);
            sendButton.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, COMPLEX_UNIT_SP);
            mobileIdCancelButton.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, COMPLEX_UNIT_SP);
            smartIdCancelButton.setAutoSizeTextTypeUniformWithConfiguration(7, 12, 1, COMPLEX_UNIT_SP);
        } else {
            signatureAddButton.setAutoSizeTextTypeUniformWithConfiguration(11, 20, 1, COMPLEX_UNIT_SP);
            signatureEncryptButton.setAutoSizeTextTypeUniformWithConfiguration(11, 20, 1, COMPLEX_UNIT_SP);
            sendButton.setAutoSizeTextTypeUniformWithConfiguration(11, 20, 1, COMPLEX_UNIT_SP);
            mobileIdCancelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
            smartIdCancelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        }
    }

    private void setSigningModalSize(View view) {
        if (isLargeFontEnabled(getResources())) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
            int marginTop = getDisplayMetricsDpToInt(getResources(), 4);
            int marginBottom = getDisplayMetricsDpToInt(getResources(), 4);
            layoutParams.setMargins(layoutParams.leftMargin, marginTop, layoutParams.rightMargin, marginBottom);
            view.setLayoutParams(layoutParams);
            int padding = convertPxToDp(4, getContext());
            view.setPadding(padding, padding, padding, padding);
        } else {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
            int marginTop = getResources().getDimensionPixelSize(R.dimen.material_dialog_screen_edge_margin_minimum_vertical);
            int marginBottom = getResources().getDimensionPixelSize(R.dimen.material_dialog_screen_edge_margin_minimum_vertical);
            int padding = getResources().getDimensionPixelSize(R.dimen.material_card_title_block_padding_horizontal);
            layoutParams.setMargins(layoutParams.leftMargin, marginTop, layoutParams.rightMargin, marginBottom);
            view.setLayoutParams(layoutParams);
            view.setPadding(padding, padding, padding, padding);
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), nameUpdateIntent(), addDocumentsIntent(),
                documentViewIntent(), documentSaveIntent(), documentRemoveIntent(), 
                signatureRoleViewIntent(), signatureRemoveIntent(),
                signatureAddIntent(), signatureViewIntent(), encryptIntent(), saveIntent(), sendIntent());
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
            if (state.containerLoadError() instanceof SSLHandshakeException) {
                Timber.log(Log.ERROR, state.containerLoadError(), "Unable to open container. SSL handshake was not successful");
                messageId = ((SSLHandshakeException) state.containerLoadError()).getMessageId();
            }
            ToastUtil.showError(getContext(), messageId);
            navigator.execute(Transaction.pop());
            SignatureUpdateProgressBar.stopProgressBar(mobileIdProgressBar);
            SignatureUpdateProgressBar.stopProgressBar(smartIdProgressBar);
            return;
        }


        nameUpdateDialog.render(showNameUpdate(state), state.nameUpdateName(), state.nameUpdateError());

        int titleResId = isExistingContainer ? R.string.signature_update_title_existing
                : R.string.signature_update_title_created;
        toolbarView.setTitle(titleResId);
        toolbarView.setNavigationIcon(R.drawable.ic_clear);
        toolbarView.setNavigationContentDescription(R.string.close);

        setAccessibilityPaneTitle(isExistingContainer ? getResources().getString(titleResId) : "Container signing");

        listView.clearFocus();

        TextView titleView = getTitleView(toolbarView);
        if (titleView != null) {
            AccessibilityUtils.disableDoubleTapToActivateFeedback(titleView);
            if (!isTitleViewFocused && isNestedContainer) {
                titleView.postDelayed(() -> {
                    titleView.requestFocus();
                    titleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                }, 1000);
                isTitleViewFocused = true;
            }
        }

        if (isNestedContainer) {
            sendButton.setVisibility(GONE);
            signatureButtonSpace.setVisibility(GONE);
            encryptButtonSpace.setVisibility(GONE);
            signatureAddButton.setVisibility(GONE);
            signatureEncryptButton.setVisibility(GONE);
        } else {
            signatureEncryptButton.setVisibility(isExistingContainer ? VISIBLE : GONE);
            sendButton.setVisibility(isExistingContainer ? VISIBLE : GONE);
            signatureButtonSpace.setVisibility(isExistingContainer ? VISIBLE : GONE);
            encryptButtonSpace.setVisibility(isExistingContainer ? VISIBLE : GONE);
            if (UNSIGNABLE_CONTAINER_EXTENSIONS.contains(
                    Files.getFileExtension(containerFile.getName()).toLowerCase()) ||
                    FileSystem.isEmptyDataFileInContainer(navigator.activity(), containerFile)) {
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
        signatureEncryptButton.setContentDescription(getResources().getString(R.string.crypto_create_encrypt_button_description));

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
        try {
            adapter.setData(navigator.activity(), state.signatureAddSuccessMessageVisible(), isExistingContainer,
                    isNestedContainer, state.container(), nestedFile, isSivaConfirmed);
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Unable to set adapter data");
            if (e instanceof SSLHandshakeException) {
                ToastUtil.showError(navigator.activity(), ((SSLHandshakeException) e).getMessageId());
            }
            navigator.execute(Transaction.pop());
        }

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

        boolean roleAddEnabled = state.roleAddConfirmation();
        if (roleAddEnabled) {
            roleAddDialog.show();
        } else {
            roleAddDialog.dismiss();
        }

        SignatureAddResponse signatureAddResponse = state.signatureAddResponse();
        if (!roleAddEnabled) {
            signatureAddView.response(signatureAddResponse);
        }

        // should be in the MobileIdView in dialog
        mobileIdContainerView.setVisibility(
                signatureAddResponse instanceof MobileIdResponse
                        ? VISIBLE
                        : GONE);
        if (signatureAddResponse instanceof MobileIdResponse) {
            setSigningModalSize(mobileIdContainerView);
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
        tintCompoundDrawables(signatureEncryptButton);

        if (mobileIdContainerView.getVisibility() == VISIBLE) {
            mobileIdContainerView.setFocusedByDefault(true);
            mobileIdContainerView.setFocusable(true);
            mobileIdContainerView.setFocusableInTouchMode(true);

            if (mobileIdProgressBar.getProgress() == 0) {
                SignatureUpdateProgressBar.startProgressBar(mobileIdProgressBar);
            }

            if (!signingInfoDelegated) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_status_request_sent);
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_info);
                signingInfoDelegated = true;
            }

            if (!mobileIdChallengeView.getText().equals(EMPTY_CHALLENGE)) {
                AccessibilityUtils.setSingleCharactersContentDescription(mobileIdChallengeView, null);
                String mobileIdChallengeDescription = getResources().getString(R.string.mobile_id_challenge) +
                        AccessibilityUtils.getTextAsSingleCharacters(mobileIdChallengeView.getText().toString());
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, mobileIdChallengeDescription);
            }
        }

        smartIdContainerView.setVisibility(
                signatureAddResponse instanceof SmartIdResponse ? VISIBLE : GONE);
        if (smartIdContainerView.getVisibility() == VISIBLE) {
            smartIdContainerView.setFocusedByDefault(true);
            smartIdContainerView.setFocusable(true);
            smartIdContainerView.setFocusableInTouchMode(true);
            setSigningModalSize(smartIdContainerView);

            if (smartIdProgressBar.getProgress() == 0) {
                SignatureUpdateProgressBar.startProgressBar(smartIdProgressBar);
            }

            if (!signingInfoDelegated) {
                AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_mobile_id_status_request_sent);
                signingInfoDelegated = true;
            }

            if (signatureAddResponse instanceof SmartIdResponse) {
                SmartIdResponse smartIdResponse = (SmartIdResponse) signatureAddResponse;
                String selectDeviceText = getResources().getString(R.string.signature_update_smart_id_select_device);
                if (smartIdResponse.selectDevice() && !smartIdInfo.getText().equals(selectDeviceText)) {
                    smartIdInfo.setText(R.string.signature_update_smart_id_select_device);
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, smartIdInfo.getText());
                }
                String smartIdChallenge = smartIdResponse.challenge();
                if (smartIdChallenge != null) {
                    smartIdChallengeView.setText(smartIdChallenge);
                    smartIdInfo.setText(R.string.signature_update_smart_id_info);
                    String smartIdChallengeDescription = getResources().getString(R.string.smart_id_challenge) +
                            AccessibilityUtils.getTextAsSingleCharacters(smartIdChallenge);
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, smartIdChallengeDescription);
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_update_smart_id_info);
                } else {
                    smartIdChallengeView.setText(EMPTY_CHALLENGE);
                }
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
        mobileIdActivityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        smartIdActivityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
        sendButton.setEnabled(!activity);
        signatureAddButton.setEnabled(!activity);
        signatureEncryptButton.setEnabled(!activity);
        if (!activity) {
            SignatureUpdateProgressBar.stopProgressBar(mobileIdProgressBar);
            SignatureUpdateProgressBar.stopProgressBar(smartIdProgressBar);
        }
    }

    private void resetSignatureAddDialog() {
        SignatureUpdateProgressBar.stopProgressBar(mobileIdProgressBar);
        SignatureUpdateProgressBar.stopProgressBar(smartIdProgressBar);
        signatureAddView.reset(viewModel);
        isRoleViewShown = false;
    }

    private void resetRoleAddDialog() {
        roleAddView.reset(roleViewModel);
    }

    private Observable<InitialIntent> initialIntent() {
        return Observable.just(InitialIntent.create(isExistingContainer, containerFile,
                signatureAddVisible ? viewModel.signatureAddMethod() : null,
                signatureAddSuccessMessageVisible));
    }

    @SuppressWarnings("unchecked")
    private Observable<NameUpdateIntent> nameUpdateIntent() {
        return Observable.mergeArray(
                adapter.nameUpdateClicks().map(ignored -> NameUpdateIntent.show(containerFile)),
                cancels(nameUpdateDialog).map(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.container_name_change_cancelled);
                    return NameUpdateIntent.clear();
                }),
                nameUpdateDialog.updates().map(name -> NameUpdateIntent.update(containerFile, name)));
    }

    private Observable<DocumentsAddIntent> addDocumentsIntent() {
        return adapter.documentAddClicks()
                .map(ignored -> DocumentsAddIntent.create(containerFile))
                .mergeWith(documentsAddIntentSubject);
    }

    private Observable<DocumentViewIntent> documentViewIntent() {
        return Observable.mergeArray(adapter.documentClicks()
                .map(document -> DocumentViewIntent.confirmation(getContext(),
                        (nestedFile != null && isSivaConfirmed) ? nestedFile : containerFile, document)),
                        sivaConfirmationDialog.positiveButtonClicks()
                                .map(ignored -> DocumentViewIntent.open(
                                        (nestedFile != null && isSivaConfirmed) ? nestedFile : containerFile,
                                        sivaConfirmation)),
                        sivaConfirmationDialog.cancels()
                                .map(ignored -> DocumentViewIntent.cancel()));
    }

    private Observable<DocumentSaveIntent> documentSaveIntent() {
        return documentSaveIntentSubject;
    }

    private Observable<DocumentRemoveIntent> documentRemoveIntent() {
        return documentRemoveIntentSubject;
    }

    private Observable<SignatureViewIntent> signatureViewIntent() {
        return adapter.signatureClicks()
                .map(document -> SignatureViewIntent.create(containerFile, document));
    }

    private Observable<SignatureRemoveIntent> signatureRemoveIntent() {
        return signatureRemoveIntentSubject;
    }

    private Observable<SignatureRoleViewIntent> signatureRoleViewIntent() {
        return signatureRoleDetailsIntentSubject;
    }

    @SuppressWarnings("unchecked")
    private Observable<SignatureAddIntent> signatureAddIntent() {
        return Observable.mergeArray(
                clicks(signatureAddButton)
                        .doOnNext(ignored -> resetSignatureAddDialog())
                        .map(ignored -> {
                            int method = viewModel.signatureAddMethod();
                            return SignatureAddIntent.show(method, isExistingContainer, containerFile, false);
                        }),
                cancels(signatureAddDialog)
                        .doOnNext(ignored -> resetSignatureAddDialog())
                        .map(ignored -> SignatureAddIntent.clear()),
                signatureAddView.methodChanges().map(method -> {
                        viewModel.setSignatureAddMethod(method);
                        sendMethodSelectionAccessibilityEvent(method);
                        return SignatureAddIntent.show(method, isExistingContainer, containerFile, false);
                }),
                signatureAddDialog.positiveButtonClicks().map(ignored -> {
                    SignatureUpdateProgressBar.stopProgressBar(mobileIdProgressBar);
                    SignatureUpdateProgressBar.stopProgressBar(smartIdProgressBar);
                    boolean isRoleAskingEnabled = activity.getSettingsDataStore().getIsRoleAskingEnabled();
                    if (isRoleAskingEnabled) {
                        roleAddDialog.show();
                        return SignatureAddIntent.show(signatureAddView.method(),
                                isExistingContainer, containerFile, true);
                    }
                    return SignatureAddIntent.sign(signatureAddView.method(),
                            isExistingContainer, containerFile, signatureAddView.request(),
                            activity.getSettingsDataStore().getIsRoleAskingEnabled() ? roleAddView.request() : null);
                }),
                cancels(roleAddDialog)
                    .doOnNext(ignored -> resetSignatureAddDialog())
                    .map(ignored -> SignatureAddIntent.clear()),
                roleAddDialog.positiveButtonClicks().map(ignored -> {
                    isRoleViewShown = true;
                    roleViewModel.setRoleData(roleAddView.request());
                    roleAddDialog.dismiss();
                    return SignatureAddIntent.sign(signatureAddView.method(),
                            isExistingContainer, containerFile, signatureAddView.request(),
                            activity.getSettingsDataStore().getIsRoleAskingEnabled() ? roleAddView.request() : null);
                }),
                signatureAddIntentSubject
        );
    }

    private Observable<EncryptIntent> encryptIntent() {
        return clicks(signatureEncryptButton)
                .map(ignored -> EncryptIntent.create(containerFile));
    }

    private Observable<SendIntent> sendIntent() {
        return clicks(sendButton)
                .map(ignored -> SendIntent.create(containerFile));
    }

    private Observable<ContainerSaveIntent> saveIntent() {
        return adapter.saveContainerClicks().map(ignored -> ContainerSaveIntent.create(containerFile));
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
                documentSaveIntentSubject.onNext(DocumentSaveIntent
                        .create((nestedFile != null) ? nestedFile : containerFile, document))));
        disposables.add(adapter.signatureClicks().subscribe(signature ->
                signatureViewIntentSubject.onNext(SignatureViewIntent
                        .create(containerFile, signature))));
        disposables.add(adapter.documentRemoveClicks().subscribe(document ->
                documentRemoveIntentSubject.onNext(DocumentRemoveIntent
                        .showConfirmation(containerFile, dataFiles, document))));
        disposables.add(documentRemoveConfirmationDialog.positiveButtonClicks().subscribe(ignored ->
                documentRemoveIntentSubject.onNext(DocumentRemoveIntent
                        .remove(containerFile, dataFiles, documentRemoveConfirmation))));
        disposables.add(documentRemoveConfirmationDialog.cancels().subscribe(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.document_removal_cancelled);
                    documentRemoveIntentSubject.onNext(DocumentRemoveIntent.clear());
                }));
        disposables.add(adapter.signatureRemoveClicks().subscribe(signature ->
                signatureRemoveIntentSubject.onNext(SignatureRemoveIntent
                        .showConfirmation(containerFile, signature))));
        disposables.add(adapter.signatureRoleDetailsClicks().subscribe(signature ->
                signatureRoleDetailsIntentSubject.onNext(SignatureRoleViewIntent
                        .create(signature))));
        disposables.add(signatureRemoveConfirmationDialog.positiveButtonClicks()
                .subscribe(ignored -> signatureRemoveIntentSubject
                        .onNext(SignatureRemoveIntent
                                .remove(containerFile, signatureRemoveConfirmation))));
        disposables.add(signatureRemoveConfirmationDialog.cancels().subscribe(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(getContext(), TYPE_ANNOUNCEMENT, R.string.signature_removal_cancelled);
                    signatureRemoveIntentSubject.onNext(SignatureRemoveIntent.clear());
                }));
        disposables.add(clicks(mobileIdCancelButton).subscribe(ignored -> {
            MobileSignService.setIsCancelled(true);
            resetSignatureAddDialog();
            signatureAddIntentSubject.onNext(SignatureAddIntent.clear());
        }));
        disposables.add(clicks(smartIdCancelButton).subscribe(ignored -> {
            SmartSignService.setIsCancelled(true);
            resetSignatureAddDialog();
            signatureAddIntentSubject.onNext(SignatureAddIntent.clear());
        }));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        signatureAddDialog.dismiss();
        sivaConfirmationDialog.dismiss();
        roleAddDialog.dismiss();
        signatureRemoveConfirmationDialog.dismiss();
        documentRemoveConfirmationDialog.dismiss();
        errorDialog.setOnDismissListener(null);
        errorDialog.dismiss();
        nameUpdateDialog.dismiss();
        isTitleViewFocused = false;
        SignatureUpdateProgressBar.stopProgressBar(mobileIdProgressBar);
        SignatureUpdateProgressBar.stopProgressBar(smartIdProgressBar);
        isRoleViewShown = false;
        ContentView.removeInvisibleElementScrollListener(listView);
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel -> {
            parcel.writeBundle(signatureAddDialog.onSaveInstanceState());
            parcel.writeBundle(nameUpdateDialog.onSaveInstanceState());
            parcel.writeBundle(roleAddDialog.onSaveInstanceState());
        });
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel -> {
            signatureAddDialog.onRestoreInstanceState(
                    parcel.readBundle(getClass().getClassLoader()));
            nameUpdateDialog.onRestoreInstanceState(parcel.readBundle(getClass().getClassLoader()));
            roleAddDialog.onRestoreInstanceState(parcel.readBundle(getClass().getClassLoader()));
        }));
    }

    private void setupAccessibilityTabs() {
        if (sendButton.getVisibility() == VISIBLE && signatureAddButton.getVisibility() == VISIBLE &&
                signatureEncryptButton.getVisibility() == VISIBLE) {
            signatureAddButton.setContentDescription(getResources().getString(R.string.sign_send_content_description, 1, 3));
            signatureEncryptButton.setContentDescription(getResources().getString(R.string.decrypt_content_description, 2, 3));
            sendButton.setContentDescription(getResources().getString(R.string.decrypt_send_content_description, 3, 3));
        } else {
            sendButton.setContentDescription(getResources().getString(R.string.signature_update_send_button).toLowerCase());
            signatureAddButton.setContentDescription(getResources().getString(R.string.signature_update_signature_add_button).toLowerCase());
            signatureEncryptButton.setContentDescription(getResources().getString(R.string.crypto_create_encrypt_button).toLowerCase());
        }
    }
}
