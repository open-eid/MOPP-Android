package ee.ria.DigiDoc.android.crypto.create;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;

import com.google.common.collect.ImmutableList;

import java.io.File;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.android.utils.files.FileSystem;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.cryptolib.DataFile;
import ee.ria.cryptolib.RecipientRepository;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.io.Files.getNameWithoutExtension;
import static ee.ria.DigiDoc.android.Constants.RC_CRYPTO_CREATE_DATA_FILE_ADD;
import static ee.ria.DigiDoc.android.Constants.RC_CRYPTO_CREATE_INITIAL;
import static ee.ria.DigiDoc.android.utils.Immutables.with;
import static ee.ria.DigiDoc.android.utils.Immutables.without;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createGetContentIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.createViewIntent;
import static ee.ria.DigiDoc.android.utils.IntentUtils.parseGetContentIntent;
import static ee.ria.mopplib.data.SignedContainer.mimeType;

final class Processor implements ObservableTransformer<Intent, Result> {

    private final ObservableTransformer<Intent.InitialIntent, Result.InitialResult> initial;

    private final ObservableTransformer<Intent.UpButtonClickIntent,
                                        Result.VoidResult> upButtonClick;

    private final ObservableTransformer<Intent.DataFilesAddIntent,
                                        Result.DataFilesAddResult> dataFilesAdd;

    private final ObservableTransformer<Intent.DataFileRemoveIntent,
                                        Result.DataFileRemoveResult> dataFileRemove;

    private final ObservableTransformer<Intent.DataFileViewIntent, Result.VoidResult> dataFileView;

    private final ObservableTransformer<Intent.RecipientsAddButtonClickIntent, Result.VoidResult>
            recipientsAddButtonClick;

    private final ObservableTransformer<Intent.RecipientsScreenUpButtonClickIntent,
                                        Result.VoidResult> recipientsScreenUpButtonClick;

    private final ObservableTransformer<Intent.RecipientsSearchIntent,
            Result.RecipientsSearchResult> recipientsSearch;

    private final ObservableTransformer<Intent.RecipientAddIntent,
            Result.RecipientAddResult> recipientAdd;

    private final ObservableTransformer<Intent.RecipientRemoveIntent,
                                        Result.RecipientRemoveResult> recipientRemove;

    private final ObservableTransformer<Intent.EncryptIntent, Result.EncryptResult> encrypt;

    @Inject Processor(Navigator navigator, RecipientRepository recipientRepository,
                      ContentResolver contentResolver, FileSystem fileSystem,
                      Application application) {
        initial = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.activityForResult(RC_CRYPTO_CREATE_INITIAL,
                    createGetContentIntent(), null));
            return navigator.activityResults()
                    .filter(activityResult ->
                            activityResult.requestCode() == RC_CRYPTO_CREATE_INITIAL)
                    .switchMap(activityResult -> {
                        android.content.Intent data = activityResult.data();
                        if (activityResult.resultCode() == Activity.RESULT_OK && data != null) {
                            return Observable
                                    .fromCallable(() -> {
                                        ImmutableList<FileStream> fileStreams =
                                                parseGetContentIntent(contentResolver, data);
                                        ImmutableList.Builder<DataFile> builder =
                                                ImmutableList.builder();
                                        for (FileStream fileStream : fileStreams) {
                                            builder.add(DataFile
                                                    .create(fileSystem.cache(fileStream)));
                                        }
                                        ImmutableList<DataFile> dataFiles = builder.build();

                                        File containerFile = fileSystem
                                                .generateSignatureContainerFile(
                                                        getNameWithoutExtension(
                                                                dataFiles.get(0).file().getName()) +
                                                                ".cdoc");

                                        return Result.InitialResult.success(containerFile,
                                                dataFiles);
                                    })
                                    .onErrorReturn(Result.InitialResult::failure)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .startWith(Result.InitialResult.activity());
                        } else {
                            return Observable.just(Result.InitialResult.clear());
                        }
                    });
        });

        upButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.pop());
            return Observable.empty();
        });

        dataFilesAdd = upstream -> upstream.switchMap(intent -> {
            ImmutableList<DataFile> dataFiles = intent.dataFiles();
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
                        if (activityResult.resultCode() == Activity.RESULT_OK && data != null) {
                            return Observable
                                    .fromCallable(() -> {
                                        ImmutableList<FileStream> fileStreams =
                                                parseGetContentIntent(contentResolver, data);
                                        ImmutableList.Builder<DataFile> builder =
                                                ImmutableList.<DataFile>builder().addAll(dataFiles);
                                        for (FileStream fileStream : fileStreams) {
                                            builder.add(DataFile
                                                    .create(fileSystem.cache(fileStream)));
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
                            //noinspection ResultOfMethodCallIgnored
                            intent.dataFile().file().delete();
                            return Result.DataFileRemoveResult.success(
                                    without(intent.dataFiles(), intent.dataFile()));
                        })
                        .onErrorReturn(Result.DataFileRemoveResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.DataFileRemoveResult.activity()));

        dataFileView = upstream -> upstream.switchMap(intent -> {
            File file = intent.dataFile().file();
            navigator.execute(Transaction
                    .activity(createViewIntent(application, file, mimeType(file)), null));
            return Observable.just(Result.VoidResult.create());
        });

        recipientsAddButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.push(
                    CryptoRecipientsScreen.create(intent.cryptoCreateScreenId())));
            return Observable.empty();
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
                        Result.RecipientAddResult.success(
                                with(intent.recipients(), intent.recipient(), false))));

        recipientRemove = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientRemoveResult.success(
                                without(intent.recipients(), intent.recipient()))));

        encrypt = upstream -> upstream.switchMap(intent -> {
            return Observable.just(Result.EncryptResult.create());
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
                shared.ofType(Intent.EncryptIntent.class).compose(encrypt)));
    }
}
