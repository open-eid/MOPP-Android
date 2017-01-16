/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.activity.ContainerDetailsActivity;
import ee.ria.EstEIDUtility.adapter.ContainerListAdapter;
import ee.ria.EstEIDUtility.domain.ContainerInfo;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.DateUtils;
import ee.ria.EstEIDUtility.util.FileUtils;

public class BrowseContainersListFragment extends ListFragment {

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchBdocDetailActivity(position);
    }

    private void launchBdocDetailActivity(int position) {
        ContainerInfo containerInfo = (ContainerInfo) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), ContainerDetailsActivity.class);
        intent.putExtra(Constants.CONTAINER_NAME_KEY, containerInfo.getName());
        intent.putExtra(Constants.CONTAINER_PATH_KEY, containerInfo.getPath().getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<ContainerInfo> bdocs = getBdocFiles();

        final ContainerListAdapter containerListAdapter = new ContainerListAdapter(getActivity(), bdocs);
        setListAdapter(containerListAdapter);

        SearchView searchView = (SearchView) getActivity().findViewById(R.id.listSearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                containerListAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    private List<ContainerInfo> getBdocFiles() {
        List<ContainerInfo> bdocs = new ArrayList<>();
        List<File> bdocFiles = getBdocContainers();
        for (File file : bdocFiles) {
            String fileCreated = getFileLastModified(file);
            bdocs.add(new ContainerInfo(file, fileCreated));
        }
        return bdocs;
    }

    private String getFileLastModified(File file) {
        Date fileModified = new Date(file.lastModified());

        if (DateUtils.isToday(fileModified)) {
            return DateUtils.TODAY_FORMAT.format(fileModified);
        } else if (DateUtils.isYesterday(fileModified)) {
            return getString(R.string.activity_browse_containers_yesterday);
        } else if (DateUtils.isCurrentYear(fileModified)) {
            return DateUtils.CURRENT_YEAR_FORMAT.format(fileModified);
        }

        return DateUtils.DATE_FORMAT.format(fileModified);
    }

    private List<File> getBdocContainers() {
        File containersPath = FileUtils.getContainersDirectory(getActivity());
        File[] containerFiles = containersPath.listFiles();

        if (containerFiles == null) {
            return Collections.emptyList();
        }

        List<File> bdocs = new ArrayList<>();
        for (File bdoc : containerFiles) {
            if (FilenameUtils.getExtension(bdoc.getName()).equals(Constants.BDOC_EXTENSION)) {
                bdocs.add(bdoc);
            }
        }
        return bdocs;
    }

}
