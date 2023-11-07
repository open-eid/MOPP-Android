package ee.ria.DigiDoc.android.crypto.create;

import static android.view.View.GONE;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.utils.BundleUtils.getFile;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putBoolean;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putFile;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.model.idcard.IdCardData;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.container.NameUpdateDialog;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.ContentView;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;
import ee.ria.DigiDoc.android.utils.widget.NotificationDialog;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.crypto.RecipientsEmptyException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public final class CryptoCreateScreen extends Controller implements Screen, ContentView,
        MviView<Intent, ViewState> {

    private static final String KEY_CONTAINER_FILE = "containerFile";
    private static final String KEY_INTENT = "intent";
    private static final String KEY_IS_FROM_SIGNATURE_VIEW = "isFromSignatureView";

    public static CryptoCreateScreen create() {
        return new CryptoCreateScreen(Bundle.EMPTY);
    }

    public static CryptoCreateScreen open(File containerFile, boolean isFromSignatureView) {
        Bundle args = new Bundle();
        putFile(args, KEY_CONTAINER_FILE, containerFile);
        putBoolean(args, KEY_IS_FROM_SIGNATURE_VIEW, isFromSignatureView);
        return new CryptoCreateScreen(args);
    }

    public static CryptoCreateScreen open(android.content.Intent intent) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_INTENT, intent);
        return new CryptoCreateScreen(args);
    }

    @Nullable private File containerFile;
    @Nullable private final android.content.Intent intent;
    private final boolean isFromSignatureView;

    private final Subject<Boolean> idCardTokenAvailableSubject = PublishSubject.create();

    private final ViewDisposables disposables = new ViewDisposables();
    private CryptoCreateViewModel viewModel;

    private View view;
    private Toolbar toolbarView;
    private NameUpdateDialog nameUpdateDialog;
    private ConfirmationDialog sivaConfirmationDialog;
    private ConfirmationDialog fileRemoveConfirmationDialog;
    private CryptoCreateAdapter adapter;
    private View activityOverlayView;
    private View activityIndicatorView;
    private Button encryptButton;
    private RecyclerView listView;
    private TextView decryptButton;
    private TextView signButton;
    private TextView sendButton;
    private View cryptoButtonSpaceView;
    private View signatureButtonSpaceView;
    private DecryptDialog decryptDialog;
    private ErrorDialog errorDialog;

    private String name;
    private ImmutableList<File> dataFiles = ImmutableList.of();
    private ImmutableList<Certificate> recipients = ImmutableList.of();
    private IdCardDataResponse decryptionIdCardDataResponse;
    @Nullable private Throwable dataFilesAddError;
    @Nullable private Throwable encryptError;
    @Nullable private Throwable decryptError;
    @Nullable private File dataFileRemoveConfirmation;
    @Nullable private File sivaConfirmation;

    @SuppressWarnings("WeakerAccess")
    public CryptoCreateScreen(Bundle args) {
        super(args);
        containerFile = args.containsKey(KEY_CONTAINER_FILE)
                ? getFile(args, KEY_CONTAINER_FILE)
                : null;
        intent = getIntent(args);
        isFromSignatureView = args.getBoolean(KEY_IS_FROM_SIGNATURE_VIEW);
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(containerFile, intent, isFromSignatureView));
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.NameUpdateIntent> nameUpdateIntent() {
        return Observable.mergeArray(
                adapter.nameUpdateClicks()
                        .map(ignored -> Intent.NameUpdateIntent.show(name)),
                cancels(nameUpdateDialog).map(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(view.getContext(), TYPE_ANNOUNCEMENT, R.string.container_name_change_cancelled);
                    return Intent.NameUpdateIntent.clear();
                }),
                nameUpdateDialog.updates()
                        .map(newName -> Intent.NameUpdateIntent.update(name, newName))
        );
    }

    private Observable<Intent.UpButtonClickIntent> upButtonClickIntent() {
        return navigationClicks(toolbarView).map(ignored -> Intent.UpButtonClickIntent.create());
    }

    private Observable<Intent.DataFilesAddIntent> dataFilesAddIntent() {
        return adapter.dataFilesAddButtonClicks()
                .map(ignored -> Intent.DataFilesAddIntent.start(dataFiles));
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.DataFileRemoveIntent> dataFileRemoveIntent() {
        return Observable.mergeArray(
                adapter.dataFileRemoveClicks()
                        .map(dataFile -> Intent.DataFileRemoveIntent.showConfirmation(dataFiles, dataFile)),
                fileRemoveConfirmationDialog.cancels().map(ignored -> {
                    AccessibilityUtils.sendAccessibilityEvent(view.getContext(), TYPE_ANNOUNCEMENT, R.string.file_removal_cancelled);
                    return Intent.DataFileRemoveIntent.clear(dataFiles);
                }),
                fileRemoveConfirmationDialog.positiveButtonClicks()
                        .map(ignored -> Intent.DataFileRemoveIntent.remove(containerFile, dataFiles, dataFileRemoveConfirmation))
        );
    }

    private Observable<Intent.DataFileSaveIntent> dataFileSaveIntent() {
        return adapter.dataFileSaveClicks()
                .map(Intent.DataFileSaveIntent::create);
    }

    @SuppressWarnings("unchecked")
    private Observable<Intent.DataFileViewIntent> dataFileViewIntent() {
        return Observable.mergeArray(adapter.dataFileClicks()
                        .flatMap(file -> Intent.DataFileViewIntent.confirmation(file, getApplicationContext())),
                sivaConfirmationDialog.positiveButtonClicks()
                        .map(ignored -> Intent.DataFileViewIntent.open(sivaConfirmation)),
                sivaConfirmationDialog.cancels()
                        .map(ignored -> Intent.DataFileViewIntent.cancel()));
    }

    private Observable<Intent.RecipientsAddButtonClickIntent> recipientsAddButtonClickIntent() {
        return adapter.recipientsAddButtonClicks()
                .map(ignored -> Intent.RecipientsAddButtonClickIntent.create(getInstanceId()));
    }

    private Observable<Intent.RecipientRemoveIntent> recipientRemoveIntent() {
        return adapter.recipientRemoveClicks()
                .map(recipient -> Intent.RecipientRemoveIntent.create(recipients, recipient));
    }

    private Observable<Intent.EncryptIntent> encryptIntent() {
        return clicks(encryptButton)
                .map(ignored -> Intent.EncryptIntent.start(name, dataFiles, recipients));
    }

    private Observable<Intent.DecryptionIntent> decryptionIntent() {
        return Observable.merge(
                clicks(decryptButton).map(ignored -> Intent.DecryptionIntent.show()),
                cancels(decryptDialog).map(ignored -> Intent.DecryptionIntent.hide()));
    }

    private Observable<Intent.DecryptIntent> decryptIntent() {
        return Observable.merge(
                decryptDialog.positiveButtonClicks()
                        .filter(ignored ->
                                decryptionIdCardDataResponse != null &&
                                        decryptionIdCardDataResponse.token() != null)
                        .map(pin1 ->
                                Intent.DecryptIntent.start(DecryptRequest.create(
                                        decryptionIdCardDataResponse.token(), containerFile,
                                        pin1))),
                idCardTokenAvailableSubject
                        .filter(duplicates())
                        .filter(available -> available)
                        .map(ignored -> Intent.DecryptIntent.cancel()));
    }

    private Observable<Intent.SendIntent> sendIntent() {
        return clicks(sendButton).map(ignored -> Intent.SendIntent.create(containerFile));
    }

    private Observable<Intent.ContainerSaveIntent> containerSaveIntent() {
        return adapter.saveContainerClicks()
                .map(ignored -> Intent.ContainerSaveIntent.create(containerFile));
    }

    private Observable<Intent.SignIntent> signIntent() {
        return clicks(signButton).map(ignored -> Intent.SignIntent.create(containerFile));
    }

    private Observable<Intent> errorIntents() {
        return cancels(errorDialog)
                .map(ignored -> {
                    if (dataFilesAddError != null) {
                        return Intent.DataFilesAddIntent.clear();
                    } else if (encryptError != null) {
                        return Intent.EncryptIntent.clear();
                    } else if (decryptError != null) {
                        return Intent.DecryptIntent.cancel();
                    }
                    throw new IllegalStateException("No errors");
                });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), nameUpdateIntent(), upButtonClickIntent(), dataFilesAddIntent(),
                dataFileRemoveIntent(), dataFileSaveIntent(), dataFileViewIntent(), recipientsAddButtonClickIntent(),
                recipientRemoveIntent(), encryptIntent(), decryptionIntent(), decryptIntent(),
                containerSaveIntent(), signIntent(), sendIntent(), errorIntents());
    }

    @Override
    public void render(ViewState state) {
        if (state.containerFile() != null) {
            containerFile = state.containerFile();
        }

        name = state.newName() != null ? FileUtil.sanitizeString(state.newName(), "") : FileUtil.sanitizeString(state.name(), "");
        dataFiles = state.dataFiles();
        recipients = state.recipients();
        dataFilesAddError = state.dataFilesAddError();
        encryptError = state.encryptError();
        decryptError = state.decryptError();

        setActivity(state.dataFilesAddState().equals(State.ACTIVE) ||
                state.encryptState().equals(State.ACTIVE));

        nameUpdateDialog.render(state.nameUpdateShowing(), FileUtil.sanitizeString(state.name(), ""), state.nameUpdateError());

        int titleResId = state.encryptButtonVisible() ? R.string.crypto_create_title_encrypt
                : R.string.crypto_create_title_decrypt;
        toolbarView.setTitle(titleResId);
        toolbarView.setNavigationIcon(R.drawable.ic_clear);
        toolbarView.setNavigationContentDescription(R.string.close);

        AccessibilityUtils.setViewAccessibilityPaneTitle(view, titleResId);

        adapter.dataForContainer(name, containerFile, dataFiles, state.dataFilesViewEnabled(),
                state.dataFilesAddEnabled(), state.dataFilesRemoveEnabled(), recipients,
                state.recipientsAddEnabled(), state.recipientsRemoveEnabled(),
                state.encryptSuccessMessageVisible(), state.decryptSuccessMessageVisible(), listView);

        if (state.encryptSuccessMessageVisible()) {
            showSuccessNotification();
        }

        dataFileRemoveConfirmation = state.dataFileRemoveConfirmation();
        if (dataFileRemoveConfirmation != null) {
            if (dataFiles.size() == 1) {
                fileRemoveConfirmationDialog.setMessage(getResources().getString(R.string.crypto_create_remove_last_data_file_confirmation_message));
            } else {
                fileRemoveConfirmationDialog.setMessage(getResources().getString(R.string.crypto_create_remove_data_file_confirmation_message));
            }
            fileRemoveConfirmationDialog.show();
        } else {
            fileRemoveConfirmationDialog.dismiss();
        }

        sivaConfirmation = state.sivaDataFile();
        if (sivaConfirmation != null) {
            sivaConfirmationDialog.show();
        } else {
            sivaConfirmationDialog.dismiss();
        }

        encryptButton.setVisibility(state.encryptButtonVisible() ? View.VISIBLE : View.GONE);
        decryptButton.setVisibility(state.decryptButtonVisible() ? View.VISIBLE : View.GONE);
        signButton.setVisibility(state.decryptButtonVisible() && !isFromSignatureView ? View.VISIBLE : View.GONE);
        sendButton.setVisibility(state.sendButtonVisible() ? View.VISIBLE : View.GONE);
        cryptoButtonSpaceView.setVisibility(state.sendButtonVisible() &&
                (state.encryptButtonVisible() || state.decryptButtonVisible())
                ? View.VISIBLE : View.GONE);
        signatureButtonSpaceView.setVisibility(
                state.sendButtonVisible() &&
                        state.decryptButtonVisible() ? View.VISIBLE : View.GONE);

        decryptionIdCardDataResponse = state.decryptionIdCardDataResponse();
        boolean decryptionPin1Locked = false;
        if (decryptionIdCardDataResponse != null) {
            IdCardData data = decryptionIdCardDataResponse.data();
            if (data != null && data.pin1RetryCount() == 0) {
                decryptionPin1Locked = true;
            }
            decryptDialog.show();
            decryptDialog.idCardDataResponse(decryptionIdCardDataResponse, state.decryptState(), decryptError);
        } else {
            decryptionPin1Locked = true;
            decryptDialog.dismiss();
        }
        idCardTokenAvailableSubject.onNext(decryptionIdCardDataResponse != null &&
                decryptionIdCardDataResponse.token() != null);

        if (decryptError != null) {
            if (decryptError instanceof Pin1InvalidException && decryptionPin1Locked) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_decrypt_pin1_locked));
                errorDialog.show();
            } else if (!(decryptError instanceof Pin1InvalidException)) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_decrypt_error));
                errorDialog.show();
            }
        } else if (encryptError != null) {
            if (encryptError instanceof RecipientsEmptyException) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_encrypt_error_no_recipients));
            } else {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_error));
            }
            errorDialog.show();
        } else if (dataFilesAddError != null) {
            if (dataFilesAddError instanceof EmptyFileException) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.empty_file_error));
            } else {
                if (dataFilesAddError.getMessage() != null && dataFilesAddError.getMessage().contains("connection_failure")) {
                    errorDialog.setMessage(errorDialog.getContext().getString(
                            R.string.no_internet_connection));
                } else {
                    errorDialog.setMessage(errorDialog.getContext().getString(
                            R.string.crypto_create_data_files_add_error_exists));
                }
            }
            errorDialog.show();
        } else {
            errorDialog.dismiss();
        }
    }

    private void showSuccessNotification() {
        Boolean showNotification = ((Activity) view.getContext()).getSettingsDataStore().getShowSuccessNotification();
        if (showNotification) {
            NotificationDialog successNotificationDialog = new NotificationDialog((Activity) view.getContext());
            if (AccessibilityUtils.isAccessibilityEnabled()) {
                new Handler(Looper.getMainLooper()).postDelayed(successNotificationDialog::show, 2000);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(successNotificationDialog::show, 1000);
            }
        }
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : GONE);
        encryptButton.setEnabled(!activity);
        decryptButton.setEnabled(!activity);
        signButton.setEnabled(!activity);
        sendButton.setEnabled(!activity);
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        viewModel = ApplicationApp.component(context).navigator()
                .viewModel(getInstanceId(), CryptoCreateViewModel.class);
    }

    @Override
    protected void onContextUnavailable() {
        viewModel = null;
        super.onContextUnavailable();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        view = inflater.inflate(R.layout.crypto_create_screen, container, false);
        toolbarView = view.findViewById(R.id.toolbar);
        nameUpdateDialog = new NameUpdateDialog(container.getContext());
        fileRemoveConfirmationDialog = new ConfirmationDialog(container.getContext(),
                R.string.crypto_create_remove_data_file_confirmation_message, R.id.documentRemovalDialog);
        sivaConfirmationDialog = new ConfirmationDialog(Activity.getContext().get(),
                R.string.siva_send_message_dialog, R.id.sivaConfirmationDialog);
        listView = view.findViewById(R.id.cryptoCreateList);
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);
        encryptButton = view.findViewById(R.id.cryptoCreateEncryptButton);
        decryptButton = view.findViewById(R.id.cryptoCreateDecryptButton);
        signButton = view.findViewById(R.id.cryptoCreateSignatureButton);
        sendButton = view.findViewById(R.id.cryptoCreateSendButton);
        sendButton.setContentDescription(getResources().getString(R.string.share_container));
        cryptoButtonSpaceView = view.findViewById(R.id.cryptoCreateCryptoButtonSpace);
        signatureButtonSpaceView = view.findViewById(R.id.cryptoCreateSignatureButtonSpace);
        decryptDialog = new DecryptDialog(inflater.getContext());

        this.errorDialog = new ErrorDialog(inflater.getContext());
        this.errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), (dialog, which) -> dialog.cancel());
        this.errorDialog.setMessage(getResources().getString(R.string.crypto_create_error));

        listView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());

        tintCompoundDrawables(encryptButton, true);
        tintCompoundDrawables(decryptButton, true);
        tintCompoundDrawables(sendButton, true);
        tintCompoundDrawables(signButton, true);

        decryptButton.setContentDescription(getResources().getString(R.string.decrypt_content_description, 1, 2));
        sendButton.setContentDescription(getResources().getString(R.string.decrypt_send_content_description, 2, 2));

        decryptButton.setContentDescription(getResources().getString(R.string.decrypt_content_description, 1, 3));
        signButton.setContentDescription(getResources().getString(R.string.sign_send_content_description, 2, 3));
        sendButton.setContentDescription(getResources().getString(R.string.decrypt_send_content_description, 3, 3));

        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());

        ContentView.addInvisibleElement(getApplicationContext(), view);

        View lastElementView = view.findViewById(R.id.lastInvisibleElement);

        if (lastElementView != null) {
            ContentView.removeInvisibleElementScrollListener(listView);
            ContentView.addInvisibleElementScrollListener(listView, lastElementView);
        }

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        decryptDialog.dismiss();
        errorDialog.dismiss();
        sivaConfirmationDialog.dismiss();
        ContentView.removeInvisibleElementScrollListener(listView);
        disposables.detach();
        super.onDestroyView(view);
    }

    private android.content.Intent getIntent(Bundle bundle) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return bundle.getParcelable(KEY_INTENT, android.content.Intent.class);
        } else {
            return bundle.getParcelable(KEY_INTENT);
        }
    }
}
