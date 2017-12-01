package ee.ria.DigiDoc.android.utils.conductor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.android.utils.navigation.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.navigation.Screen;
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
    public void popScreen() {
        router.popCurrentController();
    }

    @Override
    public Navigator childNavigator(ViewGroup container) {
        Router childRouter = router.getBackstack().get(router.getBackstackSize() - 1).controller()
                .getChildRouter(container);
        return new ConductorNavigator(childRouter);
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
}
