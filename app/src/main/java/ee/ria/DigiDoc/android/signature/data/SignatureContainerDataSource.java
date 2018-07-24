package ee.ria.DigiDoc.android.signature.data;

import com.google.common.collect.ImmutableList;

import java.io.File;

import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.Signature;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface SignatureContainerDataSource {

    Single<ImmutableList<File>> find();

    Single<ContainerAdd> addContainer(ImmutableList<FileStream> fileStreams, boolean forceCreate);

    Single<SignedContainer> get(File containerFile);

    Completable remove(File containerFile);

    Single<SignedContainer> addDocuments(File containerFile,
                                         ImmutableList<FileStream> documentStreams);

    Single<SignedContainer> removeDocument(File containerFile, DataFile document);

    Single<File> getDocumentFile(File containerFile, DataFile document);

    Single<SignedContainer> removeSignature(File containerFile, Signature signature);

    Single<SignedContainer> addSignature(File containerFile, String signature);
}
