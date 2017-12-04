package ee.ria.DigiDoc.android.utils.mvi;

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

    private final Subject<I> intentSubject;
    private final Observable<S> viewStateObservable;

    protected BaseMviViewModel(ObservableTransformer<A, R> processor) {
        intentSubject = PublishSubject.create();
        viewStateObservable = intentSubject
                .compose(this::initialIntentFilter)
                .map(intent -> {
                    Timber.d("Intent: %s", intent);
                    return intent;
                })
                .map(this::actionFromIntent)
                .map(action -> {
                    Timber.d("Action: %s", action);
                    return action;
                })
                .compose(processor)
                .map(result -> {
                    Timber.d("Result: %s", result);
                    return result;
                })
                .scan(initialViewState(), (viewState, result) -> result.reduce(viewState))
                .map(viewState -> {
                    Timber.d("ViewState: %s", viewState);
                    return viewState;
                })
                .replay(1)
                .autoConnect(0);
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

    protected abstract A actionFromIntent(I intent);

    protected abstract S initialViewState();

    private ObservableSource<I> initialIntentFilter(Observable<I> intents) {
        Class<? extends I> initialIntentType = initialIntentType();
        return intents.publish(shared ->
                Observable.merge(
                        shared.ofType(initialIntentType).take(1),
                        shared.filter(intent -> !initialIntentType.isInstance(intent))));
    }
}
