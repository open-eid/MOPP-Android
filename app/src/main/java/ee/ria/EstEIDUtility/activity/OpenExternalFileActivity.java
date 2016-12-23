package ee.ria.EstEIDUtility.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.fragment.ContainerDetailsFragment;
import ee.ria.EstEIDUtility.fragment.ErrorOpeningFileFragment;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;

public class OpenExternalFileActivity extends EntryPointActivity {

    public static final String TAG = "OPEN_EXTERNAL_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_file);
        try {
            ContainerFacade container = createContainer(getIntent().getData());
            createContainerDetailsFragment(container);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            createErrorFragment();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.clearContainerCache(this);
        FileUtils.clearDataFileCache(this);
    }

    public ContainerFacade createContainer(Uri uri) {
        String fileName = FileUtils.resolveFileName(uri, getContentResolver());
        if (isContainer(fileName)) {
            return ContainerBuilder
                    .aContainer(this)
                    .fromExistingContainer(uri)
                    .build();
        } else {
            return ContainerBuilder
                    .aContainer(this)
                    .withDataFile(uri)
                    .withContainerLocation(ContainerBuilder.ContainerLocation.CACHE)
                    .withContainerName(FilenameUtils.getBaseName(fileName) + "." + Constants.BDOC_EXTENSION)
                    .build();
        }
    }

    private boolean isContainer(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return Arrays.asList("bdoc", "asice").contains(extension);
    }

    private void createContainerDetailsFragment(ContainerFacade container) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ContainerDetailsFragment containerDetailsFragment = findContainerDetailsFragment();
        if (containerDetailsFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Bundle extras = new Bundle();

        extras.putString(Constants.CONTAINER_NAME_KEY, container.getName());
        extras.putString(Constants.CONTAINER_PATH_KEY, container.getAbsolutePath());

        containerDetailsFragment = new ContainerDetailsFragment();
        setTitle(R.string.bdoc_detail_title);
        containerDetailsFragment.setArguments(extras);
        fragmentTransaction.add(R.id.container_layout_holder, containerDetailsFragment, ContainerDetailsFragment.TAG);
        fragmentTransaction.commit();
    }

    private void createErrorFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ErrorOpeningFileFragment errorFragment = findErrorFragment();
        if (errorFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        errorFragment = new ErrorOpeningFileFragment();
        setTitle("Error");
        fragmentTransaction.add(R.id.container_layout_holder, errorFragment, ErrorOpeningFileFragment.TAG);
        fragmentTransaction.commit();
    }

    private ErrorOpeningFileFragment findErrorFragment() {
        return (ErrorOpeningFileFragment) getSupportFragmentManager().findFragmentByTag(ErrorOpeningFileFragment.TAG);
    }

    private ContainerDetailsFragment findContainerDetailsFragment() {
        return (ContainerDetailsFragment) getSupportFragmentManager().findFragmentByTag(ContainerDetailsFragment.TAG);
    }
}
