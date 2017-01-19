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

import java.io.File;
import java.util.ArrayList;
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
        launchContainerDetailActivity(position);
    }

    private void launchContainerDetailActivity(int position) {
        ContainerInfo containerInfo = (ContainerInfo) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), ContainerDetailsActivity.class);
        intent.putExtra(Constants.CONTAINER_NAME_KEY, containerInfo.getName());
        intent.putExtra(Constants.CONTAINER_PATH_KEY, containerInfo.getPath().getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<ContainerInfo> containers = getContainers();

        final ContainerListAdapter containerListAdapter = new ContainerListAdapter(getActivity(), containers);
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

    private List<ContainerInfo> getContainers() {
        List<ContainerInfo> containers = new ArrayList<>();
        List<File> containerFiles = FileUtils.getContainers(getActivity());
        for (File file : containerFiles) {
            String fileCreated = getFileLastModified(file);
            containers.add(new ContainerInfo(file, fileCreated));
        }
        return containers;
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

}
