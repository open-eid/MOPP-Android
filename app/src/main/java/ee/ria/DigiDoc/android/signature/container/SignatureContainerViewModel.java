package ee.ria.DigiDoc.android.signature.container;

import javax.inject.Inject;

import ee.ria.DigiDoc.android.utils.mvi.MviViewModel;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class SignatureContainerViewModel implements MviViewModel<Intent, ViewState> {

    private final Processor processor;
    private final Subject<Intent> intentSubject;
    private final Observable<ViewState> stateObservable;

    @Inject
    SignatureContainerViewModel() {
        processor = new Processor();
        intentSubject = PublishSubject.create();
        stateObservable = intentSubject
                .compose(intentFilter)
                .map(this::actionFromIntent)
                .compose(processor)
                .scan(ViewState.idle(), (state, result) -> result.reduce(state))
                .replay(1)
                .autoConnect(0);
    }

    @Override
    public void process(Observable<Intent> intents) {
        intents.subscribe(intentSubject);
    }

    @Override
    public Observable<ViewState> states() {
        return stateObservable;
    }

    private Action actionFromIntent(Intent intent) {
        if (intent instanceof Intent.InitialIntent
                || intent instanceof Intent.ChooseDocumentsIntent) {
            return Action.ChooseDocumentsAction.create();
        } else if (intent instanceof Intent.AddDocumentsIntent) {
            return Action.AddDocumentsAction
                    .create(((Intent.AddDocumentsIntent) intent).fileStreams());
        }
        throw new IllegalArgumentException("Unknown intent " + intent);
    }

    private final ObservableTransformer<Intent, Intent> intentFilter =
            upstream -> upstream.publish(shared ->
                    Observable.merge(
                            shared.ofType(Intent.InitialIntent.class).take(1),
                            shared.filter(intent -> !(intent instanceof Intent.InitialIntent))));
}
