package ee.ria.DigiDoc.android.signature.data;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.files.FileStream;

/**
 * Change this into immutable object, deal with native lib in processor.
 */
@AutoValue
public abstract class SignatureContainer {

    private static final ImmutableSet<String> EXTENSIONS = ImmutableSet.<String>builder()
            .add("asice", "asics", "sce", "scs", "adoc", "bdoc", "ddoc", "edoc")
            .build();

    public abstract String name();

    public abstract ImmutableList<Document> documents();

    public abstract boolean documentsLocked();

    public abstract ImmutableList<Signature> signatures();

    public static SignatureContainer create(String name, ImmutableList<Document> documents,
                                            boolean documentsLocked,
                                            ImmutableList<Signature> signatures) {
        return new AutoValue_SignatureContainer(name, documents, documentsLocked, signatures);
    }

    /**
     * Check whether this is a signature container file which should be opened as such
     * or a regular file which should be added to the container.
     *
     * @param fileStream File stream containing information about the document.
     * @return True if it is a container, false otherwise.
     */
    public static boolean isContainerFile(FileStream fileStream) {
        return EXTENSIONS.contains(Files.getFileExtension(fileStream.displayName()).toLowerCase());
    }
}
