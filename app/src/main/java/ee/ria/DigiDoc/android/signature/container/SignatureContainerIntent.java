package ee.ria.DigiDoc.android.signature.container;

import android.net.Uri;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.android.utils.mvi.MviIntent;

interface SignatureContainerIntent extends MviIntent {

    @AutoValue
    abstract class DocumentAddIntent implements SignatureContainerIntent {

        abstract ImmutableList<Uri> uris();

        static DocumentAddIntent create(ImmutableList<Uri> uris) {
            return new AutoValue_SignatureContainerIntent_DocumentAddIntent(uris);
        }
    }
}
