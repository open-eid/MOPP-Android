package ee.ria.DigiDoc.android.crypto.create;

import static android.app.Activity.RESULT_OK;
import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static ee.ria.DigiDoc.android.Constants.RC_CRYPTO_CREATE_DATA_FILE_ADD;
import static ee.ria.DigiDoc.android.Constants.RC_CRYPTO_CREATE_INITIAL;
import static ee.ria.DigiDoc.android.Constants.SAVE_FILE;
import static ee.ria.DigiDoc.android.utils.Immutables.with;
import static ee.ria.DigiDoc.android.utils.Immutables.without;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSaveIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSendIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;
import static ee.ria.DigiDoc.crypto.CryptoContainer.createContainerFileName;
import static ee.ria.DigiDoc.crypto.CryptoContainer.isContainerFileName;
import static ee.ria.DigiDoc.sign.SignedContainer.mimeType;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateScreen;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.ActivityUtil;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.crypto.RecipientRepository;
import ee.ria.DigiDoc.idcard.Token;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

final class Processor implements ObservableTransformer<Intent, Result> {

    private final ContentResolver contentResolver;
    private final FileSystem fileSystem;

    private final ObservableTransformer<Intent.InitialIntent, Result.InitialResult> initial;

    private final ObservableTransformer<Intent.UpButtonClickIntent, Result> upButtonClick;

    private final ObservableTransformer<Intent.NameUpdateIntent, Result.NameUpdateResult>
            nameUpdate;

    private final ObservableTransformer<Intent.DataFilesAddIntent,
                                        Result.DataFilesAddResult> dataFilesAdd;

    private final ObservableTransformer<Intent.DataFileRemoveIntent,
                                        Result.DataFileRemoveResult> dataFileRemove;

    private final ObservableTransformer<Intent.DataFileSaveIntent, Result> dataFileSave;

    private final ObservableTransformer<Intent.DataFileViewIntent, Result> dataFileView;

    private final ObservableTransformer<Intent.RecipientsAddButtonClickIntent,
                                        Result.RecipientsAddButtonClickResult>
            recipientsAddButtonClick;

    private final ObservableTransformer<Intent.RecipientsScreenUpButtonClickIntent,
                                        Result> recipientsScreenUpButtonClick;

    private final ObservableTransformer<Intent.RecipientsScreenDoneButtonClickIntent,
            Result> recipientsScreenDoneButtonClick;

    private final ObservableTransformer<Intent.RecipientsSearchIntent,
            Result.RecipientsSearchResult> recipientsSearch;

    private final ObservableTransformer<Intent.RecipientAddIntent,
            Result.RecipientAddResult> recipientAdd;

    private final ObservableTransformer<Intent.RecipientRemoveIntent,
                                        Result.RecipientRemoveResult> recipientRemove;

    private final ObservableTransformer<Intent.EncryptIntent, Result.EncryptResult> encrypt;

    private final ObservableTransformer<Intent.DecryptionIntent,
                                        Result.DecryptionResult> decryption;

    private final ObservableTransformer<Intent.DecryptIntent, Result.DecryptResult> decrypt;

    private final ObservableTransformer<Intent.SendIntent, Result> send;

