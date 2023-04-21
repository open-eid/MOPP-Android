package ee.ria.DigiDoc.android.utils.navigator;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AutoValue
public abstract class RequestPermissionsResult {

    public abstract int requestCode();

    public abstract List<String> permissions();

    public abstract List<Integer> grantResults();

    public static RequestPermissionsResult create(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        return new AutoValue_RequestPermissionsResult(requestCode, List.of(permissions),
                Arrays.stream(grantResults)
                        .boxed()
                        .collect(Collectors.toList()));
    }
}
