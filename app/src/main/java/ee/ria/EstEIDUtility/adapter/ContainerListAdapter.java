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

package ee.ria.EstEIDUtility.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.domain.ContainerInfo;

public class ContainerListAdapter extends ArrayAdapter<ContainerInfo> implements Filterable {

    private List<ContainerInfo> allContainers ;
    private List<ContainerInfo> filteredContainers;
    private ContainerFilter containerFilter = new ContainerFilter();
    private AlertDialog confirmDialog;

    private static class ViewHolder {
        TextView fileName;
        TextView fileCreated;
        ImageView removeContainer;
    }

    public ContainerListAdapter(Context context, List<ContainerInfo> allContainers) {
        super(context, 0, allContainers);
        this.allContainers = allContainers;
        this.filteredContainers = allContainers;
    }

    public int getCount() {
        return filteredContainers.size();
    }

    public ContainerInfo getItem(int position) {
        return filteredContainers.get(position);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.container_list_row, parent, false);

            viewHolder.fileName = (TextView) convertView.findViewById(R.id.listDocName);
            viewHolder.fileCreated = (TextView) convertView.findViewById(R.id.listDocTime);
            viewHolder.removeContainer = (ImageView) convertView.findViewById(R.id.removeContainer);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ContainerInfo containerInfo = getItem(position);
        viewHolder.fileName.setText(containerInfo.getName());
        viewHolder.fileCreated.setText(containerInfo.getCreated());

        viewHolder.removeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.bdoc_remove_confirm_title);

                String confirmMessage = getContext().getString(R.string.bdoc_remove_confirm_message);
                confirmMessage = String.format(confirmMessage, containerInfo.getName());

                builder.setMessage(confirmMessage);

                builder.setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContainerInfo item = getItem(position);
                        getContext().deleteFile(item.getName());

                        filteredContainers.remove(item);
                        notifyDataSetChanged();
                    }
                });

                builder.setNegativeButton(R.string.cancel_button, null);
                confirmDialog = builder.create();
                confirmDialog.show();
            }
        });

        return convertView;
    }

    public Filter getFilter() {
        return containerFilter;
    }

    private class ContainerFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            final List<ContainerInfo> all = allContainers;
            final List<ContainerInfo> filtered = new ArrayList<>();

            for (ContainerInfo ci : all) {
                if (matchesConstraintIgnoreCase(ci.getName(), constraint)) {
                    filtered.add(ci);
                }
            }

            results.values = filtered;
            results.count = filtered.size();

            return results;
        }

        private boolean matchesConstraintIgnoreCase(String containerName, CharSequence constraint) {
            return containerName.toLowerCase().contains(constraint.toString().toLowerCase());
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredContainers = (ArrayList<ContainerInfo>) results.values;
            notifyDataSetChanged();
        }
    }

}
