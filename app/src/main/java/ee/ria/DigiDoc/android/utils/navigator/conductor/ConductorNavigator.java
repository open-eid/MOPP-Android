package ee.ria.DigiDoc.android.utils.navigator.conductor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import ee.ria.DigiDoc.android.utils.navigator.ActivityResult;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.RequestPermissionsResult;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import timber.log.Timber;

public final class ConductorNavigator implements Navigator {

    private final Callable<List<Screen>> rootScreenFactory;
    private final ViewModelProvider.Factory viewModelFactory;
    private final Map<String, ScreenViewModelProvider> viewModelProviders;
    private final Subject<ActivityResult> activityResultSubject;
    private final Subject<RequestPermissionsResult> requestPermissionsResultSubject;
    private final List<BackButtonClickListener> backButtonClickListeners = new ArrayList<>();

    private Router router;

    public ConductorNavigator(Callable<List<Screen>> rootScreenFactory,
                              ViewModelProvider.Factory viewModelFactory) {
        this.rootScreenFactory = rootScreenFactory;
        this.viewModelFactory = viewModelFactory;
        this.viewModelProviders = new HashMap<>();
        this.activityResultSubject = PublishSubject.create();
        this.requestPermissionsResultSubject = PublishSubject.create();
    }

    @Override
    public void onCreate(Activity activity, ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
        router = Conductor.attachRouter(activity, container, savedInstanceState)
                .setPopRootControllerMode(Router.PopRootControllerMode.NEVER);
        router.addChangeListener(new ControllerChangeHandler.ControllerChangeListener() {
            @Override
            public void onChangeStarted(@Nullable Controller to, @Nullable Controller from,
                                        boolean isPush, @NonNull ViewGroup container,
                                        @NonNull ControllerChangeHandler handler) {
            }
            @Override
            public void onChangeCompleted(@Nullable Controller to, @Nullable Controller from,
                                          boolean isPush, @NonNull ViewGroup container,
                                          @NonNull ControllerChangeHandler handler) {
                Timber.log(Log.DEBUG, "onChangeCompleted:\nFROM:  %s\nTO:    %s\nPUSH:  %s\nSTACK: %s", from, to,
                        isPush, stack(router));
                if (!isPush && from != null) {
                    String screenId = from.getInstanceId();
                    clearViewModel(screenId);
                }
            }
        });
        if (!router.hasRootController()) {
            try {
                List<Screen> screens = rootScreenFactory.call();
                if (screens.isEmpty()) {
                    throw new IllegalStateException("Screen list is empty");
                }

                Transaction transaction;
                if (screens.size() == 1) {
                    transaction = Transaction.root(screens.get(0));
                } else {
                    transaction = Transaction.backstack(screens);
                }
                execute(transaction);
            } catch (Exception e) {
                throw new IllegalStateException("Root screen creation failed", e);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        for (int i = backButtonClickListeners.size() - 1; i >= 0; i--) {
            if (backButtonClickListeners.get(i).onBackButtonClick()) {
                return true;
            }
        }
        return router.handleBack();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Timber.log(Log.DEBUG, "onActivityResult: %s, %s, %s", requestCode, resultCode, data);
        activityResultSubject.onNext(ActivityResult.create(requestCode, resultCode, data));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.log(Log.DEBUG, "onRequestPermissionsResult: %s, %s, %s", requestCode, permissions, grantResults);
        requestPermissionsResultSubject.onNext(RequestPermissionsResult.create(requestCode, permissions, grantResults));
    }

    @Override
    public <T extends ViewModel> T viewModel(String screenId, Class<T> type) {
        if (!viewModelProviders.containsKey(screenId)) {
            viewModelProviders.put(screenId, ScreenViewModelProvider.create(viewModelFactory));
        }
        return viewModelProviders.get(screenId).get(type);
    }

    @Override
    public void clearViewModel(String screenId) {
        if (viewModelProviders.containsKey(screenId)) {
            viewModelProviders.get(screenId).clear();
            viewModelProviders.remove(screenId);
        }
    }

    @Override
    public void execute(Transaction transaction) {
        Timber.log(Log.DEBUG, "Execute: %s", transaction);
        transaction.execute(router, activity());
    }

    @Override
    public Observable<ActivityResult> activityResults() {
        return activityResultSubject;
    }

    @Override
    public Observable<RequestPermissionsResult> requestPermissionsResults() {
        return requestPermissionsResultSubject;
    }

    @Override
    public void addBackButtonClickListener(BackButtonClickListener listener) {
        backButtonClickListeners.add(listener);
    }

    @Override
    public void removeBackButtonClickListener(BackButtonClickListener listener) {
        backButtonClickListeners.remove(listener);
    }

    @Override
    public Activity activity() {
        if (router == null) {
            throw new IllegalStateException("Router is null");
        }
        Activity activity = router.getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity is null");
        }
        return activity;
    }

    private static RouterTransaction routerTransaction(Screen screen) {
        return RouterTransaction.with((Controller) screen);
    }

    static final class ScreenViewModelProvider extends ViewModelProvider {

        static ScreenViewModelProvider create(Factory factory) {
            return new ScreenViewModelProvider(new ViewModelStore(), factory);
        }

        private final ViewModelStore store;

        ScreenViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
            super(store, factory);
            this.store = store;
        }

        void clear() {
            store.clear();
        }
    }

    private static List<Controller> stack(Router router) {
        List<Controller> controllers = new ArrayList<>();
        for (RouterTransaction transaction : router.getBackstack()) {
            controllers.add(transaction.controller());
        }
        return controllers;
    }
}
