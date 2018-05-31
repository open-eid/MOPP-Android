package ee.ria.DigiDoc.android.crypto.create;

import android.os.SystemClock;

import com.google.common.collect.ImmutableList;

import org.threeten.bp.LocalDate;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.model.EIDType;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.cryptolib.Recipient;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

final class Processor implements ObservableTransformer<Intent, Result> {

    private final ObservableTransformer<Intent.InitialIntent, Result.VoidResult> initial;

    private final ObservableTransformer<Intent.RecipientsAddButtonClickIntent, Result.VoidResult>
            recipientsAddButtonClick;

    private final ObservableTransformer<Intent.RecipientsSearchIntent,
                                        Result.RecipientsSearchResult> recipientsSearch;

    @Inject
    Processor(Navigator navigator) {
        initial = upstream -> upstream.map(intent -> Result.VoidResult.create());

        recipientsAddButtonClick = upstream -> upstream.switchMap(intent -> {
            navigator.execute(Transaction.push(
                    CryptoRecipientsScreen.create(intent.cryptoCreateScreenId())));
            return Observable.empty();
        });

        recipientsSearch = upstream -> upstream.switchMap(intent ->
                Observable
                        .fromCallable(() -> {
                            SystemClock.sleep(3000);
                            return ImmutableList.of(
                                    Recipient.create("Mari Maasikas, 48405050123", EIDType.DIGI_ID,
                                            LocalDate.now()),
                                    Recipient.create("Jüri Juurikas, 38405050123", EIDType.ID_CARD,
                                            LocalDate.now()));
                        })
                        .map(Result.RecipientsSearchResult::success)
                        .onErrorReturn(Result.RecipientsSearchResult::failure)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(Result.RecipientsSearchResult.activity()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Intent> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Intent.InitialIntent.class).compose(initial),
                shared.ofType(Intent.RecipientsAddButtonClickIntent.class)
                        .compose(recipientsAddButtonClick),
                shared.ofType(Intent.RecipientsSearchIntent.class).compose(recipientsSearch)));
    }
}
