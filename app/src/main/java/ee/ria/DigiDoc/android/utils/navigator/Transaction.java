package ee.ria.DigiDoc.android.utils.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

public abstract class Transaction {

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
}
