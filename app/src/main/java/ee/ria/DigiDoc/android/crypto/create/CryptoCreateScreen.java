package ee.ria.DigiDoc.android.crypto.create;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.Certificate;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.mvi.State;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import io.reactivex.Observable;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;
import static com.jakewharton.rxbinding2.view.RxView.clicks;
import static ee.ria.DigiDoc.android.utils.BundleUtils.getFile;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putFile;

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

    private final ViewDisposables disposables = new ViewDisposables();
    private CryptoCreateViewModel viewModel;

    private Toolbar toolbarView;
    private CryptoCreateAdapter adapter;
    private View activityOverlayView;
    private View activityIndicatorView;
    private View encryptButton;

    @Nullable private File containerFile;
    private ImmutableList<File> dataFiles = ImmutableList.of();
    private ImmutableList<Certificate> recipients = ImmutableList.of();

    @SuppressWarnings("WeakerAccess")
    public CryptoCreateScreen(Bundle args) {
        super(args);
        if (args.containsKey(KEY_CONTAINER_FILE)) {
            containerFile = getFile(args, KEY_CONTAINER_FILE);
        }
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
                .map(ignored -> Intent.EncryptIntent.create(containerFile, dataFiles, recipients));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), upButtonClickIntent(), dataFilesAddIntent(),
                dataFileRemoveIntent(), dataFileViewIntent(), recipientsAddButtonClickIntent(),
                recipientRemoveIntent(), encryptIntent());
    }

    @Override
    public void render(ViewState state) {
        if (state.containerFile() != null) {
            containerFile = state.containerFile();
        }
        dataFiles = state.dataFiles();
        recipients = state.recipients();

        setActivity(state.dataFilesAddState().equals(State.ACTIVE) ||
                state.dataFileRemoveState().equals(State.ACTIVE) ||
                state.encryptState().equals(State.ACTIVE));
        adapter.dataForContainer(containerFile, dataFiles, recipients,
                state.encryptSuccessMessageVisible());
    }

    private void setActivity(boolean activity) {
        activityOverlayView.setVisibility(activity ? View.VISIBLE : View.GONE);
        activityIndicatorView.setVisibility(activity ? View.VISIBLE : View.GONE);
        encryptButton.setEnabled(!activity);
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

        listView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        listView.setAdapter(adapter = new CryptoCreateAdapter());

        disposables.attach();
        disposables.add(viewModel.viewStates().subscribe(this::render));
        viewModel.process(intents());

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        disposables.detach();
        super.onDestroyView(view);
    }
}
