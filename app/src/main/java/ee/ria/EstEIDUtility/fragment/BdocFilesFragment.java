package ee.ria.EstEIDUtility.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ListView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;

import ee.ria.EstEIDUtility.BuildConfig;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.DataFilesAdapter;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.ContainerUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;

public class BdocFilesFragment extends ListFragment {

    public static final String TAG = "BDOC_FILES_FRAGMENT";

    private DataFilesAdapter filesAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String bdocName = getArguments().getString(Constants.BDOC_NAME);

        Container container = FileUtils.getContainer(getContext().getFilesDir(), bdocName);

        List<DataFile> dataFiles = ContainerUtils.extractDataFiles(container);
        filesAdapter = new DataFilesAdapter(getActivity(), dataFiles);
        setListAdapter(filesAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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

    private void launchFileContentActivity(int position) {
        DataFile file = (DataFile) getListAdapter().getItem(position);
        String fileName = file.fileName();

        File bdocsFilesPath = FileUtils.getBdocsFilesPath(getContext().getFilesDir());
        File attachment = new File(bdocsFilesPath, fileName);

        Uri contentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, attachment);
        getContext().grantUriPermission(BuildConfig.APPLICATION_ID, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
        intent.setDataAndType(contentUri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchFileContentActivity: no handler for this type of file ", e);
            NotificationUtil.showNotification(getActivity(), R.string.file_handler_error, NotificationUtil.NotificationType.ERROR);
        }
    }

}
