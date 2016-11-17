package ee.ria.EstEIDUtility.fragment;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.adapter.BdocAdapter;
import ee.ria.EstEIDUtility.domain.BdocItem;
import ee.ria.EstEIDUtility.util.DateUtils;

public class BrowseContainersListFragment extends ListFragment {

    private static final String BDOC_EXTENSION = "bdoc";

    private SearchView searchView;

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        launchBdocDetailActivity(position);
    }

    private void launchBdocDetailActivity(int position) {
        BdocItem bdocItem = (BdocItem) getListAdapter().getItem(position);

        Intent intent = new Intent(getActivity(), BdocDetailActivity.class);
        intent.putExtra(BrowseContainersActivity.BDOC_NAME, bdocItem.getName());

        startActivity(intent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<BdocItem> bdocs = getBdocFiles();

        final BdocAdapter bdocAdapter = new BdocAdapter(getActivity(), bdocs);
        setListAdapter(bdocAdapter);

        searchView = (SearchView) getActivity().findViewById(R.id.listSearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                bdocAdapter.getFilter().filter(newText);
                return false;
            }
        });

        registerForContextMenu(getListView());
    }

    private List<BdocItem> getBdocFiles() {
        List<BdocItem> bdocs = new ArrayList<>();

        List<String> bdocFiles = getBdocContainers();
        for (String fileName : bdocFiles) {
            String fileCreated = getFileLastModified(fileName);
            bdocs.add(new BdocItem(fileName, fileCreated));
        }

        return bdocs;
    }

    private String getFileLastModified(String fileName) {
        File file = getActivity().getFileStreamPath(fileName);
        Date fileModified = new Date(file.lastModified());

        if (fileModified == null) {
            return null;
        }
        if (DateUtils.isToday(fileModified)) {
            return DateUtils.TODAY_FORMAT.format(fileModified);
        } else if (DateUtils.isYesterday(fileModified)) {
            return getResources().getString(R.string.activity_browse_containers_yesterday);
        } else if (DateUtils.isCurrentYear(fileModified)) {
            return DateUtils.CURRENT_YEAR_FORMAT.format(fileModified);
        }

        return DateUtils.DATE_FORMAT.format(fileModified);
    }

    private List<String> getBdocContainers() {
        String[] files = getActivity().fileList();
        List<String> bdocs = new ArrayList<>();
        for (String fileName : files) {
            if (FilenameUtils.getExtension(fileName).equals(BDOC_EXTENSION)) {
                bdocs.add(fileName);
            }
        }
        return bdocs;
    }

}
