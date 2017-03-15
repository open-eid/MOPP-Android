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

package ee.ria.DigiDoc.adapter;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.util.DateUtils;

public class ContainerListAdapter extends ArrayAdapter<File> implements Filterable {

    private List<File> allContainers ;
    private List<File> filteredContainers;
    private ContainerFilter containerFilter = new ContainerFilter();
    private AlertDialog confirmDialog;

    static class ViewHolder {
        @BindView(R.id.listDocName) TextView fileName;
        @BindView(R.id.listDocTime) TextView fileCreated;
        @BindView(R.id.removeContainer) ImageView removeContainer;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public ContainerListAdapter(Context context, List<File> allContainers) {
        super(context, 0, allContainers);
        Collections.sort(allContainers, new ContainersListComparator());
        this.allContainers = allContainers;
        this.filteredContainers = allContainers;
    }

    public int getCount() {
        return filteredContainers.size();
    }

    public File getItem(int position) {
        return filteredContainers.get(position);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.container_list_row, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final File containerFile = getItem(position);
        viewHolder.fileName.setText(containerFile.getName());
        viewHolder.fileCreated.setText(getFileLastModified(containerFile.lastModified()));
        viewHolder.removeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.bdoc_remove_confirm_title);

                String confirmMessage = getContext().getString(R.string.bdoc_remove_confirm_message);
                confirmMessage = String.format(confirmMessage, containerFile.getName());

                builder.setMessage(confirmMessage);

                builder.setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File item = getItem(position);
                        boolean deleted = item.delete();
                        if (deleted) {
                            filteredContainers.remove(item);
                            notifyDataSetChanged();
                        }
                    }
                });

                builder.setNegativeButton(R.string.cancel_button, null);
                confirmDialog = builder.create();
                confirmDialog.show();
            }
        });

        return convertView;
    }

    private String getFileLastModified(long lastModified) {
        Date fileModified = new Date(lastModified);

        if (DateUtils.isToday(fileModified)) {
            return DateUtils.TODAY_FORMAT.format(fileModified);
        } else if (DateUtils.isYesterday(fileModified)) {
            return getContext().getString(R.string.activity_browse_containers_yesterday);
        } else if (DateUtils.isCurrentYear(fileModified)) {
            return DateUtils.CURRENT_YEAR_FORMAT.format(fileModified);
        }

        return DateUtils.DATE_FORMAT.format(fileModified);
    }

    public @NonNull Filter getFilter() {
        return containerFilter;
    }

    private class ContainerFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            final List<File> all = allContainers;
            final List<File> filtered = new ArrayList<>();

            for (File ci : all) {
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
            filteredContainers = (ArrayList<File>) results.values;
            notifyDataSetChanged();
        }
    }

    private class ContainersListComparator implements Comparator<File> {

        @Override
        public int compare(File o1, File o2) {
            if (o1.lastModified() > o2.lastModified()) {
                return -1;
            }
            if (o1.lastModified() == o2.lastModified()) {
                return 0;
            }
            return 1;
        }
    }

}
