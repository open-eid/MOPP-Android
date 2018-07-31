package ee.ria.DigiDoc.android.crypto.create;

import android.app.Application;
import android.content.ContentResolver;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.model.idcard.IdCardDataResponse;
import ee.ria.DigiDoc.android.model.idcard.IdCardService;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.Certificate;
import ee.ria.DigiDoc.crypto.CryptoContainer;
import ee.ria.DigiDoc.crypto.Pin1InvalidException;
import ee.ria.DigiDoc.crypto.RecipientRepository;
import ee.ria.DigiDoc.idcard.Token;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static ee.ria.DigiDoc.android.Constants.RC_CRYPTO_CREATE_DATA_FILE_ADD;
import static ee.ria.DigiDoc.android.Constants.RC_CRYPTO_CREATE_INITIAL;
import static ee.ria.DigiDoc.android.utils.Immutables.with;
import static ee.ria.DigiDoc.android.utils.Immutables.without;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createSendIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;
import static ee.ria.DigiDoc.crypto.CryptoContainer.createContainerFileName;
import static ee.ria.DigiDoc.crypto.CryptoContainer.isContainerFileName;
import static ee.ria.DigiDoc.sign.SignedContainer.mimeType;

/**
 * Error when opening crypto container: show error message and close on dialog cancel?
 * Do not create file when encrypt is not successful
 * Check for all cases when application is terminated
 */
final class Processor implements ObservableTransformer<Intent, Result> {

    private final ContentResolver contentResolver;
    private final FileSystem fileSystem;

    private final ObservableTransformer<Intent.InitialIntent, Result.InitialResult> initial;

    private final ObservableTransformer<Intent.UpButtonClickIntent, Result> upButtonClick;

    private final ObservableTransformer<Intent.DataFilesAddIntent,
                                        Result.DataFilesAddResult> dataFilesAdd;

    private final ObservableTransformer<Intent.DataFileRemoveIntent,
                                        Result.DataFileRemoveResult> dataFileRemove;

    private final ObservableTransformer<Intent.DataFileViewIntent, Result> dataFileView;

    private final ObservableTransformer<Intent.RecipientsAddButtonClickIntent,
                                        Result.RecipientsAddButtonClickResult>
            recipientsAddButtonClick;

