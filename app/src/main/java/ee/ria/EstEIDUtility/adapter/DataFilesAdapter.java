package ee.ria.EstEIDUtility.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.libdigidocpp.DataFile;

public class DataFilesAdapter extends ArrayAdapter<DataFile> {

    private static class ViewHolder {
        TextView fileName;
        TextView fileSize;
    }

    public DataFilesAdapter(Context context, List<DataFile> dataFiles) {
        super(context, 0, dataFiles);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_bdoc_detail_files, parent, false);

            viewHolder.fileSize = (TextView) convertView.findViewById(R.id.fileSize);
            viewHolder.fileName = (TextView) convertView.findViewById(R.id.fileName);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final DataFile file = getItem(position);
        viewHolder.fileName.setText(file.fileName());

        String fileSizeText = getContext().getResources().getString(R.string.file_size);
        viewHolder.fileSize.setText(String.format(fileSizeText, FileUtils.getKilobytes(file.fileSize())));

        return convertView;
    }

}
