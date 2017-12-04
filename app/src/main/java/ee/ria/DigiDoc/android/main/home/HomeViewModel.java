package ee.ria.DigiDoc.android.main.home;

import ee.ria.DigiDoc.android.utils.mvi.MviViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class HomeViewModel implements MviViewModel<HomeIntent, HomeViewState> {

    private final Subject<HomeIntent> intentSubject = PublishSubject.create();

    private final HomeProcessor processor = new HomeProcessor();

    @Override
    public void process(Observable<HomeIntent> intents) {
        intents.subscribe(intentSubject);
    }

    @Override
    public Observable<HomeViewState> viewStates() {
        return intentSubject.compose(processor);
    }
}
