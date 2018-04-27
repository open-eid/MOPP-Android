package ee.ria.DigiDoc.android.main.home;

import android.app.Application;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;

import javax.inject.Inject;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.main.about.AboutScreen;
import ee.ria.DigiDoc.android.main.diagnostics.DiagnosticsScreen;
import ee.ria.DigiDoc.android.main.home.Intent.NavigationVisibilityIntent;
import ee.ria.DigiDoc.android.main.settings.SettingsScreen;
import ee.ria.DigiDoc.android.signature.list.SignatureListScreen;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

import static ee.ria.DigiDoc.android.utils.IntentUtils.createBrowserIntent;

final class Processor implements ObservableTransformer<Action, Result> {

    private static final SparseIntArray NAVIGATION_ITEM_VIEWS = new SparseIntArray();
    static {
        NAVIGATION_ITEM_VIEWS.put(R.id.mainHomeNavigationSignature, R.id.mainHomeSignature);
        NAVIGATION_ITEM_VIEWS.put(R.id.mainHomeNavigationCrypto, R.id.mainHomeCrypto);
        NAVIGATION_ITEM_VIEWS.put(R.id.mainHomeNavigationEID, R.id.mainHomeEID);
    }

    private final Navigator navigator;

    private final ObservableTransformer<Action.NavigationAction, Result.NavigationResult>
            navigation;

    private final ObservableTransformer<Action.MenuAction, Result.MenuResult> menu;

    private final ObservableTransformer<NavigationVisibilityIntent,
                                        Result.NavigationVisibilityResult> navigationVisibility;

    @Nullable private String eidScreenId;

    @Inject Processor(Application application, Navigator navigator) {
        this.navigator = navigator;
        navigation = upstream -> upstream.switchMap(action -> {
            if (action.item() != R.id.mainHomeNavigationEID) {
                clearEidViewModel();
            }
            return Observable.just(Result.NavigationResult
                    .create(NAVIGATION_ITEM_VIEWS.get(action.item())));
        });

        menu = upstream -> upstream.switchMap(action -> {
            Boolean isOpen = action.isOpen();
            Integer item = action.menuItem();
            if (isOpen != null) {
                return Observable.just(Result.MenuResult.create(isOpen));
            } else if (item != null) {
                clearEidViewModel();
                navigator.execute(menuItemToTransaction(application, item));
                return Observable.just(Result.MenuResult.create(false));
            }
            throw new IllegalStateException("Action is in invalid state: " + action);
        });

        navigationVisibility = upstream -> upstream.map(action ->
                Result.NavigationVisibilityResult.create(action.visible()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableSource<Result> apply(Observable<Action> upstream) {
        return upstream.publish(shared -> Observable.mergeArray(
                shared.ofType(Action.NavigationAction.class).compose(navigation),
                shared.ofType(Action.MenuAction.class).compose(menu),
                shared.ofType(NavigationVisibilityIntent.class).compose(navigationVisibility)));
    }

    void eidScreenId(@Nullable String eidScreenId) {
        this.eidScreenId = eidScreenId;
    }

    private void clearEidViewModel() {
        if (eidScreenId != null) {
            navigator.clearViewModel(eidScreenId);
        }
    }

    private static Transaction menuItemToTransaction(Context context, @IdRes int item) {
        switch (item) {
            case R.id.mainHomeMenuHelp:
                return Transaction
                        .activity(createBrowserIntent(context, R.string.main_home_menu_help_url),
                                null);
            case R.id.mainHomeMenuRecent:
                return Transaction.push(SignatureListScreen.create());
            case R.id.mainHomeMenuSettings:
                return Transaction.push(SettingsScreen.create());
            case R.id.mainHomeMenuAbout:
                return Transaction.push(AboutScreen.create());
            case R.id.mainHomeMenuDiagnostics:
                return Transaction.push(DiagnosticsScreen.create());
            default:
                throw new IllegalArgumentException("Unknown menu item: " + item);
        }
    }
}
