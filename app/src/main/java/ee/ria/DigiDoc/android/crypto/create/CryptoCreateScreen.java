package ee.ria.DigiDoc.android.crypto.create;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;

import org.openeid.cdoc4j.exception.RecipientMissingException;

import java.io.File;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.BundleUtils.getFile;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putFile;
import static ee.ria.DigiDoc.android.utils.Predicates.duplicates;
import static ee.ria.DigiDoc.android.utils.TintUtils.tintCompoundDrawables;
import static ee.ria.DigiDoc.android.utils.rxbinding.app.RxDialog.cancels;

public final class CryptoCreateScreen extends Controller implements Screen,
        MviView<Intent, ViewState> {

    private static final String KEY_CONTAINER_FILE = "containerFile";

    public static CryptoCreateScreen create() {
        return new CryptoCreateScreen(Bundle.EMPTY);
    }

    public static CryptoCreateScreen open(File containerFile) {
        Bundle args = new Bundle();
        putFile(args, KEY_CONTAINER_FILE, containerFile);
        return new CryptoCreateScreen(args);
    }

    @Nullable private final File containerFile;

    private final Subject<Boolean> idCardTokenAvailableSubject = PublishSubject.create();

    private final ViewDisposables disposables = new ViewDisposables();
    private CryptoCreateViewModel viewModel;

    private Toolbar toolbarView;
    private CryptoCreateAdapter adapter;
    private View activityOverlayView;
    private View activityIndicatorView;
    private TextView encryptButton;
    private TextView decryptButton;
    private TextView sendButton;
    private View buttonSpaceView;
    private DecryptDialog decryptDialog;
    private AlertDialog errorDialog;

    private String name;
    private ImmutableList<File> dataFiles = ImmutableList.of();
    private ImmutableList<Certificate> recipients = ImmutableList.of();
    private IdCardDataResponse decryptionIdCardDataResponse;
    @Nullable private Throwable dataFilesAddError;
    @Nullable private Throwable encryptError;
    @Nullable private Throwable decryptError;

    @SuppressWarnings("WeakerAccess")
    public CryptoCreateScreen(Bundle args) {
        super(args);
        containerFile = args.containsKey(KEY_CONTAINER_FILE)
                ? getFile(args, KEY_CONTAINER_FILE)
                : null;
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create(containerFile));
    }

    private Observable<Intent.UpButtonClickIntent> upButtonClickIntent() {
        return navigationClicks(toolbarView).map(ignored -> Intent.UpButtonClickIntent.create());
    }

    private Observable<Intent.DataFilesAddIntent> dataFilesAddIntent() {
        return adapter.dataFilesAddButtonClicks()
                .map(ignored -> Intent.DataFilesAddIntent.start(dataFiles));
    }

    private Observable<Intent.DataFileRemoveIntent> dataFileRemoveIntent() {
        return adapter.dataFileRemoveClicks()
                .map(dataFile -> Intent.DataFileRemoveIntent.create(dataFiles, dataFile));
    }

    private Observable<Intent.DataFileViewIntent> dataFileViewIntent() {
        return adapter.dataFileClicks()
                .map(Intent.DataFileViewIntent::create);
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
        return Observable.mergeArray(initialIntent(), upButtonClickIntent(), dataFilesAddIntent(),
                dataFileRemoveIntent(), dataFileViewIntent(), recipientsAddButtonClickIntent(),
                recipientRemoveIntent(), encryptIntent(), decryptionIntent(), decryptIntent(),
                errorIntents());
    }

    @Override
    public void render(ViewState state) {
        tintCompoundDrawables(encryptButton);
        tintCompoundDrawables(decryptButton);
        tintCompoundDrawables(sendButton);

        name = state.name();
        dataFiles = state.dataFiles();
        recipients = state.recipients();
        dataFilesAddError = state.dataFilesAddError();
        encryptError = state.encryptError();
        decryptError = state.decryptError();

        setActivity(state.dataFilesAddState().equals(State.ACTIVE) ||
                state.encryptState().equals(State.ACTIVE));

        adapter.dataForContainer(name, dataFiles, state.dataFilesViewEnabled(),
                state.dataFilesAddEnabled(), state.dataFilesRemoveEnabled(), recipients,
                state.recipientsAddEnabled(), state.recipientsRemoveEnabled(),
                state.encryptSuccessMessageVisible());

        encryptButton.setVisibility(state.encryptButtonVisible() ? View.VISIBLE : View.GONE);
        decryptButton.setVisibility(state.decryptButtonVisible() ? View.VISIBLE : View.GONE);
        sendButton.setVisibility(state.sendButtonVisible() ? View.VISIBLE : View.GONE);
        buttonSpaceView.setVisibility(state.sendButtonVisible() &&
                (state.encryptButtonVisible() || state.decryptButtonVisible())
                ? View.VISIBLE : View.GONE);

        decryptionIdCardDataResponse = state.decryptionIdCardDataResponse();
        if (decryptionIdCardDataResponse != null) {
            decryptDialog.show();
            decryptDialog.idCardDataResponse(decryptionIdCardDataResponse, state.decryptState(),
                    state.decryptIdCardData(), state.decryptError());
        } else {
            decryptDialog.dismiss();
        }
        idCardTokenAvailableSubject.onNext(decryptionIdCardDataResponse != null &&
                decryptionIdCardDataResponse.token() != null);

        if (decryptError != null) {
            errorDialog.setMessage(errorDialog.getContext().getString(
                    R.string.crypto_create_error));
            errorDialog.show();
        } else if (encryptError != null) {
            if (encryptError instanceof RecipientMissingException) {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_encrypt_error_no_recipients));
            } else {
                errorDialog.setMessage(errorDialog.getContext().getString(
                        R.string.crypto_create_error));
            }
            errorDialog.show();
        } else if (dataFilesAddError != null) {
            errorDialog.setMessage(errorDialog.getContext().getString(
                    R.string.crypto_create_data_files_add_error_exists));
            errorDialog.show();
        } else {
            errorDialog.dismiss();
        }
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : View.GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : View.GONE);
        encryptButton.setEnabled(!activity);
        decryptButton.setEnabled(!activity);
        sendButton.setEnabled(!activity);
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
        toolbarView = view.findViewById(R.id.toolbar);
        RecyclerView listView = view.findViewById(R.id.cryptoCreateList);
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);
        encryptButton = view.findViewById(R.id.cryptoCreateEncryptButton);
        decryptButton = view.findViewById(R.id.cryptoCreateDecryptButton);
        sendButton = view.findViewById(R.id.cryptoCreateSendButton);
        buttonSpaceView = view.findViewById(R.id.cryptoCreateButtonSpace);
        decryptDialog = new DecryptDialog(inflater.getContext());
        errorDialog = new AlertDialog.Builder(inflater.getContext())
                .setMessage(R.string.crypto_create_error)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                .create();

        listView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());

        tintCompoundDrawables(encryptButton);
        tintCompoundDrawables(decryptButton);
        tintCompoundDrawables(sendButton);

        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        decryptDialog.dismiss();
        errorDialog.dismiss();
        disposables.detach();
        super.onDestroyView(view);
    }
}
