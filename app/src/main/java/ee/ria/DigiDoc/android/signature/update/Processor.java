package ee.ria.DigiDoc.android.signature.update;

import static android.app.Activity.RESULT_OK;
import static com.google.common.io.Files.getFileExtension;
import static ee.ria.DigiDoc.android.Constants.SAVE_FILE;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSaveIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSendIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.crypto.create.CryptoCreateScreen;
import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.android.signature.detail.SignatureDetailScreen;
import ee.ria.DigiDoc.android.utils.IntentUtils;
import ee.ria.DigiDoc.android.utils.SivaUtil;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.files.FileAlreadyExistsException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.mobileid.service.MobileSignService;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.service.SmartSignService;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.ContainerLoadAction,
                                        Result.ContainerLoadResult> containerLoad;

    private final ObservableTransformer<Intent.NameUpdateIntent, Result.NameUpdateResult>
            nameUpdate;

    private final ObservableTransformer<Action.DocumentsAddAction,
                                        Result.DocumentsAddResult> documentsAdd;

    private final ObservableTransformer<Intent.DocumentViewIntent,
                                        Result.DocumentViewResult> documentView;

    private final ObservableTransformer<Intent.DocumentSaveIntent,
            Result> documentSave;

    private final ObservableTransformer<Action.DocumentRemoveAction,
                                        Result.DocumentRemoveResult> documentRemove;

    private final ObservableTransformer<Action.SignatureRemoveAction,
                                        Result.SignatureRemoveResult> signatureRemove;

    private final ObservableTransformer<Action.SignatureViewAction,
            Result.SignatureViewResult> signatureView;

    private final ObservableTransformer<Action.SignatureAddAction,
                                        Result.SignatureAddResult> signatureAdd;

    private final ObservableTransformer<Action.SendAction, Result.SendResult> send;

    private android.content.Intent intent;

    @Inject Processor(SignatureContainerDataSource signatureContainerDataSource,
                      SignatureAddSource signatureAddSource, Application application,
                      Navigator navigator,
                      FileSystem fileSystem) {
        containerLoad = upstream -> upstream.switchMap(action ->
                signatureContainerDataSource.get(action.containerFile())
                        .toObservable()
                        .switchMap(container -> {
                            if (action.signatureAddSuccessMessageVisible()) {
                                return Observable.timer(3, TimeUnit.SECONDS)
                                        .map(ignored ->
                                                Result.ContainerLoadResult.success(container, null,
                                                        false))
                                        .startWithItem(Result.ContainerLoadResult.success(container,
                                                null, true));
                            } else {
                                final Observable<Result.ContainerLoadResult> just = Observable
                                        .just(Result.ContainerLoadResult.success(container,
                                                action.signatureAddMethod(),
                                                action.signatureAddSuccessMessageVisible()));
                                if (!action.isExistingContainer()) {
                                    announceAccessibilityFilesAddedEvent(application.getApplicationContext(),
                                            container.dataFiles().size());
                                }
                                return just;
                            }
                        })
                        .onErrorReturn(Result.ContainerLoadResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.ContainerLoadResult.progress()));

        nameUpdate = upstream -> upstream.switchMap(action -> {
            File containerFile = action.containerFile();
            String name = containerFile != null ? assignName(action, containerFile) : null;

            if (containerFile == null) {
                return Observable.just(Result.NameUpdateResult.hide());
            } else if (name == null) {
                return Observable.just(
                        Result.NameUpdateResult.name(containerFile),
                        Result.NameUpdateResult.show(containerFile));
            } else if (name.equals(containerFile.getName())) {
                return Observable.just(Result.NameUpdateResult.hide());
            } else if (name.isEmpty()) {
                return Observable.just(Result.NameUpdateResult
                        .failure(containerFile, new IOException()));
            } else {
                return Observable
                        .fromCallable(() -> {
                            File newFile = new File(containerFile.getParentFile(), name);
                            if (!newFile.getParentFile().equals(containerFile.getParentFile())) {
                                throw new IOException("Can't jump directories");
                            } else if (newFile.createNewFile()) {

                                checkContainerName(newFile);

                                boolean isFileDeleted = newFile.delete();
                                if (!isFileDeleted || !containerFile.renameTo(newFile)) {
                                    throw new IOException();
                                }

                                AccessibilityUtils.sendAccessibilityEvent(
                                        application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.container_name_changed);


                                return newFile;
                            } else {
                                checkContainerName(newFile);

                                throw new FileAlreadyExistsException(newFile);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(newFile -> {
                            navigator.execute(Transaction.replace(SignatureUpdateScreen
                                    .create(true, false, newFile, false, false, null, true)));
                            return Result.NameUpdateResult.progress(newFile);
                        })
                        .onErrorReturn(throwable ->
                                Result.NameUpdateResult.failure(containerFile, throwable))
                        .startWithItem(Result.NameUpdateResult.progress(containerFile));
            }
        });

        documentsAdd = upstream -> upstream
                .switchMap(action -> {
                    if (action.containerFile() == null) {
                        return Observable.just(Result.DocumentsAddResult.clear());
                    } else {
                        navigator.execute(action.transaction());
                        return navigator.activityResults()
                                .filter(activityResult ->
                                        activityResult.requestCode()
                                                == action.transaction().requestCode())
                                .switchMap(activityResult -> {
                                    android.content.Intent data = activityResult.data();
                                    if (activityResult.resultCode() == RESULT_OK && data != null) {
                                        ImmutableList<FileStream> validFiles = FileSystem.getFilesWithValidSize(
                                                parseGetContentIntent(navigator.activity(), application.getContentResolver(),
                                                        data, fileSystem.getExternallyOpenedFilesDir()));
                                        ToastUtil.handleEmptyFileError(validFiles, application, navigator.activity());
                                        ImmutableList<FileStream> filesNotInContainer = getFilesNotInContainer(navigator.activity(), validFiles, action.containerFile());
                                        if (filesNotInContainer.isEmpty()) {
                                            throw new FileAlreadyExistsException(navigator.activity()
                                                    .getResources()
                                                    .getString(R.string.signature_update_documents_add_error_exists));
                                        }
                                        announceAccessibilityFilesAddedEvent(application.getApplicationContext(), filesNotInContainer.size());
                                        return signatureContainerDataSource
                                                .addDocuments(action.containerFile(), filesNotInContainer)
                                                .toObservable()
                                                .map(Result.DocumentsAddResult::success)
                                                .onErrorReturn(Result.DocumentsAddResult::failure)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .startWithItem(Result.DocumentsAddResult.adding());
                                    } else {
                                        return Observable.just(Result.DocumentsAddResult.clear());
                                    }
                                })
                                .onErrorReturn(Result.DocumentsAddResult::failure);
                    }
                });

        documentView = upstream -> upstream.switchMap(action -> {
            if (action.containerFile() == null) {
                return Observable.just(Result.DocumentViewResult.idle());
            } else if (action.confirmation()) {
                return Observable
                        .just(Result.DocumentViewResult.confirmation(action.document()));
            } else {
                File containerFile = action.containerFile();
                return signatureContainerDataSource
                        .getDocumentFile(containerFile, action.document())
                        .toObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(documentFile -> {
                            Transaction transaction;
                            String containerFileExtension = getFileExtension(containerFile.getName()).toLowerCase(Locale.US);
                            String documentFileExtension = getFileExtension(documentFile.getName()).toLowerCase(Locale.US);
                            boolean isPdfInSignedPdfContainer = false;
                            if (action.document() != null) {
                                isPdfInSignedPdfContainer = containerFileExtension.equals("pdf") &&
                                        (SivaUtil.isSivaConfirmationNeeded(containerFile, action.document()) &&
                                                documentFileExtension.equals("pdf"));
                            }
                            if (!isPdfInSignedPdfContainer && SignedContainer.isContainer(navigator.activity(), documentFile)) {
                                transaction = Transaction.push(SignatureUpdateScreen
                                        .create(true, true, documentFile, false, false, null, true));
                            } else if (CryptoContainer.isContainerFileName(documentFile.getName())) {
                                transaction = Transaction.push(CryptoCreateScreen.open(documentFile));
                            } else {
                                transaction = Transaction.activity(IntentUtils
                                        .createViewIntent(application, documentFile,
                                                SignedContainer.mimeType(documentFile)), null);
                            }
                            navigator.execute(transaction);
                            return Result.DocumentViewResult.idle();
                        })
                        .onErrorReturn(throwable -> {
                            if (throwable instanceof NoInternetConnectionException) {
                                ToastUtil.showError(navigator.activity(),R.string.no_internet_connection);
                            } else {
                                ToastUtil.showError(navigator.activity(), R.string.signature_update_container_load_error);
                            }
                            return Result.DocumentViewResult.idle();
                        })
                        .startWithItem(Result.DocumentViewResult.activity());
            }
        });

        documentSave = upstream -> upstream.switchMap(action -> {
            navigator.execute(Transaction.activityForResult(SAVE_FILE,
                    createSaveIntent(action.document()), null));
            return navigator.activityResults()
                    .filter(activityResult ->
                            activityResult.requestCode() == SAVE_FILE)
                    .switchMap(activityResult -> signatureContainerDataSource
                            .getDocumentFile(action.containerFile(), action.document())
                            .toObservable()
                            .map(documentFile -> {
                                if (activityResult.resultCode() == RESULT_OK) {
                                    try (
                                            InputStream inputStream = new FileInputStream(documentFile);
                                            OutputStream outputStream = application.getContentResolver().openOutputStream(activityResult.data().getData())
                                    ) {
                                        ByteStreams.copy(inputStream, outputStream);
                                    }
                                    ToastUtil.showError(navigator.activity(), R.string.file_saved);
                                }
                                return Result.DocumentSaveResult.idle();
                            })
                            .onErrorReturn(ignored -> Result.DocumentSaveResult.idle())
                            .startWithItem(Result.DocumentSaveResult.activity()));
        });

        documentRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null || action.document() == null) {
                return Observable.just(Result.DocumentRemoveResult.clear());
            } else if (action.showConfirmation()) {
                return Observable.just(Result.DocumentRemoveResult.confirmation(action.document()));
            } else {
                if (action.documents().size() == 1) {
                    boolean isFileDeleted = action.containerFile().delete();
                    if (isFileDeleted) {
                        Timber.log(Log.DEBUG, "File %s deleted", action.containerFile().getName());
                    }
                    navigator.execute(Transaction.pop());
                    return Observable.just(Result.DocumentRemoveResult.success(null));
                } else {
                    return signatureContainerDataSource
                            .removeDocument(action.containerFile(), action.document())
                            .toObservable()
                            .map(container -> {
                                AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.file_removed);
                                return Result.DocumentRemoveResult.success(container);
                            })
                            .onErrorReturn(Result.DocumentRemoveResult::failure)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWithItem(Result.DocumentRemoveResult.progress());
                }
            }
        });

        signatureRemove = upstream -> upstream.flatMap(action -> {
            if (action.containerFile() == null || action.signature() == null) {
                return Observable.just(Result.SignatureRemoveResult.clear());
            } else if (action.showConfirmation()) {
                return Observable.just(Result.SignatureRemoveResult
                        .confirmation(action.signature()));
            } else {
                return signatureContainerDataSource
                        .removeSignature(action.containerFile(), action.signature())
                        .toObservable()
                        .map(container -> {
                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.signature_removed);
                            return Result.SignatureRemoveResult.success(container);
                        })
                        .onErrorReturn(Result.SignatureRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.SignatureRemoveResult.progress());
            }
        });

        signatureView = upstream -> upstream.flatMap(action -> {
            File containerFile = action.containerFile();
            return signatureContainerDataSource
                    .get(containerFile)
                    .toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(signedContainerFile -> {
                        Transaction transaction;
                        transaction = Transaction.push(SignatureDetailScreen
                                .create(action.signature(), signedContainerFile));
                        navigator.execute(transaction);
                        return Result.SignatureViewResult.idle();
                    })
                    .onErrorReturn(ignored -> Result.SignatureViewResult.idle())
                    .startWithItem(Result.SignatureViewResult.activity());
        });


        signatureAdd = upstream -> upstream.switchMap(action -> {
            Integer method = action.method();
            Boolean existingContainer = action.existingContainer();
            File containerFile = action.containerFile();
            SignatureAddRequest request = action.request();
            boolean isCancelled = action.isCancelled();
            if (method != null) {
                intent = getSigningIntent(navigator, method);
            }
            if (method == null || isCancelled) {
                if (intent != null) {
                    Handler handler = new Handler(navigator.activity().getMainLooper());
                    handler.post(() -> stopSigningService(navigator, intent));
                }
                return Observable.just(Result.SignatureAddResult.clear());
            } else if (request == null && existingContainer != null && containerFile != null) {
                if (SignedContainer.isLegacyContainer(containerFile)) {
                    return signatureContainerDataSource
                            .addContainer(navigator.activity(), ImmutableList.of(FileStream.create(containerFile)), true)
                            .toObservable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(containerAdd ->
                                    navigator.execute(Transaction.push(SignatureUpdateScreen.create(
                                            containerAdd.isExistingContainer(), false,
                                            containerAdd.containerFile(), true, false, null, true))))
                            .map(containerAdd -> Result.SignatureAddResult.clear())
                            .onErrorReturn(Result.SignatureAddResult::failure)
                            .startWithItem(Result.SignatureAddResult.activity());
                } else {
                    return signatureAddSource.show(method);
                }
            } else if (existingContainer != null && containerFile != null) {
                return signatureAddSource.sign(containerFile, request, navigator, intent)
                        .switchMap(response -> {
                            if (response.container() != null) {
                                return Observable.fromCallable(() -> {
                                    navigator.execute(Transaction.replace(SignatureUpdateScreen
                                            .create(true, false, containerFile, false, true, null, true)));
                                    return Result.SignatureAddResult.method(method, response);
                                });
                            } else {
                                return Observable
                                        .just(Result.SignatureAddResult.method(method, response));
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn(Result.SignatureAddResult::failure)
                        .startWithItem(Result.SignatureAddResult.activity(method));
            } else {
                throw new IllegalArgumentException("Can't handle action " + action);
            }
        });

        send = upstream -> upstream
                .doOnNext(action ->
                        navigator.execute(Transaction.activity(
                                createSendIntent(application, action.containerFile()), null)))
                .map(action -> Result.SendResult.success())
                .onErrorReturn(Result.SendResult::failure);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.ContainerLoadAction.class).compose(containerLoad),
                shared.ofType(Intent.NameUpdateIntent.class).compose(nameUpdate),
                shared.ofType(Action.DocumentsAddAction.class).compose(documentsAdd),
                shared.ofType(Intent.DocumentViewIntent.class).compose(documentView),
                shared.ofType(Intent.DocumentSaveIntent.class).compose(documentSave),
                shared.ofType(Action.DocumentRemoveAction.class).compose(documentRemove),
                shared.ofType(Action.SignatureRemoveAction.class).compose(signatureRemove),
                shared.ofType(Action.SignatureViewAction.class).compose(signatureView),
                shared.ofType(Action.SignatureAddAction.class).compose(signatureAdd),
                shared.ofType(Action.SendAction.class).compose(send)));
    }

    private void checkContainerName(File newContainerFileName) throws IOException {
        if (newContainerFileName.getName().startsWith(".")) {
            throw new IOException();
        }
    }

    private String addContainerExtension(File oldContainerFileName, String newName) {
        String[] oldContainerNameParts = oldContainerFileName.getName().split("\\.");
        String oldContainerNamePart = oldContainerNameParts[oldContainerNameParts.length - 1];

        return newName.concat(".").concat(oldContainerNamePart);
    }

    private String assignName(Intent.NameUpdateIntent action, File containerFile) {
        String name = FileUtil.sanitizeString(action.name(), "");
        if (name != null && !name.isEmpty()) {
            return addContainerExtension(containerFile, name);
        }

        return name;
    }

    private ImmutableList<FileStream> getFilesNotInContainer(Context context, ImmutableList<FileStream> validFiles, File container) throws Exception {
        List<FileStream> filesNotInContainer = new ArrayList<>();
        List<String> containerDataFileNames = new ArrayList<>();
        if (!validFiles.isEmpty() && SignedContainer.isContainer(context, container)) {
            SignedContainer signedContainer = SignedContainer.open(container);
            ImmutableList<DataFile> dataFiles = signedContainer.dataFiles();
            for (DataFile dataFile : dataFiles) {
                containerDataFileNames.add(FileUtil.normalizeString(dataFile.name()));
            }

            for (FileStream validFile : validFiles) {
                if (!containerDataFileNames.contains(FileUtil.normalizeString(validFile.displayName()))) {
                    filesNotInContainer.add(validFile);
                }
            }

            return ImmutableList.copyOf(filesNotInContainer);
        }

        return validFiles;
    }

    private void announceAccessibilityFilesAddedEvent(Context context, int addedDataList) {
        if (addedDataList > 1) {
            AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.files_added);
        } else {
            AccessibilityUtils.sendAccessibilityEvent(context, AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.file_added);
        }
    }

    private android.content.Intent getSigningIntent(Navigator navigator, Integer method) {
        if (method != null) {
            if (method == R.id.signatureUpdateSignatureAddMethodMobileId) {
                return new android.content.Intent(navigator.activity(), MobileSignService.class);
            } else if (method == R.id.signatureUpdateSignatureAddMethodSmartId) {
                return new android.content.Intent(navigator.activity(), SmartSignService.class);
            }
        }
        return null;
    }

    private void stopSigningService(Navigator navigator, android.content.Intent intent) {
        navigator.activity().stopService(intent);
    }
}
