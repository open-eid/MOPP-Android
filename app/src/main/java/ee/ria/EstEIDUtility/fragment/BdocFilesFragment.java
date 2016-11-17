package ee.ria.EstEIDUtility.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ListView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.adapter.DataFilesAdapter;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;

public class BdocFilesFragment extends ListFragment {

    private static final String TAG = "BdocFilesFragment";
    private String bdocName;

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchFileContentActivity(position);
    }

    private void launchFileContentActivity(int position) {
        DataFile file = (DataFile) getListAdapter().getItem(position);

        String fileName = file.fileName();

        file.saveAs(getActivity().getFilesDir().getAbsolutePath() + "/" + fileName);

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = myMime.getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));

        File f = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + fileName);
        intent.setDataAndType(Uri.fromFile(f), mimeType);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchFileContentActivity: no handler for this type of file ", e);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        bdocName = intent.getExtras().getString(BrowseContainersActivity.BDOC_NAME);

        Container container = Container.open(getActivity().getFilesDir().getAbsolutePath() + "/" + bdocName);

        DataFiles containerDataFiles = container.dataFiles();
        List<DataFile> dataFiles = new ArrayList<>();
        for (int i = 0; i < containerDataFiles.size(); i++) {
            dataFiles.add(containerDataFiles.get(i));
        }

        DataFilesAdapter filesAdapter = new DataFilesAdapter(getActivity(), dataFiles);
        setListAdapter(filesAdapter);

        registerForContextMenu(getListView());
    }

}