    @Inject Processor(Navigator navigator, RecipientRepository recipientRepository,
                      ContentResolver contentResolver, FileSystem fileSystem,
                      Application application, IdCardService idCardService) {
        this.contentResolver = contentResolver;
        this.fileSystem = fileSystem;

        initial = upstream -> upstream.switchMap(intent -> {
            File containerFile = intent.containerFile();
            android.content.Intent androidIntent = intent.intent();
            if (containerFile != null) {
                return Observable
                        .fromCallable(() -> CryptoContainer.open(containerFile))
                        .map(Result.InitialResult::success)
                        .onErrorReturn(Result.InitialResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.InitialResult.activity());
            } else if (androidIntent != null) {
                return parseIntent(androidIntent, application, fileSystem.getExternallyOpenedFilesDir());
            } else {
                navigator.execute(Transaction.activityForResult(RC_CRYPTO_CREATE_INITIAL,
                        createGetContentIntent(), null));
                return navigator.activityResults()
                        .filter(activityResult ->
                                activityResult.requestCode() == RC_CRYPTO_CREATE_INITIAL)
                        .switchMap(activityResult -> {
                            android.content.Intent data = activityResult.data();
                            if (activityResult.resultCode() == RESULT_OK && data != null) {
                                return parseIntent(data, application, fileSystem.getExternallyOpenedFilesDir())
                                        .onErrorReturn(throwable -> {
                                            if (throwable instanceof EmptyFileException) {
                                                ToastUtil.showEmptyFileError(application);
                                            }
                                            navigator.execute(Transaction.pop());
                                            return Result.InitialResult.failure(throwable);
                                        });
                            } else {
                                navigator.execute(Transaction.pop());
                                return Observable.just(Result.InitialResult.clear());
                            }
                        });
            }
        });

        upButtonClick = upstream -> upstream.switchMap(intent -> {
            if (ActivityUtil.isExternalFileOpened(navigator.activity())) {
                ActivityUtil.restartActivity(application.getApplicationContext(), navigator.activity());
            } else {
                navigator.execute(Transaction.pop());
            }
            return Observable.empty();
        });

        nameUpdate = upstream -> upstream.switchMap(action -> {
            String name = FileUtil.sanitizeString(action.name(), "");
            String newName = FileUtil.sanitizeString(action.newName(), "");
            if (newName != null) {
                return Observable
                        .fromCallable(() -> newName)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map((renameTo) -> {
                            Result.NameUpdateResult result = Result.NameUpdateResult.progress(assignName(name, renameTo));
                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.container_name_changed);
                            return result;
                        })
                        .onErrorReturn(throwable -> Result.NameUpdateResult.failure(newName, throwable))
                        .startWithItem(Result.NameUpdateResult.progress(name));
            } else if (name != null) {
                return Observable.just(Result.NameUpdateResult.show(name));
            } else {
                return Observable.just(Result.NameUpdateResult
                        .failure(name, new IOException()));
            }
        });

