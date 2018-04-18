package ee.ria.DigiDoc.android.main.home;

import android.app.Application;
import android.net.Uri;
import android.support.annotation.IdRes;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.crypto.CryptoHomeScreen;
import ee.ria.DigiDoc.android.eid.EIDHomeScreen;
import ee.ria.DigiDoc.android.main.about.AboutScreen;
import ee.ria.DigiDoc.android.main.settings.SettingsScreen;
import ee.ria.DigiDoc.android.signature.home.SignatureHomeScreen;
import ee.ria.DigiDoc.android.signature.list.SignatureListScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import android.content.Intent;
final class Processor implements ObservableTransformer<Action, Result> {

    private final ObservableTransformer<Action.NavigationAction, Result.NavigationResult>
            navigation;

    private final ObservableTransformer<Action.MenuAction, Result.MenuResult> menu;

    @Inject Processor(Application application, Navigator navigator) {
        navigation = upstream -> upstream.switchMap(action ->
                Observable.just(Result.NavigationResult
                        .create(navigationItemToScreen(action.item()))));

        menu = upstream -> upstream.switchMap(action -> {
            Boolean isOpen = action.isOpen();
            Integer item = action.menuItem();
            if (isOpen != null) {
                return Observable.just(Result.MenuResult.create(isOpen));
            } else if (item != null) {
                if(item == R.id.mainHomeMenuHelp){
                    navigator.execute(Transaction.
                            activity(new Intent(Intent.ACTION_VIEW, Uri.parse(application.getString(R.string.help_url))),
                            null));
                } else {
                    navigator.execute(Transaction.push(menuItemToScreen(item)));
                }
                return Observable.just(Result.MenuResult.create(false));
            }
            throw new IllegalStateException("Action is in invalid state: " + action);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.NavigationAction.class).compose(navigation),
                shared.ofType(Action.MenuAction.class).compose(menu)));
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

    private Screen menuItemToScreen(@IdRes int item) {
        switch (item) {
            case R.id.mainHomeMenuRecent:
                return SignatureListScreen.create();
            case R.id.mainHomeMenuSettings:
                return SettingsScreen.create();
            case R.id.mainHomeMenuAbout:
                return AboutScreen.create();
            default:
                throw new IllegalArgumentException("Unknown menu item: " + item);
        }
    }
}
