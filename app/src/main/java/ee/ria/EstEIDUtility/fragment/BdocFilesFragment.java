package ee.ria.EstEIDUtility.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ListView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.adapter.DataFilesAdapter;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;

public class BdocFilesFragment extends ListFragment {

    private static final String TAG = "BdocFilesFragment";

    private DataFilesAdapter filesAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        String bdocName = intent.getExtras().getString(BrowseContainersActivity.BDOC_NAME);

        Container container = Container.open(getActivity().getFilesDir().getAbsolutePath() + "/" + bdocName);

        List<DataFile> dataFiles = extractDataFiles(container);

        filesAdapter = new DataFilesAdapter(getActivity(), dataFiles);
        setListAdapter(filesAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ListView listView = getListView();
        int totalHeight = 0;

        for (int i = 0; i < filesAdapter.getCount(); i++) {
            View mView = filesAdapter.getView(i, null, listView);
            mView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += mView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (filesAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchFileContentActivity(position);
    }

    private List<DataFile> extractDataFiles(Container container) {
        if (container == null) {
            return Collections.emptyList();
        }
        DataFiles containerDataFiles = container.dataFiles();
        List<DataFile> dataFiles = new ArrayList<>();
        for (int i = 0; i < containerDataFiles.size(); i++) {
            dataFiles.add(containerDataFiles.get(i));
        }
        return dataFiles;
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

}
