package ee.ria.DigiDoc.android.signature.data;

import com.google.auto.value.AutoValue;

import java.io.File;

@AutoValue
public abstract class ContainerAdd {

    public abstract boolean isExistingContainer();

    public abstract File containerFile();

    public static ContainerAdd create(boolean isExistingContainer, File containerFile) {
        return new AutoValue_ContainerAdd(isExistingContainer, containerFile);
    }
}
