package ee.ria.mopplib.data;

import android.support.test.InstrumentationRegistry;

import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;

import ee.ria.mopplib.Files;
import ee.ria.mopplib.R;

import static com.google.common.truth.Truth.assertThat;

public final class SignedContainerTest {

    @Rule public final ExpectedException exception = ExpectedException.none();
    @Rule public final TemporaryFolder folder =
            new TemporaryFolder(InstrumentationRegistry.getContext().getCacheDir());

    @Test public void open_fileDoesNotExist_throwsFileNotFoundException() throws Exception {
        exception.expect(FileNotFoundException.class);
        SignedContainer.open(folder.newFile());
    }

    @Test public void open_valid() throws Exception {
        File containerFile = Files.copyRaw(folder.newFile(),
                R.raw.baltic_mou_digital_signing_est_lt_lv);
        SignedContainer container = SignedContainer.open(containerFile);
        assertThat(container).isNotNull();
        assertThat(container.dataFiles()).hasSize(1);
        assertThat(container.signatures()).hasSize(3);
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
