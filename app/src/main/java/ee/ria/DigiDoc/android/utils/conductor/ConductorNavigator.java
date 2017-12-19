package ee.ria.DigiDoc.android.utils.conductor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.android.utils.mvi.MviViewModelProvider;
import ee.ria.DigiDoc.android.utils.navigation.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.navigation.Screen;
import ee.ria.DigiDoc.android.utils.navigation.Transaction;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.bluelinelabs.conductor.Conductor.attachRouter;

@Singleton
public final class ConductorNavigator implements Navigator {

    private final Subject<ActivityResult> activityResultSubject = PublishSubject.create();

    private Router router;

    @Inject
    ConductorNavigator() {
    }

    /**
     * Create child Navigator.
     *
     * @see #childNavigator(ViewGroup)
     *
     * @param router Child router.
     */
    private ConductorNavigator(Router router) {
        this.router = router;
    }

    @Override
    public void transaction(Transaction transaction) {
        if (transaction instanceof Transaction.SetRootScreenTransaction) {
            setRootScreen(((Transaction.SetRootScreenTransaction) transaction).screen());
        } else if (transaction instanceof Transaction.PushScreenTransaction) {
            pushScreen(((Transaction.PushScreenTransaction) transaction).screen());
        } else if (transaction instanceof Transaction.PushScreensTransaction) {
            pushScreens(((Transaction.PushScreensTransaction) transaction).screens());
        } else if (transaction instanceof Transaction.PopScreenTransaction) {
            popScreen();
        } else if (transaction instanceof Transaction.ReplaceCurrentScreenTransaction) {
            replaceCurrentScreen(
                    ((Transaction.ReplaceCurrentScreenTransaction) transaction).screen());
        } else {
            throw new IllegalArgumentException("Unknown navigator transaction " + transaction);
        }
    }

    public void attach(Activity activity, @IdRes int containerId, Bundle savedInstanceState) {
        router = attachRouter(activity, activity.findViewById(containerId), savedInstanceState);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        activityResultSubject.onNext(ActivityResult.create(requestCode, resultCode, data));
    }

    @Override
    public boolean hasRootScreen() {
        return router.hasRootController();
    }

    @Override
    public void setRootScreen(Screen screen) {
        router.setRoot(RouterTransaction.with((ConductorScreen) screen));
    }

    @Override
    public boolean handleBack() {
        return router.handleBack();
    }

    @Override
    public void pushScreen(Screen screen) {
        router.pushController(RouterTransaction.with((ConductorScreen) screen));
    }

    @Override
    public void pushScreens(ImmutableList<Screen> screens) {
        List<RouterTransaction> backstack = router.getBackstack();
        for (Screen screen : screens) {
            backstack.add(RouterTransaction.with((ConductorScreen) screen));
        }
        router.setBackstack(backstack, null);
    }

    @Override
    public void popScreen() {
        router.popCurrentController();
    }

    @Override
    public void replaceCurrentScreen(Screen screen) {
        router.replaceTopController(RouterTransaction.with((Controller) screen));
    }

    @Override
    public Navigator childNavigator(ViewGroup container) {
        return new ConductorNavigator(getCurrentScreen().getChildRouter(container));
    }

    @Override
    public void getActivityResult(int requestCode, Intent intent) {
        getActivityResult(requestCode, intent, null);
    }

    @Override
    public void getActivityResult(int requestCode, Intent intent, @Nullable Bundle options) {
        Activity activity = router.getActivity();
        if (activity == null) {
            throw new IllegalStateException("Can't request activity results, activity is null");
        }
        activity.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public Observable<ActivityResult> activityResults(int requestCode) {
        return activityResultSubject
                .filter(result -> result.requestCode() == requestCode);
    }

    @Override
    public Observable<Intent> activityOkResults(int requestCode) {
        return activityResults(requestCode)
                .filter(result -> result.resultCode() == Activity.RESULT_OK)
                .map(ActivityResult::data);
    }

    @Override
    public MviViewModelProvider getViewModelProvider() {
        return getCurrentScreen().getViewModelProvider();
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        AppCompatActivity activity = (AppCompatActivity) router.getActivity();
        if (activity == null) {
            throw new IllegalStateException("Can't start action mode, activity is null");
        }
        return activity.startSupportActionMode(callback);
    }

    private ConductorScreen getCurrentScreen() {
        return (ConductorScreen) router.getBackstack()
                .get(router.getBackstackSize() - 1)
                .controller();
    }
}
