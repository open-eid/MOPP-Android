package ee.ria.DigiDoc.android.main.home;

import org.junit.Before;
import org.junit.Test;

import ee.ria.DigiDoc.R;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class HomeViewModelTest {

    private Subject<HomeIntent> intents;
    private TestObserver<HomeViewState> states;

    @Before
    public void before() {
        HomeViewModel viewModel = new HomeViewModel();
        states = viewModel.viewStates().test();
        viewModel.process(intents = PublishSubject.create());
    }

    @Test
    public void navigationIntent_currentScreensAreCorrect() {
        intents.onNext(HomeIntent.NavigationIntent.create(R.id.mainHomeNavigationSignature));
        states.assertValueAt(0, HomeViewState.create(R.id.signatureHomeScreen));

        intents.onNext(HomeIntent.NavigationIntent.create(R.id.mainHomeNavigationCrypto));
        states.assertValueAt(1, HomeViewState.create(R.id.cryptoHomeScreen));

        intents.onNext(HomeIntent.NavigationIntent.create(R.id.mainHomeNavigationEID));
        states.assertValueAt(2, HomeViewState.create(R.id.eidHomeScreen));

        states.assertNotTerminated();
    }

    @Test
    public void navigationIntent_ignoreUnknownNavigationItems() {
        intents.onNext(HomeIntent.NavigationIntent.create(-1));
        states.assertEmpty();
    }
}
