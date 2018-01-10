package ee.ria.DigiDoc.android.document.data;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Document implements Parcelable {

    /**
     * Human-readable display name for the document.
     */
    public abstract String name();

    public abstract long size();

    public static Document create(String name, long size) {
        return new AutoValue_Document(name, size);
    }
}
