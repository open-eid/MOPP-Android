package ee.ria.DigiDoc.android.utils.mvi;

import android.support.annotation.Nullable;

import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.navigation.NavigatorResult;
import ee.ria.DigiDoc.android.utils.navigation.Transaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public abstract class BaseMviViewModel<
        I extends MviIntent,
        S extends MviViewState,
        A extends MviAction,
        R extends MviResult<S>> implements MviViewModel<I, S> {

    protected final Subject<I> intentSubject;
    private final Observable<S> viewStateObservable;

    protected BaseMviViewModel(ObservableTransformer<A, R> processor, Navigator navigator) {
        intentSubject = PublishSubject.create();
        viewStateObservable = intentSubject
                .compose(this::initialIntentFilter)
                .doOnNext(intent -> Timber.d("Intent: %s", intent))
                .filter(this::filterIntent)
                .map(this::actionFromIntent)
                .doOnNext(action -> Timber.d("Action: %s", action))
                .compose(processor)
                .doOnNext(result -> Timber.d("Result: %s", result))
                .doOnNext(this::onResult)
                .doOnNext(result -> {
                    if (!(result instanceof NavigatorResult)) {
                        return;
                    }
                    @Nullable Transaction transaction = ((NavigatorResult) result).transaction();
                    if (transaction == null) {
                        return;
                    }
                    Timber.d("Navigator transaction: %s", transaction);
                    navigator.transaction(transaction);
                })
                .scan(initialViewState(), (viewState, result) -> result.reduce(viewState))
                .doOnNext(viewState -> Timber.d("ViewState: %s", viewState))
                .replay(1)
                .autoConnect(0)
                .doOnDispose(this::onDispose);
    }

    @Override
    public void process(Observable<I> intents) {
        intents.subscribe(intentSubject);
    }

    @Override
    public Observable<S> viewStates() {
        return viewStateObservable;
    }

    protected abstract Class<? extends I> initialIntentType();

    protected boolean filterIntent(I intent) {
        return true;
    }

    protected abstract A actionFromIntent(I intent);

    protected abstract S initialViewState();

    protected void onResult(R result) {
    }

    protected void onDispose() {
    }

    private ObservableSource<I> initialIntentFilter(Observable<I> intents) {
        Class<? extends I> initialIntentType = initialIntentType();
        return intents.publish(shared ->
                Observable.merge(
                        shared.ofType(initialIntentType).take(1),
                        shared.filter(intent -> !initialIntentType.isInstance(intent))));
    }
}
