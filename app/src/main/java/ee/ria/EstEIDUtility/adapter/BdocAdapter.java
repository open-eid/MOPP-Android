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

import ee.ria.EstEIDUtility.domain.BdocItem;
import ee.ria.EstEIDUtility.R;

public class BdocAdapter extends ArrayAdapter<BdocItem> implements Filterable {

    private List<BdocItem> bdocs;
    private List<BdocItem> filteredData;
    private BdocFilter bdocFilter = new BdocFilter();
    private AlertDialog confirmDialog;

    private static class ViewHolder {
        TextView fileName;
        TextView fileCreated;
        ImageView removeBdoc;
    }

    public BdocAdapter(Context context, List<BdocItem> bdocs) {
        super(context, 0, bdocs);
        this.bdocs = bdocs;
        this.filteredData = bdocs;
    }

    public int getCount() {
        return filteredData.size();
    }

    public BdocItem getItem(int position) {
        return filteredData.get(position);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bdoc_list_row, parent, false);

            viewHolder.fileName = (TextView) convertView.findViewById(R.id.listDocName);
            viewHolder.fileCreated = (TextView) convertView.findViewById(R.id.listDocTime);
            viewHolder.removeBdoc = (ImageView) convertView.findViewById(R.id.removeBdoc);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final BdocItem dboc = getItem(position);
        viewHolder.fileName.setText(dboc.getName());
        viewHolder.fileCreated.setText(dboc.getCreated());

        viewHolder.removeBdoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.bdoc_remove_confirm_title);

                String confirmMessage = getContext().getResources().getString(R.string.bdoc_remove_confirm_message);
                confirmMessage = String.format(confirmMessage, dboc.getName());

                builder.setMessage(confirmMessage);

                builder.setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BdocItem item = getItem(position);
                        getContext().deleteFile(item.getName());

                        filteredData.remove(item);
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
        return bdocFilter;
    }

    private class BdocFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            constraint = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final List<BdocItem> list = bdocs;

            int count = list.size();
            final List<BdocItem> result = new ArrayList<>(count);

            BdocItem filterableBdoc;

            for (int i = 0; i < count; i++) {
                filterableBdoc = list.get(i);
                if (filterableBdoc.getName().toLowerCase().contains(constraint)) {
                    result.add(filterableBdoc);
                }
            }

            results.values = result;
            results.count = result.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<BdocItem>) results.values;
            notifyDataSetChanged();
        }
    }

}
