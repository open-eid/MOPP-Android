package ee.ria.EstEIDUtility;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class MyFileItemRecyclerViewAdapter extends RecyclerView.Adapter<MyFileItemRecyclerViewAdapter.ViewHolder> {

    private final List<FileItem> mValues;
    private final FileItemFragment.OnListFragmentInteractionListener mListener;

    public MyFileItemRecyclerViewAdapter(List<FileItem> items, FileItemFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_fileitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.fileItem = mValues.get(position);
        holder.fileNameView.setText(mValues.get(position).getName());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.fileItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void addItem(FileItem item) {
        mValues.add(item);
        notifyItemInserted(mValues.size() - 1);
    }

    public List<FileItem> getAllItems() {
        return Collections.unmodifiableList(mValues);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;

        public final ImageView iconView;
        public final TextView fileNameView;
        public FileItem fileItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            iconView = (ImageView) view.findViewById(R.id.list_item_file_icon);
            fileNameView = (TextView) view.findViewById(R.id.list_item_filename);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + fileNameView.getText() + "'";
        }
    }
}
