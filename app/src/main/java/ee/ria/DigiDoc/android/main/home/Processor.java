package ee.ria.DigiDoc.android.main.home;

import android.support.annotation.IdRes;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.crypto.CryptoHomeScreen;
import ee.ria.DigiDoc.android.eid.EIDHomeScreen;
import ee.ria.DigiDoc.android.signature.home.SignatureHomeScreen;
import ee.ria.DigiDoc.android.utils.navigation.Screen;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.NavigationAction, Result.NavigationResult>
            navigation;

    @Inject Processor() {
        navigation = upstream -> upstream.switchMap(action ->
                Observable.just(Result.NavigationResult
                        .create(navigationItemToScreen(action.item()))));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.NavigationAction.class).compose(navigation)));
    }

    private Screen navigationItemToScreen(@IdRes int item) {
        switch (item) {
            case R.id.mainHomeNavigationSignature:
                return SignatureHomeScreen.create();
            case R.id.mainHomeNavigationCrypto:
                return CryptoHomeScreen.create();
            case R.id.mainHomeNavigationEID:
                return EIDHomeScreen.create();
            default:
                throw new IllegalArgumentException("Unknown navigation item: " + item);
        }
    }
}
