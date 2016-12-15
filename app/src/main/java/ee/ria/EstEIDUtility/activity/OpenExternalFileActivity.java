package ee.ria.EstEIDUtility.activity;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.fragment.ContainerDetailsFragment;
import ee.ria.EstEIDUtility.fragment.ErrorOpeningFileFragment;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.Container;

public class OpenExternalFileActivity extends AppCompatActivity {

    public static final String TAG = "OPEN_EXTERNAL_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_file);
        try {
            String containerName = createContainer(getIntent().getData());
            createContainerDetailsFragment(containerName);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            createErrorFragment();
        }
    }

    private String createContainer(Uri data) {
        String fileName = saveToCache(data);
        if (isContainer(fileName)) {
            return fileName;
        } else {
            String containerName = FilenameUtils.getBaseName(fileName) + "." + Constants.BDOC_EXTENSION;
            Container container = FileUtils.getContainer(getCachePath(), containerName);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
            container.addDataFile(FileUtils.getFile(getCachePath(), fileName).getAbsolutePath(), mimeType);
            container.save(FileUtils.getFile(getCachePath(), containerName).getAbsolutePath());
            return containerName;
        }
    }

    private String saveToCache(Uri data) {
        ContentResolver contentResolver = getContentResolver();
        String fileName = resolveFileName(data, contentResolver);
        try (InputStream input = contentResolver.openInputStream(data)) {
            File file = new File(getCachePath(), fileName);
            OutputStream output = new FileOutputStream(file);
            IOUtils.copy(input, output);
        } catch (IOException e) {
            Log.e(TAG, "resolveFileItemFromUri: ", e);
        }
        return fileName;
    }

    private String getCachePath() {
        return FileUtils.getCachePath(getCacheDir()).getAbsolutePath();
    }

    private boolean isContainer(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return Arrays.asList("bdoc", "asice").contains(extension);
    }

    private String resolveFileName(Uri uri, ContentResolver contentResolver) {
        String uriString = uri.toString();
        if (isContentUri(uriString)) {
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        } else if (isFileUri(uriString)) {
            return new File(uriString).getName();
        }
        return null;
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

    private void createContainerDetailsFragment(String containerName) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ContainerDetailsFragment containerDetailsFragment = findContainerDetailsFragment();
        if (containerDetailsFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Bundle extras = new Bundle();
        extras.putString(Constants.CONTAINER_NAME_KEY, containerName);
        extras.putString(Constants.CONTAINER_WORKING_PATH_KEY, getCachePath());
        extras.putString(Constants.CONTAINER_SAVE_PATH_KEY, getContainerSavePath());

        containerDetailsFragment = new ContainerDetailsFragment();
        setTitle(R.string.bdoc_detail_title);
        containerDetailsFragment.setArguments(extras);
        fragmentTransaction.add(R.id.container_layout_holder, containerDetailsFragment, ContainerDetailsFragment.TAG);
        fragmentTransaction.commit();
    }

    private String getContainerSavePath() {
        return FileUtils.getBdocsPath(getFilesDir()).getAbsolutePath();
    }

    private ContainerDetailsFragment findContainerDetailsFragment() {
        return (ContainerDetailsFragment) getSupportFragmentManager().findFragmentByTag(ContainerDetailsFragment.TAG);
    }

    private boolean isContentUri(String uriString) {
        return uriString.startsWith("content://");
    }

    private boolean isFileUri(String uriString) {
        return uriString.startsWith("file://");
    }
}
