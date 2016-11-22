package ee.ria.EstEIDUtility.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import ee.ria.EstEIDUtility.FileItem;
import ee.ria.EstEIDUtility.fragment.BdocDetailFragment;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.fragment.BdocFilesFragment;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
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

    private void addToFileList(Uri uri) {
        FileItem fileItem = FileUtils.resolveFileItemFromUri(uri, getContentResolver(), getFilesDir().getAbsolutePath());
        if (fileItem == null) {
            return;
        }
        Container container = FileUtils.getContainer(getFilesDir().getAbsolutePath(), bdocFileName);

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String mimeType = myMime.getMimeTypeFromExtension(FilenameUtils.getExtension(fileItem.getName()));

        container.addDataFile(getFilesDir().getAbsolutePath() + "/" + fileItem.getName(), mimeType);
        container.save(getFilesDir().getAbsolutePath() + "/" + bdocFileName);

        for (int i = 0; i < container.dataFiles().size(); i++) {
            DataFile dataFile = container.dataFiles().get(i);
            if (dataFile.fileName().equals(fileItem.getName())) {
                BdocDetailFragment bdocDetailFragment = (BdocDetailFragment) getSupportFragmentManager().findFragmentByTag(BdocDetailFragment.TAG);
                BdocFilesFragment bdocFilesFragment = (BdocFilesFragment) bdocDetailFragment.getChildFragmentManager().findFragmentByTag(BdocFilesFragment.TAG);
                bdocFilesFragment.addFile(dataFile);
            }
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
