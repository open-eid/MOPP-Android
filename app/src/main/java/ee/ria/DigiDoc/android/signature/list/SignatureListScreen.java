package ee.ria.DigiDoc.android.signature.list;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import com.bluelinelabs.conductor.Controller;
import com.google.common.io.Files;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.mvi.MviView;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.widget.ConfirmationDialog;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.core.Observable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

public final class SignatureListScreen extends Controller implements Screen,
        MviView<Intent, ViewState> {

    public static SignatureListScreen create() {
        return new SignatureListScreen();
    }

    private final ViewDisposables disposables = new ViewDisposables();
    private SignatureListViewModel viewModel;

    private ConfirmationDialog removeConfirmationDialog;
    private ConfirmationDialog sivaConfirmationDialog;
    private Toolbar toolbarView;
    private RecyclerView listView;
    private SignatureListAdapter adapter;
    private View emptyView;
    private View activityIndicatorView;
    private View activityOverlayView;

    @Nullable private File removeConfirmationContainerFile;
    @Nullable private File sivaConfirmationContainerFile;

    @SuppressWarnings("WeakerAccess")
    public SignatureListScreen() {
    }

    private Observable<Intent.InitialIntent> initialIntent() {
        return Observable.just(Intent.InitialIntent.create());
    }

    private Observable<Intent.UpButtonIntent> upButtonIntent() {
        return navigationClicks(toolbarView)
                .map(ignored -> Intent.UpButtonIntent.create());
    }

    private Observable<Intent.ContainerOpenIntent> containerOpenIntent() {
        return Observable.merge(adapter.itemClicks()
                .flatMap(file -> Intent.ContainerOpenIntent.confirmation(file, getApplicationContext())),
                sivaConfirmationDialog.positiveButtonClicks()
                    .map(ignored -> Intent.ContainerOpenIntent.open(sivaConfirmationContainerFile, true)),
                sivaConfirmationDialog.cancels()
                        .map(ignored -> {
                            if (sivaConfirmationContainerFile != null &&
                                    SignedContainer.isContainer(sivaConfirmationContainerFile) &&
                                    SignedContainer.isAsicsFile(sivaConfirmationContainerFile.getName())) {
                                SignedContainer signedContainer = SignedContainer.open(sivaConfirmationContainerFile);
                                if (signedContainer.dataFiles().size() == 1 &&
                                        Files.getFileExtension(signedContainer.dataFiles().get(0).name()).equalsIgnoreCase("ddoc")) {
                                    return Intent.ContainerOpenIntent.open(sivaConfirmationContainerFile, false);
                                }
                            }
                            return Intent.ContainerOpenIntent.cancel();
                        }));
    }

    private Observable<Intent.ContainerRemoveIntent> containerRemoveIntent() {
        return Observable.merge(
                adapter.removeButtonClicks()
                        .map(Intent.ContainerRemoveIntent::confirmation),
                removeConfirmationDialog.positiveButtonClicks()
                        .map(ignored -> Intent.ContainerRemoveIntent
                                .remove(removeConfirmationContainerFile)),
                removeConfirmationDialog.cancels()
                        .map(ignored -> {
                            if (getApplicationContext() != null) {
                                AccessibilityUtils.sendAccessibilityEvent(getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.document_removal_cancelled);
                            }
                            return Intent.ContainerRemoveIntent.cancel();
                        }));
    }

    private Observable<Intent.RefreshIntent> refreshIntent() {
        return Observable.just(Intent.RefreshIntent.create());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<Intent> intents() {
        return Observable.mergeArray(initialIntent(), upButtonIntent(), containerOpenIntent(),
                containerRemoveIntent(), refreshIntent());
    }

    @Override
    public void render(ViewState state) {
        removeConfirmationContainerFile = state.removeConfirmationContainerFile();
        sivaConfirmationContainerFile = state.sivaConfirmationContainerFile();

        setActivity(state.indicateActivity()
                && (state.containerLoadProgress() || state.containerRemoveProgress()));

        adapter.setData(state.containerFiles());
        if (removeConfirmationContainerFile != null) {
            removeConfirmationDialog.show();
        } else {
            removeConfirmationDialog.dismiss();
        }

        if (sivaConfirmationContainerFile != null) {
            sivaConfirmationDialog.show();
        } else {
            sivaConfirmationDialog.dismiss();
            sivaConfirmationDialog.cancel();
        }

        setEmpty(!state.containerLoadProgress() && state.containerFiles().size() == 0);
    }

    private void setActivity(boolean activity) {
        activityIndicatorView.setVisibility(activity ? VISIBLE : GONE);
        activityOverlayView.setVisibility(activity ? VISIBLE : GONE);
    }

    private void setEmpty(boolean empty) {
        listView.setVisibility(empty ? GONE : VISIBLE);
        emptyView.setVisibility(empty ? VISIBLE : GONE);
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        viewModel = Application.component(context).navigator()
                .viewModel(getInstanceId(), SignatureListViewModel.class);
    }

    @Override
    protected void onContextUnavailable() {
        viewModel = null;
        super.onContextUnavailable();
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedViewState) {
        removeConfirmationDialog = new ConfirmationDialog(container.getContext(),
                R.string.signature_list_remove_confirmation_message, R.id.documentRemovalDialog);
        sivaConfirmationDialog = new ConfirmationDialog(Activity.getContext().get(),
                R.string.siva_send_message_dialog, R.id.sivaConfirmationDialog);
        View view = inflater.inflate(R.layout.signature_list_screen, container, false);
        AccessibilityUtils.setViewAccessibilityPaneTitle(view, R.string.signature_list_title);

        toolbarView = view.findViewById(R.id.toolbar);
        toolbarView.setTitle(R.string.signature_list_title);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);
        listView = view.findViewById(R.id.signatureList);
        listView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        listView.setAdapter(adapter = new SignatureListAdapter());
        emptyView = view.findViewById(R.id.listEmpty);
        activityIndicatorView = view.findViewById(R.id.activityIndicator);
        activityOverlayView = view.findViewById(R.id.activityOverlay);
        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        removeConfirmationDialog.dismiss();
        sivaConfirmationDialog.dismiss();
        super.onDestroyView(view);
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
