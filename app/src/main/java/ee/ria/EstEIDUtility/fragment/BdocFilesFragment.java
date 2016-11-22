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

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.DataFilesAdapter;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.DataFiles;

public class BdocFilesFragment extends ListFragment {

    public static final String TAG = "BDOC_FILES_FRAGMENT";

    private DataFilesAdapter filesAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String bdocName = getArguments().getString(Constants.BDOC_NAME);
        Container container = FileUtils.getContainer(getActivity().getFilesDir().getAbsolutePath(), bdocName);
        List<DataFile> dataFiles = extractDataFiles(container);
        filesAdapter = new DataFilesAdapter(getActivity(), dataFiles);
        setListAdapter(filesAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setFragmentHeight();
    }

    private void setFragmentHeight() {
        ListView listView = getListView();

        int totalHeight = 0;

        for (int i = 0; i < filesAdapter.getCount(); i++) {
            View mView = filesAdapter.getView(i, null, listView);
            mView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += mView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (filesAdapter.getCount()));
        listView.setLayoutParams(params);
        listView.requestLayout();

        String emptyText = getResources().getString(R.string.empty_container_files);
        setEmptyText(emptyText);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchFileContentActivity(position);
    }

    public void addFile(DataFile dataFile) {
        filesAdapter.add(dataFile);
        setFragmentHeight();
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
