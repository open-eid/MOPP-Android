package ee.ria.mopplib;

import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.FileNotFoundException;

public final class SignedContainerTest {

    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("xerces-c-3.2");
        System.loadLibrary("xalanMsg");
        System.loadLibrary("xalan-c");
        System.loadLibrary("xml-security-c");
        System.loadLibrary("digidoc_java");
    }

    private static final String NEW_CONTAINER = "new_container.bdoc";

    @Rule public final ExpectedException exception = ExpectedException.none();
    @Rule public final TemporaryFolder folder = new TemporaryFolder();

    @Test public void open_fileDoesNotExist_throwsFileNotFoundException() throws Exception {
        exception.expect(FileNotFoundException.class);
        SignedContainer.open(folder.newFile(NEW_CONTAINER));
    }

    @Test public void create_dataFilesNull_throwsContainerDataFilesEmptyException() throws Exception {
        exception.expect(ContainerDataFilesEmptyException.class);
        SignedContainer.create(folder.newFile(NEW_CONTAINER), null);
    }

    @Test public void create_dataFilesEmpty_throwsContainerDataFilesEmptyException() throws Exception {
        exception.expect(ContainerDataFilesEmptyException.class);
        SignedContainer.create(folder.newFile(NEW_CONTAINER), ImmutableList.of());
    }
}
