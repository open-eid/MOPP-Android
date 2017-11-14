package ee.ria.DigiDoc.android.utils.conductor;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import javax.inject.Inject;
import javax.inject.Singleton;

import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import ee.ria.DigiDoc.android.utils.navigation.Screen;

import static com.bluelinelabs.conductor.Conductor.attachRouter;

@Singleton
public final class ConductorNavigator implements Navigator {

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
    public Navigator childNavigator(ViewGroup container) {
        Router childRouter = router.getBackstack().get(router.getBackstackSize() - 1).controller()
                .getChildRouter(container);
        return new ConductorNavigator(childRouter);
    }
}
