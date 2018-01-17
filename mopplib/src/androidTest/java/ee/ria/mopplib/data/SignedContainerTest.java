package ee.ria.mopplib.data;

import android.support.test.InstrumentationRegistry;

import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.FileNotFoundException;

public final class SignedContainerTest {

    @Rule public final ExpectedException exception = ExpectedException.none();
    @Rule public final TemporaryFolder folder =
            new TemporaryFolder(InstrumentationRegistry.getContext().getCacheDir());

    @Test public void open_fileDoesNotExist_throwsFileNotFoundException() throws Exception {
        exception.expect(FileNotFoundException.class);
        SignedContainer.open(folder.newFile());
    }

    @Test public void create_dataFilesNull_throwsContainerDataFilesEmptyException() throws Exception {
        exception.expect(ContainerDataFilesEmptyException.class);
        SignedContainer.create(folder.newFile(), null);
    }

    @Test public void create_dataFilesEmpty_throwsContainerDataFilesEmptyException() throws Exception {
        exception.expect(ContainerDataFilesEmptyException.class);
        SignedContainer.create(folder.newFile(), ImmutableList.of());
    }
}
