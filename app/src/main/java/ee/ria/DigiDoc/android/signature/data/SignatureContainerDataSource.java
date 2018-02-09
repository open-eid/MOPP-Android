package ee.ria.DigiDoc.android.signature.data;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.mopplib.data.DataFile;
import ee.ria.mopplib.data.Signature;
import ee.ria.mopplib.data.SignedContainer;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface SignatureContainerDataSource {

    Single<ContainerAdd> addContainer(ImmutableList<FileStream> fileStreams, boolean forceCreate);

    Single<SignedContainer> get(File containerFile);

    Single<SignedContainer> addDocuments(File containerFile,
                                         ImmutableList<FileStream> documentStreams);

    Completable removeDocument(File containerFile, DataFile document);

    Single<File> getDocumentFile(File containerFile, DataFile document);

    Completable removeSignature(File containerFile, Signature signature);

    Completable addSignature(File containerFile, String signature);
}
