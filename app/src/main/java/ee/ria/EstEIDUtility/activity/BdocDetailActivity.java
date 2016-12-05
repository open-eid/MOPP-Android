package ee.ria.EstEIDUtility.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.EstEIDUtility.domain.FileItem;
import ee.ria.EstEIDUtility.fragment.BdocDetailFragment;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.fragment.BdocFilesFragment;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.ContainerUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;

public class BdocDetailActivity extends AppCompatActivity {

    private String bdocFileName;

    public static final int CHOOSE_FILE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bdoc_detail);
        createFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            addToFileList(uri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.clearCacheDir(getCacheDir());
    }

    private void addToFileList(Uri uri) {
        File cacheDir = FileUtils.getCachePath(getCacheDir());

        FileItem fileItem = FileUtils.resolveFileItemFromUri(uri, getContentResolver(), cacheDir.getAbsolutePath());
        if (fileItem == null) {
            return;
        }

        Container container = FileUtils.getContainer(getFilesDir(), bdocFileName);

        String attachedName = fileItem.getName();
        if (ContainerUtils.hasDataFile(container.dataFiles(), attachedName)) {
            NotificationUtil.showNotification(this, R.string.container_has_file_with_same_name, NotificationUtil.NotificationType.WARNING);
            return;
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(attachedName));

        File bdocFile = FileUtils.getBdocFile(getFilesDir(), bdocFileName);
        File attachedFile = new File(cacheDir, attachedName);

        container.addDataFile(attachedFile.getAbsolutePath(), mimeType);
        container.save(bdocFile.getAbsolutePath());

        DataFile dataFile = ContainerUtils.getDataFile(container.dataFiles(), attachedName);
        if (dataFile != null) {
            BdocDetailFragment bdocDetailFragment = (BdocDetailFragment) getSupportFragmentManager().findFragmentByTag(BdocDetailFragment.TAG);
            BdocFilesFragment bdocFilesFragment = (BdocFilesFragment) bdocDetailFragment.getChildFragmentManager().findFragmentByTag(BdocFilesFragment.TAG);
            bdocFilesFragment.addFile(dataFile);
        }
    }

    private void createFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        BdocDetailFragment bdocDetailFragment = (BdocDetailFragment) fragmentManager.findFragmentByTag(BdocDetailFragment.TAG);
        if (bdocDetailFragment != null) {
            return;
        }

        Intent intent = getIntent();
        bdocFileName = intent.getExtras().getString(Constants.BDOC_NAME);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Bundle extras = new Bundle();
        extras.putString(Constants.BDOC_NAME, bdocFileName);

        bdocDetailFragment = new BdocDetailFragment();
        setTitle(R.string.bdoc_detail_title);
        bdocDetailFragment.setArguments(extras);
        fragmentTransaction.add(R.id.bdoc_detail, bdocDetailFragment, BdocDetailFragment.TAG);
        fragmentTransaction.commit();
    }

}
