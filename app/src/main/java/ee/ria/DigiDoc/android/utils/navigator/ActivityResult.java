package ee.ria.DigiDoc.android.utils.navigator;

import android.content.Intent;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ActivityResult {

    public abstract int requestCode();

    public abstract int resultCode();

    @Nullable public abstract Intent data();

    public static ActivityResult create(int requestCode, int resultCode, @Nullable Intent data) {
        return new AutoValue_ActivityResult(requestCode, resultCode, data);
    }
}
