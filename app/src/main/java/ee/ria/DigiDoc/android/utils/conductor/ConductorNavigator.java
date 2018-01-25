package ee.ria.DigiDoc.android.utils.conductor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Inject ConductorNavigator() {
    }

    @Override
    public void transaction(Transaction transaction) {
        transactions.add(transaction);
        handleTransactionQueue();
    }

    public void attach(Activity activity, @IdRes int containerId, Bundle savedInstanceState) {
        router = attachRouter(activity, activity.findViewById(containerId), savedInstanceState);
        router.addChangeListener(controllerChangeListener);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        transaction(Transaction.ActivityResultTransaction.create(ActivityResult
                .create(requestCode, resultCode, data)));
    }

    @Override
    public boolean hasRootScreen() {
        return router.hasRootController();
    }

    @Override
    public void setRootScreen(Screen screen) {
        transaction(Transaction.SetRootScreenTransaction.create(screen));
    }

    @Override
    public boolean handleBack() {
        return router.handleBack();
    }

    @Override
    public void pushScreen(Screen screen) {
        transaction(Transaction.PushScreenTransaction.create(screen));
    }

    @Override
    public void pushScreens(ImmutableList<Screen> screens) {
        Screen[] screenArray = new Screen[screens.size()];
        for (int i = 0; i < screenArray.length; i++) {
            screenArray[i] = screens.get(i);
        }
        transaction(Transaction.PushScreensTransaction.create(screenArray));
    }

    @Override
    public void popScreen() {
        transaction(Transaction.PopScreenTransaction.create());
    }

    @Override
    public void replaceCurrentScreen(Screen screen) {
        transaction(Transaction.ReplaceCurrentScreenTransaction.create(screen));
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
    public MviViewModelProvider getViewModelProvider() {
        return getCurrentScreen().getViewModelProvider();
    }

    private ConductorScreen getCurrentScreen() {
        return (ConductorScreen) router.getBackstack()
                .get(router.getBackstackSize() - 1)
                .controller();
    }

    /*
     * Have to manage queue of transactions because of a race condition.
     * View modification is not allowed in View#onAttachedToWindow,
     * where most of navigation transactions happen.
     *
     * Waiting until Conductor controller is in attached state (Controller#onAttached is called).
     *
     * This happens when Android has destroyed the activity instance.
     * Can be tested with "Don't keep activities" setting in Developer options.
     */

    private final AtomicBoolean screenAttached = new AtomicBoolean(true);
    private final List<Transaction> transactions = new ArrayList<>();

    private void handleTransactionQueue() {
        if (!screenAttached.get()) {
            return;
        }
        for (Transaction transaction : transactions) {
            handleTransaction(transaction);
        }
        transactions.clear();
    }

    private void handleTransaction(Transaction transaction) {
        if (transaction instanceof Transaction.SetRootScreenTransaction) {
            router.setRoot(RouterTransaction.with((ConductorScreen)
                    ((Transaction.SetRootScreenTransaction) transaction).screen()));
        } else if (transaction instanceof Transaction.PushScreenTransaction) {
            router.pushController(RouterTransaction.with((ConductorScreen)
                    ((Transaction.PushScreenTransaction) transaction).screen()));
        } else if (transaction instanceof Transaction.PushScreensTransaction) {
            List<RouterTransaction> backstack = router.getBackstack();
            for (Screen screen : ((Transaction.PushScreensTransaction) transaction).screens()) {
                backstack.add(RouterTransaction.with((ConductorScreen) screen));
            }
            router.setBackstack(backstack, null);
        } else if (transaction instanceof Transaction.PopScreenTransaction) {
            router.popCurrentController();
        } else if (transaction instanceof Transaction.ReplaceCurrentScreenTransaction) {
            router.replaceTopController(RouterTransaction.with((ConductorScreen)
                    ((Transaction.ReplaceCurrentScreenTransaction) transaction).screen()));
        } else if (transaction instanceof Transaction.ActivityResultTransaction) {
            activityResultSubject.onNext(((Transaction.ActivityResultTransaction) transaction)
                    .activityResult());
        } else {
            throw new IllegalArgumentException("Unknown navigator transaction " + transaction);
        }
    }

    private final ControllerChangeHandler.ControllerChangeListener controllerChangeListener =
            new ControllerChangeHandler.ControllerChangeListener() {

        @Override
        public void onChangeStarted(@Nullable Controller to, @Nullable Controller from,
                                    boolean isPush, @NonNull ViewGroup container,
                                    @NonNull ControllerChangeHandler handler) {}

        @Override
        public void onChangeCompleted(@Nullable Controller to, @Nullable Controller from,
                                      boolean isPush, @NonNull ViewGroup container,
                                      @NonNull ControllerChangeHandler handler) {
            if (to != null) {
                to.addLifecycleListener(lifecycleListener);
            }
        }
    };

    private final Controller.LifecycleListener lifecycleListener =
            new Controller.LifecycleListener() {

        @Override
        public void postAttach(@NonNull Controller controller, @NonNull View view) {
            screenAttached.set(true);
            handleTransactionQueue();
        }

        @Override
        public void preDetach(@NonNull Controller controller, @NonNull View view) {
            screenAttached.set(false);
        }

        @Override
        public void preDestroy(@NonNull Controller controller) {
            controller.removeLifecycleListener(lifecycleListener);
        }
    };
}
