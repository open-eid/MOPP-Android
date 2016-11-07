package ee.ria.EstEIDUtility;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.SearchView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BrowseContainersListFragment extends ListFragment {

    private static final String BDOC_EXTENSION = "bdoc";
    private static final SimpleDateFormat TODAY_FORMAT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat CURRENT_YEAR_FORMAT = new SimpleDateFormat("dd.MMM");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private SearchView searchView;
    List<BdocItem> bdocs;
    BdocAdapter bdocAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        bdocs = createBdocs();

        bdocAdapter = new BdocAdapter(getActivity(), bdocs);
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

    private List<BdocItem> createBdocs() {
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
            return TODAY_FORMAT.format(fileModified);
        } else if (DateUtils.isYesterday(fileModified)) {
            return getResources().getString(R.string.activity_browse_containers_yesterday);
        } else if (DateUtils.isCurrentYear(fileModified)) {
            return CURRENT_YEAR_FORMAT.format(fileModified);
        }

        return DATE_FORMAT.format(fileModified);
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
