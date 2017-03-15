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

package ee.ria.DigiDoc.fragment;

import android.app.ListFragment;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.File;
import java.util.List;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.activity.ContainerDetailsActivity;
import ee.ria.DigiDoc.adapter.ContainerListAdapter;
import ee.ria.DigiDoc.util.Constants;
import ee.ria.DigiDoc.util.FileUtils;

public class BrowseContainersListFragment extends ListFragment {

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchContainerDetailActivity(position);
    }

    private void launchContainerDetailActivity(int position) {
        File containerFile = (File) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), ContainerDetailsActivity.class);
        intent.putExtra(Constants.CONTAINER_NAME_KEY, containerFile.getName());
        intent.putExtra(Constants.CONTAINER_PATH_KEY, containerFile.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        List<File> containers = FileUtils.getContainers(getActivity());

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

}
