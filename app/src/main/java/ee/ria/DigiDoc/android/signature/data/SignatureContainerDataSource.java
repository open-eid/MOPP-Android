package ee.ria.DigiDoc.android.signature.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface SignatureContainerDataSource {

    Single<File> addContainer(ImmutableList<FileStream> fileStreams, boolean forceCreate);

    Single<SignatureContainer> get(File containerFile);

    Completable addDocuments(File containerFile, ImmutableList<FileStream> documentStreams);

    Completable removeDocument(File containerFile, Document document);

    Single<File> getDocumentFile(File containerFile, Document document);

    Completable removeSignature(File containerFile, Signature signature);

    Completable addSignature(File containerFile, String signature);
}
