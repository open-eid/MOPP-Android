package ee.ria.DigiDoc.android.utils.files;

import java.io.File;

import ee.ria.DigiDoc.android.signature.data.SignatureContainerDataSource;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.SignedContainer;

public class SignedFilesUtil {

    public static File getContainerDataFile(SignatureContainerDataSource signatureContainerDataSource, SignedContainer signedContainer, boolean isSentToSiva) {
        DataFile dataFile = signedContainer.dataFiles().get(0);
        return signatureContainerDataSource
                .getDocumentFile(signedContainer.file(), dataFile, isSentToSiva)
                .blockingGet();
    }
}
