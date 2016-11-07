package ee.ria.EstEIDUtility;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BdocAdapter extends ArrayAdapter<BdocItem> implements Filterable {

    List<BdocItem> bdocs;
    List<BdocItem> filteredData;
    private BdocFilter bdocFilter = new BdocFilter();

    static class ViewHolder {
        TextView fileName;
        TextView fileCreated;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bdoc_list_row, parent, false);

            viewHolder.fileName = (TextView) convertView.findViewById(R.id.listDocName);
            viewHolder.fileCreated = (TextView) convertView.findViewById(R.id.listDocTime);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BdocItem dboc = getItem(position);
        viewHolder.fileName.setText(dboc.getName());
        viewHolder.fileCreated.setText(dboc.getCreated());

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
