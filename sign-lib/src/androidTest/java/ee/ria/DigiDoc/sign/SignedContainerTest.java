package ee.ria.DigiDoc.sign;

import android.content.Context;
import android.content.res.AssetManager;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ee.ria.DigiDoc.configuration.ConfigurationManager;
import ee.ria.DigiDoc.configuration.ConfigurationProperties;
import ee.ria.DigiDoc.configuration.loader.CachedConfigurationHandler;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static ee.ria.DigiDoc.sign.SignedContainerSubject.assertThat;

public final class SignedContainerTest {

    static {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        ConfigurationProperties configurationProperties = new ConfigurationProperties(targetContext.getAssets());
        CachedConfigurationHandler cachedConfigurationHandler = new CachedConfigurationHandler(targetContext.getCacheDir());
        ConfigurationManager configurationManager = new ConfigurationManager(targetContext, configurationProperties, cachedConfigurationHandler, "SignedContainerTest-User-Agent");
        SignLib.init(targetContext, "tsa_url", configurationManager.getConfiguration(), "SignedContainerTest-User-Agent");
    }

    private static final String DIR = "signed-containers";
    private static final String METADATA_EXTENSION = "json";

    @Rule public final ExpectedException exception = ExpectedException.none();
    @Rule public final TemporaryFolder folder =
            new TemporaryFolder(InstrumentationRegistry.getContext().getCacheDir());

    @Test public void open_matchesMetadata() throws Exception {
        AssetManager assetManager = InstrumentationRegistry.getTargetContext().getAssets();
        for (String metadataFileName : assetManager.list(DIR)) {
            if (!getFileExtension(metadataFileName).equals(METADATA_EXTENSION)) {
                continue;
            }
            String containerFileName = getNameWithoutExtension(metadataFileName);
            File containerFile = folder.newFile(containerFileName);
            try (
                    InputStream containerInputStream = assetManager
                            .open(DIR + "/" + containerFileName);
                    OutputStream containerOutputStream = new FileOutputStream(containerFile)
            ) {
                ByteStreams.copy(containerInputStream, containerOutputStream);
            }

            assertThat(containerFile)
                    .matchesMetadata(assetManager.open(DIR + "/" + metadataFileName));
        }
    }

    @Test public void open_fileDoesNotExist_throwsIOException() throws Exception {
        exception.expect(IOException.class);
        SignedContainer.open(folder.newFile());
    }

    @Test public void create_dataFilesNull_throwsContainerDataFilesEmptyException() throws
            Exception {
        exception.expect(ContainerDataFilesEmptyException.class);
        SignedContainer.create(folder.newFile(), null);
    }

    @Test public void create_dataFilesEmpty_throwsContainerDataFilesEmptyException() throws
            Exception {
        exception.expect(ContainerDataFilesEmptyException.class);
        SignedContainer.create(folder.newFile(), ImmutableList.of());
    }
}
