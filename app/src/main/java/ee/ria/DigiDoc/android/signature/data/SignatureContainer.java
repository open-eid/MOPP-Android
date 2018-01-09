package ee.ria.DigiDoc.android.signature.data;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.document.data.Document;

@AutoValue
public abstract class SignatureContainer {

    public abstract String name();

    public abstract ImmutableList<Document> documents();

    public abstract ImmutableList<Signature> signatures();

    public boolean documentAddEnabled() {
        return signatures().size() == 0;
    }

    public boolean documentRemoveEnabled() {
        return documentAddEnabled() && documents().size() != 1;
    }

    public static SignatureContainer create(String name, ImmutableList<Document> documents,
                                            ImmutableList<Signature> signatures) {
        return new AutoValue_SignatureContainer(name, documents, signatures);
    }
}
