package ee.ria.DigiDoc.android.crypto.create;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.cryptolib.RecipientRepository;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static ee.ria.DigiDoc.android.utils.Immutables.with;
import static ee.ria.DigiDoc.android.utils.Immutables.without;

final class Processor implements ObservableTransformer<Intent, Result> {

    private final ObservableTransformer<Intent.InitialIntent, Result.VoidResult> initial;

    private final ObservableTransformer<Intent.RecipientsAddButtonClickIntent, Result.VoidResult>
            recipientsAddButtonClick;

    private final ObservableTransformer<Intent.RecipientsSearchIntent,
            Result.RecipientsSearchResult> recipientsSearch;

    private final ObservableTransformer<Intent.RecipientAddIntent,
            Result.RecipientAddResult> recipientAdd;

    private final ObservableTransformer<Intent.RecipientRemoveIntent,
                                        Result.RecipientRemoveResult> recipientRemove;

    @Inject
    Processor(Navigator navigator, RecipientRepository recipientRepository) {
        initial = upstream -> upstream.map(intent -> Result.VoidResult.create());

        recipientsAddButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.push(
                    CryptoRecipientsScreen.create(intent.cryptoCreateScreenId())));
            return Observable.empty();
        });

        recipientsSearch = upstream -> upstream.switchMap(intent ->
                Observable
                        .fromCallable(() -> recipientRepository.find(intent.query()))
                        .map(Result.RecipientsSearchResult::success)
                        .onErrorReturn(Result.RecipientsSearchResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.RecipientsSearchResult.activity()));

        recipientAdd = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientAddResult.success(
                                with(intent.recipients(), intent.recipient(), false))));

        recipientRemove = upstream -> upstream.switchMap(intent ->
                Observable.fromCallable(() ->
                        Result.RecipientRemoveResult.success(
                                without(intent.recipients(), intent.recipient()))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.RecipientsAddButtonClickIntent.class)
                        .compose(recipientsAddButtonClick),
                shared.ofType(Intent.RecipientsSearchIntent.class).compose(recipientsSearch),
                shared.ofType(Intent.RecipientAddIntent.class).compose(recipientAdd),
                shared.ofType(Intent.RecipientRemoveIntent.class).compose(recipientRemove)));
    }
}