    private final ObservableTransformer<Intent.RecipientsScreenUpButtonClickIntent,
                                        Result> recipientsScreenUpButtonClick;

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
                        .startWith(Result.InitialResult.activity());
            } else if (androidIntent != null) {
                return parseIntent(androidIntent);
            } else {
                navigator.execute(Transaction.activityForResult(RC_CRYPTO_CREATE_INITIAL,
                        createGetContentIntent(), null));
                return navigator.activityResults()
                        .filter(activityResult ->
                                activityResult.requestCode() == RC_CRYPTO_CREATE_INITIAL)
                        .switchMap(activityResult -> {
                            android.content.Intent data = activityResult.data();
                            if (activityResult.resultCode() == RESULT_OK && data != null) {
                                return parseIntent(data);
                            } else {
                                navigator.onBackPressed();
                                return Observable.just(Result.InitialResult.clear());
                            }
                        });
            }
        });

        upButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.pop());
            return Observable.empty();
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
                                                parseGetContentIntent(contentResolver, data);
                                        ImmutableList.Builder<File> builder =
                                                ImmutableList.<File>builder().addAll(dataFiles);
                                        for (FileStream fileStream : fileStreams) {
                                            File dataFile = fileSystem.cache(fileStream);
                                            if (dataFiles.contains(dataFile)) {
                                                throw new IllegalArgumentException(
                                                        "File already exists in the container");
                                            }
                                            builder.add(dataFile);
                                        }
                                        return builder.build();
                                    })
                                    .map(Result.DataFilesAddResult::success)
                                    .onErrorReturn(Result.DataFilesAddResult::failure)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .startWith(Result.DataFilesAddResult.activity());
                        } else {
                            return Observable.just(Result.DataFilesAddResult.clear());
                        }
                    });
        });

        dataFileRemove = upstream -> upstream.switchMap(intent ->
                Observable
                        .fromCallable(() -> {
                            try {
                                //noinspection ResultOfMethodCallIgnored
                                intent.dataFile().delete();
                            } catch (Exception e) {
                                // ignore because it's a cache file and is deleted anyway
                            }
                            return Result.DataFileRemoveResult.create(
                                    without(intent.dataFiles(), intent.dataFile()));
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()));

        dataFileView = upstream -> upstream.switchMap(intent -> {
            File file = intent.dataFile();
            navigator.execute(Transaction
                    .activity(createViewIntent(application, file, mimeType(file)), null));
            return Observable.empty();
        });

        recipientsAddButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.push(
                    CryptoRecipientsScreen.create(intent.cryptoCreateScreenId())));
            return Observable.just(Result.RecipientsAddButtonClickResult.create());
        });

        recipientsScreenUpButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.pop());
            return Observable.empty();
        });

        recipientsSearch = upstream -> upstream.switchMap(intent -> {
            if (intent.query() == null) {
                return Observable.just(Result.RecipientsSearchResult.clear());
            } else {
                return Observable
                        .fromCallable(() -> recipientRepository.find(intent.query()))
                        .map(Result.RecipientsSearchResult::success)
                        .onErrorReturn(Result.RecipientsSearchResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.RecipientsSearchResult.activity());
            }
        });

        recipientAdd = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientAddResult.create(
                                with(intent.recipients(), intent.recipient(), false))));

        recipientRemove = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientRemoveResult.create(
                                without(intent.recipients(), intent.recipient()))));

        encrypt = upstream -> upstream.switchMap(intent -> {
            String name = intent.name();
            ImmutableList<File> dataFiles = intent.dataFiles();
            ImmutableList<Certificate> recipients = intent.recipients();
            if (name != null && dataFiles != null && recipients != null) {
                return Single
                        .fromCallable(() -> {
                            File containerFile = fileSystem.generateSignatureContainerFile(name);
                            return CryptoContainer.encrypt(dataFiles, recipients, containerFile)
                                    .file();
                        })
                        .flatMapObservable(file ->
                                Observable
                                        .timer(3, TimeUnit.SECONDS)
                                        .map(ignored -> Result.EncryptResult.success(file))
                                        .startWith(Result.EncryptResult.successMessage(file)))
                        .onErrorReturn(Result.EncryptResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.EncryptResult.activity());
            } else {
                return Observable.just(Result.EncryptResult.clear());
            }
        });

        decryption = upstream -> upstream.switchMap(intent -> {
            if (intent.visible()) {
                return idCardService.data()
                        .map(Result.DecryptionResult::show)
                        .startWith(Result.DecryptionResult.show(IdCardDataResponse.initial()));
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
                        .flatMapObservable(dataFiles ->
                                Observable
                                        .timer(3, TimeUnit.SECONDS)
                                        .map(ignored -> Result.DecryptResult.success(dataFiles))
                                        .startWith(Result.DecryptResult.successMessage(dataFiles)))
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
                        .startWith(Result.DecryptResult.activity());
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
                shared.ofType(Intent.DataFilesAddIntent.class).compose(dataFilesAdd),
                shared.ofType(Intent.DataFileRemoveIntent.class).compose(dataFileRemove),
                shared.ofType(Intent.DataFileViewIntent.class).compose(dataFileView),
                shared.ofType(Intent.RecipientsAddButtonClickIntent.class)
                        .compose(recipientsAddButtonClick),
                shared.ofType(Intent.RecipientsScreenUpButtonClickIntent.class)
                        .compose(recipientsScreenUpButtonClick),
                shared.ofType(Intent.RecipientsSearchIntent.class).compose(recipientsSearch),
                shared.ofType(Intent.RecipientAddIntent.class).compose(recipientAdd),
                shared.ofType(Intent.RecipientRemoveIntent.class).compose(recipientRemove),
                shared.ofType(Intent.EncryptIntent.class).compose(encrypt),
                shared.ofType(Intent.DecryptionIntent.class).compose(decryption),
                shared.ofType(Intent.DecryptIntent.class).compose(decrypt),
                shared.ofType(Intent.SendIntent.class).compose(send)));
    }

    private Observable<Result.InitialResult> parseIntent(android.content.Intent intent) {
        return Observable
                .fromCallable(() -> {
                    ImmutableList<FileStream> fileStreams =
                            parseGetContentIntent(contentResolver, intent);
                    if (fileStreams.size() == 1
                            && isContainerFileName(fileStreams.get(0).displayName())) {
                        File file = fileSystem.addSignatureContainer(fileStreams.get(0));
                        return Result.InitialResult.success(CryptoContainer.open(file));
                    } else {
                        ImmutableList.Builder<File> builder = ImmutableList.builder();
                        for (FileStream fileStream : fileStreams) {
                            builder.add(fileSystem.cache(fileStream));
                        }
                        ImmutableList<File> dataFiles = builder.build();
                        File file = fileSystem.generateSignatureContainerFile(
                                createContainerFileName(dataFiles.get(0).getName()));
                        return Result.InitialResult.success(file, dataFiles);
                    }
                })
                .onErrorReturn(Result.InitialResult::failure)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .startWith(Result.InitialResult.activity());
    }
}
