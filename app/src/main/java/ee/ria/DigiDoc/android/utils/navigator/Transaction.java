package ee.ria.DigiDoc.android.utils.navigator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import java.util.ArrayList;
import java.util.List;

public abstract class Transaction {

    public abstract void execute(Router router, Activity activity);

    public static RootTransaction root(Screen screen) {
        return new RootTransaction(screen);
    }

    public static PushTransaction push(Screen screen) {
        return new PushTransaction(screen);
    }

    public static PopTransaction pop() {
        return new PopTransaction();
    }

    public static ReplaceTransaction replace(Screen screen) {
        return new ReplaceTransaction(screen);
    }

    public static BackstackTransaction backstack(List<Screen> screens) {
        return new BackstackTransaction(screens);
    }

    public static ActivityTransaction activity(Intent intent, Bundle options) {
        return new ActivityTransaction(intent, options);
    }

    public static ActivityForResultTransaction activityForResult(int requestCode, Intent intent,
                                                                 @Nullable Bundle options) {
        return new ActivityForResultTransaction(requestCode, intent, options);
    }

    private static RouterTransaction routerTransaction(Screen screen) {
        return RouterTransaction.with((Controller) screen);
    }

    public static class RootTransaction extends Transaction {
        private final Screen screen;

        public RootTransaction(Screen screen) {
            this.screen = screen;
        }

        public Screen getScreen() {
            return screen;
        }

        @Override
        public void execute(Router router, Activity activity) {
            router.setRoot(routerTransaction(screen));
        }
    }

    public static class PushTransaction extends Transaction {
        private final Screen screen;

        public PushTransaction(Screen screen) {
            this.screen = screen;
        }

        public Screen getScreen() {
            return screen;
        }

        @Override
        public void execute(Router router, Activity activity) {
            router.pushController(routerTransaction(screen));
        }
    }

    public static class PopTransaction extends Transaction {
        @Override
        public void execute(Router router, Activity activity) {
            if (activity != null) {
                activity.onBackPressed();
            }
        }
    }

    public static class ReplaceTransaction extends Transaction {
        private final Screen screen;

        public ReplaceTransaction(Screen screen) {
            this.screen = screen;
        }

        public Screen getScreen() {
            return screen;
        }

        @Override
        public void execute(Router router, Activity activity) {
            router.replaceTopController(routerTransaction(screen));
        }
    }

    public static class BackstackTransaction extends Transaction {
        private final List<Screen> screens;

        public BackstackTransaction(List<Screen> screens) {
            this.screens = screens;
        }

        public List<Screen> getScreens() {
            return screens;
        }

        @Override
        public void execute(Router router, Activity activity) {
            List<RouterTransaction> routerTransactions = new ArrayList<>();
            for (Screen screen : screens) {
                routerTransactions.add(routerTransaction(screen));
            }
            router.setBackstack(routerTransactions, null);
        }
    }

    public static class ActivityTransaction extends Transaction {
        private final Intent intent;
        @Nullable private final Bundle options;

        public ActivityTransaction(Intent intent, @Nullable Bundle options) {
            this.intent = intent;
            this.options = options;
        }

        public Intent getIntent() {
            return intent;
        }

        @Nullable
        public Bundle getOptions() {
            return options;
        }

        @Override
        public void execute(Router router, Activity activity) {
            activity.startActivity(intent, options);
        }
    }

    public static class ActivityForResultTransaction extends Transaction {
        private final int requestCode;
        private final Intent intent;
        @Nullable private final Bundle options;

        public ActivityForResultTransaction(int requestCode, Intent intent,
                                            @Nullable Bundle options) {
            this.requestCode = requestCode;
            this.intent = intent;
            this.options = options;
        }

        public int getRequestCode() {
            return requestCode;
        }

        public Intent getIntent() {
            return intent;
        }

        @Nullable
        public Bundle getOptions() {
            return options;
        }

        @Override
        public void execute(Router router, Activity activity) {
            activity.startActivityForResult(intent, requestCode, options);
        }
    }
}



/*public abstract class Transaction {

    public static RootTransaction root(Screen screen) {
        return RootTransaction.create(screen);
    }

    public static PushTransaction push(Screen screen) {
        return PushTransaction.create(screen);
    }

    public static PopTransaction pop() {
        return PopTransaction.create();
    }

    public static ReplaceTransaction replace(Screen screen) {
        return ReplaceTransaction.create(screen);
    }

    public static ActivityTransaction activity(Intent intent, Bundle options) {
        return ActivityTransaction.create(intent, options);
    }

    public static ActivityForResultTransaction activityForResult(int requestCode, Intent intent,
                                                                 @Nullable Bundle options) {
        return ActivityForResultTransaction.create(requestCode, intent, options);
    }

    @AutoValue
    public abstract static class RootTransaction extends Transaction {

        public abstract Screen screen();

        static RootTransaction create(Screen screen) {
            return new AutoValue_Transaction_RootTransaction(screen);
        }
    }

    @AutoValue
    public abstract static class PushTransaction extends Transaction {

        public abstract Screen screen();

        static PushTransaction create(Screen screen) {
            return new AutoValue_Transaction_PushTransaction(screen);
        }
    }

    @AutoValue
    public abstract static class PopTransaction extends Transaction {

        static PopTransaction create() {
            return new AutoValue_Transaction_PopTransaction();
        }
    }

    @AutoValue
    public abstract static class ReplaceTransaction extends Transaction {

        public abstract Screen screen();

        static ReplaceTransaction create(Screen screen) {
            return new AutoValue_Transaction_ReplaceTransaction(screen);
        }
    }

    @AutoValue
    public abstract static class ActivityTransaction extends Transaction {

        public abstract Intent intent();

        @Nullable public abstract Bundle options();

        static ActivityTransaction create(Intent intent, @Nullable Bundle options) {
            return new AutoValue_Transaction_ActivityTransaction(intent, options);
        }
    }

    @AutoValue
    public abstract static class ActivityForResultTransaction extends Transaction {

        public abstract int requestCode();

        public abstract Intent intent();

        @Nullable public abstract Bundle options();

        static ActivityForResultTransaction create(int requestCode, Intent intent,
                                                   @Nullable Bundle options) {
            return new AutoValue_Transaction_ActivityForResultTransaction(requestCode, intent,
                    options);
        }
    }
}*/
