package ee.ria.DigiDoc.android.document.data;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Document {

    /**
     * Human-readable display name for the document.
     */
    public abstract String name();

    public static Document create(String name) {
        return new AutoValue_Document(name);
    }
}
