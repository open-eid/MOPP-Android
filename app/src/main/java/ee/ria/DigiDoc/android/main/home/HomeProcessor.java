package ee.ria.DigiDoc.android.main.home;

import com.google.common.collect.ImmutableMap;

import ee.ria.DigiDoc.R;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

final class HomeProcessor implements ObservableTransformer<HomeIntent, HomeViewState> {

    private static final ImmutableMap<Integer, Integer> NAVIGATION_ITEM_SCREEN_MAP = ImmutableMap
            .<Integer, Integer>builder()
            .put(R.id.mainHomeNavigationSignature, R.id.signatureHomeScreen)
            .put(R.id.mainHomeNavigationCrypto, R.id.cryptoHomeScreen)
            .put(R.id.mainHomeNavigationEID, R.id.eidHomeScreen)
            .build();

    private final ObservableTransformer<HomeIntent.NavigationIntent, HomeViewState>
            navigationProcessor = intent -> intent
            .filter(navigationIntent ->
                    NAVIGATION_ITEM_SCREEN_MAP.containsKey(navigationIntent.navigationItem()))
            .map(navigationIntent ->
                    HomeViewState.create(NAVIGATION_ITEM_SCREEN_MAP
                            .get(navigationIntent.navigationItem())));

    @Override
    public ObservableSource<HomeViewState> apply(Observable<HomeIntent> upstream) {
        return upstream.publish(intent -> Observable.merge(
                intent.ofType(HomeIntent.NavigationIntent.class).compose(navigationProcessor),
                Observable.empty()));
    }
}