        dataFilesAdd = upstream -> upstream.switchMap(intent -> {
            ImmutableList<File> dataFiles = intent.dataFiles();
            if (dataFiles == null) {
                return Observable.just(Result.DataFilesAddResult.clear());
            }

            navigator.execute(Transaction.activityForResult(RC_CRYPTO_CREATE_DATA_FILE_ADD,
                    createGetContentIntent(), null));
            return navigator.activityResults()
                    .filter(activityResult ->
                            activityResult.requestCode() == RC_CRYPTO_CREATE_DATA_FILE_ADD)
                    .switchMap(activityResult -> {
                        android.content.Intent data = activityResult.data();
                        if (activityResult.resultCode() == RESULT_OK && data != null) {
                            return Observable
                                    .fromCallable(() -> {
                                        ImmutableList<FileStream> fileStreams =
                                                parseGetContentIntent(contentResolver, data, fileSystem.getExternallyOpenedFilesDir());
                                        ImmutableList.Builder<File> builder =
                                                ImmutableList.<File>builder().addAll(dataFiles);
                                        ImmutableList<FileStream> validFiles = FileSystem.getFilesWithValidSize(fileStreams);
                                        ToastUtil.handleEmptyFileError(validFiles, application);
                                        for (FileStream fileStream : validFiles) {
                                            File dataFile = fileSystem.cache(fileStream);
                                            if (dataFiles.contains(dataFile)) {
                                                throw new IllegalArgumentException(
                                                        Activity.getContext().get().getString(
                                                                R.string.crypto_create_data_files_add_error_exists));
                                            }
                                            builder.add(dataFile);
                                        }
                                        if (fileStreams.size() > 1) {
                                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.files_added);
                                        } else {
                                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.file_added);
                                        }
                                        return builder.build();
                                    })
                                    .map(Result.DataFilesAddResult::success)
                                    .onErrorReturn(throwable -> {
                                        Timber.log(Log.DEBUG, throwable, "No valid files in list");
                                        return Result.DataFilesAddResult.failure(throwable);
                                    })
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .startWithItem(Result.DataFilesAddResult.activity());
                        } else {
                            return Observable.just(Result.DataFilesAddResult.clear());
                        }
                    });
        });

        dataFileRemove = upstream -> upstream.switchMap(intent -> {
            if (!intent.showConfirmation() && intent.dataFile() == null) {
                return Observable.just(Result.DataFileRemoveResult.clear(intent.dataFiles()));
            }
            if (intent.showConfirmation()) {
                return Observable.just(
                        Result.DataFileRemoveResult.confirmation(intent.dataFiles(), intent.dataFile())
                );
            } else {
                return Observable
                        .fromCallable(() -> {
                            try {
                                //noinspection ResultOfMethodCallIgnored
                                intent.dataFile().delete();
                            } catch (Exception e) {
                                // ignore because it's a cache file and is deleted anyway
                            }
                            ImmutableList<File> remainingDataFiles = without(intent.dataFiles(), intent.dataFile());
                            if (remainingDataFiles.isEmpty()) {
                                if (intent.containerFile() != null) {
                                    boolean isFileDeleted = intent.containerFile().delete();
                                    if (isFileDeleted) {
                                        Timber.log(Log.DEBUG, "File %s deleted", intent.containerFile().getName());
                                    }
                                }
                                navigator.execute(Transaction.pop());
                            }
                            return Result.DataFileRemoveResult.success(remainingDataFiles);
                        })
                        .doFinally(() -> AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.file_removed))
                        .observeOn(AndroidSchedulers.mainThread());
            }
        });

        dataFileSave = upstream -> upstream.switchMap(action -> {
            navigator.execute(Transaction.activityForResult(SAVE_FILE,
                    createSaveIntent(action.dataFile(), application.getContentResolver()), null));
            return navigator.activityResults()
                    .filter(activityResult ->
                            activityResult.requestCode() == SAVE_FILE)
                    .switchMap(activityResult -> {
                        if (activityResult.resultCode() == RESULT_OK) {
                            try (
                                    InputStream inputStream = new FileInputStream(action.dataFile());
                                    OutputStream outputStream = application.getContentResolver().openOutputStream(activityResult.data().getData())
                            ) {
                                ByteStreams.copy(inputStream, outputStream);
                            }
                            Toast.makeText(application, Activity.getContext().get().getString(R.string.file_saved),
                                    Toast.LENGTH_LONG).show();
                        }
                        return Observable.empty();
                    });
        });

        dataFileView = upstream -> upstream.switchMap(intent -> {
            if (intent.dataFile() == null) {
                return Observable.just(Result.OpenDataFileResult.clear());
            } else if (intent.confirmation()) {
                return Observable
                        .just(Result.OpenDataFileResult.confirmation(intent.dataFile()));
            } else {
                return Observable
                        .fromCallable(() -> {
                            File file = intent.dataFile();
                            if (CryptoContainer.isContainerFileName(file.getName())) {
                                return Transaction.push(CryptoCreateScreen.open(file));
                            } else if (SignedContainer.isContainer(file)) {
                                return Transaction.push(
                                        SignatureUpdateScreen.create(true, true, file, false, false, null));
                            } else {
                                return Transaction.activity(
                                        createViewIntent(application, file, mimeType(file)), null);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap(transaction -> {
                            navigator.execute(transaction);
                            return Observable.just(Result.OpenDataFileResult.success());
                        });
            }
        });

        recipientsAddButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.push(
                    CryptoRecipientsScreen.create(intent.cryptoCreateScreenId())));
            return Observable.just(Result.RecipientsAddButtonClickResult.create());
        });

        recipientsScreenUpButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.pop());
            if (application.getApplicationContext() != null) {
                AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.recipient_addition_cancelled);
            }
            return Observable.empty();
        });

        recipientsScreenDoneButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.pop());
            if (application.getApplicationContext() != null) {
                AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.recipients_added);
            }
            return Observable.empty();
        });

        recipientsSearch = upstream -> upstream.switchMap(intent -> {
            if (intent.query() == null) {
                return Observable.just(Result.RecipientsSearchResult.clear());
            } else {
                return Observable
                        .fromCallable(() -> recipientRepository.find(intent.query()))
                        .map(searchResult -> {
                            Context context = application.getApplicationContext();
                            if (searchResult.size() > 1) {
                                String resultString = context.getResources().getString(R.string.recipients_found);
                                AccessibilityUtils.sendAccessibilityEvent(
                                        context, TYPE_ANNOUNCEMENT, searchResult.size() + " " + resultString);
                            } else if (searchResult.size() == 1) {
                                AccessibilityUtils.sendAccessibilityEvent(
                                        context, TYPE_ANNOUNCEMENT, R.string.recipient_found);
                            } else {
                                AccessibilityUtils.sendAccessibilityEvent(
                                        context, TYPE_ANNOUNCEMENT, R.string.crypto_recipients_search_result_empty);
                            }
                            return Result.RecipientsSearchResult.success(searchResult);
                        })
                        .onErrorReturn(Result.RecipientsSearchResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.RecipientsSearchResult.activity());
            }
        });

        recipientAdd = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientAddResult.create(with(intent.recipients(), intent.recipient(), false)))
                    .doFinally(() ->
                        AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.recipient_added)));

        recipientRemove = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientRemoveResult.create(without(intent.recipients(), intent.recipient())))
                    .doFinally(() ->
                        AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.recipient_removed)));

        encrypt = upstream -> upstream.switchMap(intent -> {
            String name = intent.name();
            ImmutableList<File> dataFiles = intent.dataFiles();
            ImmutableList<Certificate> recipients = intent.recipients();
            if (name != null && dataFiles != null && recipients != null) {
                return Single
                        .fromCallable(() -> {
                            File containerFile = fileSystem.generateSignatureContainerFile(name);
                            try {
                                File file = CryptoContainer.encrypt(dataFiles, recipients, containerFile).file();
                                if (dataFiles.size() > 1) {
                                    AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.files_encrypted);
                                } else {
                                    AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.crypto_create_encrypt_success_message);
                                }
                                return file;
                            } catch (Exception e) {
                                boolean isFileDeleted = containerFile.delete();
                                if (isFileDeleted) {
                                    Timber.log(Log.DEBUG, "File %s deleted", containerFile.getName());
                                }
                                throw e;
                            }
                        })
                        .flatMapObservable(file ->
                                Observable
                                        .timer(3, TimeUnit.SECONDS)
                                        .map(ignored -> Result.EncryptResult.success(file))
                                        .startWithItem(Result.EncryptResult.successMessage(file)))
                        .onErrorReturn(Result.EncryptResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.EncryptResult.activity());
            } else {
                return Observable.just(Result.EncryptResult.clear());
            }
        });

        decryption = upstream -> upstream.switchMap(intent -> {
            if (intent.visible()) {
                return idCardService.data()
                        .map(Result.DecryptionResult::show)
                        .startWithItem(Result.DecryptionResult.show(IdCardDataResponse.initial()));
            } else {
                return Observable.just(Result.DecryptionResult.hide());
            }
        });

        decrypt = upstream -> upstream.switchMap(intent -> {
            DecryptRequest request = intent.request();
            if (request != null) {
                Token token = request.token();
                return idCardService
                        .decrypt(token, request.containerFile(), request.pin1())
                        .doOnSuccess(ignored ->
                                AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(),
                                        AccessibilityEvent.TYPE_ANNOUNCEMENT, R.string.document_decrypted)
                        )
                        .flatMapObservable(dataFiles ->
                                Observable
                                        .timer(3, TimeUnit.SECONDS)
                                        .map(ignored -> Result.DecryptResult.success(dataFiles))
                                        .startWithItem(Result.DecryptResult.successMessage(dataFiles)))
                        .onErrorReturn(throwable -> {
                            IdCardDataResponse idCardDataResponse = null;
                            if (throwable instanceof Pin1InvalidException) {
                                try {
                                    idCardDataResponse = IdCardDataResponse
                                            .success(IdCardService.data(token), token);
                                } catch (Exception ignored) {}
                            }
                            return Result.DecryptResult.failure(throwable, idCardDataResponse);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWithItem(Result.DecryptResult.activity());
            } else {
                return Observable.just(Result.DecryptResult.clear(), Result.DecryptResult.idle());
            }
        });

        send = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction
                    .activity(createSendIntent(application, intent.containerFile()), null));
            return Observable.empty();
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.UpButtonClickIntent.class).compose(upButtonClick),
                shared.ofType(Intent.NameUpdateIntent.class).compose(nameUpdate),
                shared.ofType(Intent.DataFilesAddIntent.class).compose(dataFilesAdd),
                shared.ofType(Intent.DataFileRemoveIntent.class).compose(dataFileRemove),
                shared.ofType(Intent.DataFileSaveIntent.class).compose(dataFileSave),
                shared.ofType(Intent.DataFileViewIntent.class).compose(dataFileView),
                shared.ofType(Intent.RecipientsAddButtonClickIntent.class)
                        .compose(recipientsAddButtonClick),
                shared.ofType(Intent.RecipientsScreenUpButtonClickIntent.class)
                        .compose(recipientsScreenUpButtonClick),
                shared.ofType(Intent.RecipientsScreenDoneButtonClickIntent.class)
                        .compose(recipientsScreenDoneButtonClick),
                shared.ofType(Intent.RecipientsSearchIntent.class).compose(recipientsSearch),
                shared.ofType(Intent.RecipientAddIntent.class).compose(recipientAdd),
                shared.ofType(Intent.RecipientRemoveIntent.class).compose(recipientRemove),
                shared.ofType(Intent.EncryptIntent.class).compose(encrypt),
                shared.ofType(Intent.DecryptionIntent.class).compose(decryption),
                shared.ofType(Intent.DecryptIntent.class).compose(decrypt),
                shared.ofType(Intent.SendIntent.class).compose(send)));
    }

    private Observable<Result.InitialResult> parseIntent(android.content.Intent intent, Application application, File externallyOpenedFileDir) {
        return Observable
                .fromCallable(() -> {
                    ImmutableList<FileStream> validFiles = FileSystem.getFilesWithValidSize(
                            parseGetContentIntent(contentResolver, intent, externallyOpenedFileDir));
                    ToastUtil.handleEmptyFileError(validFiles, application);
                    if (validFiles.size() == 1
                            && isContainerFileName(validFiles.get(0).displayName())) {
                        File file = fileSystem.addSignatureContainer(validFiles.get(0));
                        return Result.InitialResult.success(CryptoContainer.open(file));
                    } else {
                        ImmutableList.Builder<File> builder = ImmutableList.builder();
                        for (FileStream fileStream : validFiles) {
                            builder.add(fileSystem.cache(fileStream));
                        }
                        ImmutableList<File> dataFiles = builder.build();
                        File file = fileSystem.generateSignatureContainerFile(
                                createContainerFileName(dataFiles.get(0).getName()));
                        if (dataFiles.size() > 1) {
                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.files_added);
                        } else {
                            AccessibilityUtils.sendAccessibilityEvent(application.getApplicationContext(), TYPE_ANNOUNCEMENT, R.string.file_added);
                        }
                        return Result.InitialResult.success(file, dataFiles);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .startWithItem(Result.InitialResult.activity());
    }

    private String assignName(String oldName, String newName) {
        if (oldName != null && !oldName.isEmpty()) {
            String[] oldNameParts = oldName.split("\\.");
            String oldNameExtension = oldNameParts[oldNameParts.length - 1];
            return newName.concat(".").concat(oldNameExtension);

        }
        return newName;
    }
}
