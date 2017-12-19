package ee.ria.DigiDoc.android.signature.data;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;

/**
 * Change this into immutable object, deal with native lib in processor.
 */
@AutoValue
public abstract class SignatureContainer {

    public abstract String name();

    public abstract ImmutableList<Document> documents();

    public abstract boolean documentsLocked();

    public abstract ImmutableList<Signature> signatures();

    public int invalidSignatureCount() {
        int count = 0;
        for (Signature signature : signatures()) {
            if (!signature.valid()) {
                count++;
            }
        }
        return count;
    }

    public static SignatureContainer create(String name, ImmutableList<Document> documents,
                                            boolean documentsLocked,
                                            ImmutableList<Signature> signatures) {
        return new AutoValue_SignatureContainer(name, documents, documentsLocked, signatures);
    }
}
