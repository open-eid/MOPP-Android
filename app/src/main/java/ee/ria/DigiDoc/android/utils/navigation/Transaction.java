package ee.ria.DigiDoc.android.utils.navigation;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

public interface Transaction {

    @AutoValue
    abstract class SetRootScreenTransaction implements Transaction {

        public abstract Screen screen();

        public static SetRootScreenTransaction create(Screen screen) {
            return new AutoValue_Transaction_SetRootScreenTransaction(screen);
        }
    }

    @AutoValue
    abstract class PushScreenTransaction implements Transaction {

        public abstract Screen screen();

        public static PushScreenTransaction create(Screen screen) {
            return new AutoValue_Transaction_PushScreenTransaction(screen);
        }
    }

    @AutoValue
    abstract class PushScreensTransaction implements Transaction {

        public abstract ImmutableList<Screen> screens();

        public static PushScreensTransaction create(Screen... screens) {
            return new AutoValue_Transaction_PushScreensTransaction(ImmutableList.copyOf(screens));
        }
    }

    @AutoValue
    abstract class PopScreenTransaction implements Transaction {

        public static PopScreenTransaction create() {
            return new AutoValue_Transaction_PopScreenTransaction();
        }
    }

    @AutoValue
    abstract class ReplaceCurrentScreenTransaction implements Transaction {

        public abstract Screen screen();

        public static ReplaceCurrentScreenTransaction create(Screen screen) {
            return new AutoValue_Transaction_ReplaceCurrentScreenTransaction(screen);
        }
    }
}
