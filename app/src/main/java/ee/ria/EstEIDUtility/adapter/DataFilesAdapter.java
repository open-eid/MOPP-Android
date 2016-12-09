package ee.ria.EstEIDUtility.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.fragment.BdocFilesFragment;
import ee.ria.EstEIDUtility.util.ContainerUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.Signature;

public class DataFilesAdapter extends ArrayAdapter<DataFile> {

    private final Activity context;
    private final String bdocFileName;
    private BdocFilesFragment bdocFilesFragment;

    private static class ViewHolder {
        TextView fileName;
        TextView fileSize;
        ImageView removeFile;
    }

    public DataFilesAdapter(Activity context, List<DataFile> dataFiles, String bdocFileName, BdocFilesFragment bdocFilesFragment) {
        super(context, 0, dataFiles);
        this.context = context;
        this.bdocFileName = bdocFileName;
        this.bdocFilesFragment = bdocFilesFragment;
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
            viewHolder.removeFile = (ImageView) convertView.findViewById(R.id.removeFile);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final DataFile file = getItem(position);
        viewHolder.fileName.setText(file.fileName());

        String fileSizeText = getContext().getString(R.string.file_size);
        viewHolder.fileSize.setText(String.format(fileSizeText, FileUtils.getKilobytes(file.fileSize())));

        viewHolder.removeFile.setOnClickListener(new RemoveFileListener(position, file.fileName()));

        return convertView;
    }

    private class RemoveFileListener implements View.OnClickListener {

        private int position;
        private String fileName;

        RemoveFileListener(int position, String fileName) {
            this.position = position;
            this.fileName = fileName;
        }

        @Override
        public void onClick(View v) {
            final Container container = FileUtils.getContainer(context.getFilesDir(), bdocFileName);
            List<Signature> signatures = ContainerUtils.extractSignatures(container);
            if (!signatures.isEmpty()) {
                NotificationUtil.showWarning(context, R.string.datafile_delete_not_allowed_signed, NotificationUtil.NotificationDuration.LONG);
                return;
            }

            if (getCount() == 1) {
                NotificationUtil.showWarning(context, R.string.datafile_delete_not_allowed_empty, NotificationUtil.NotificationDuration.LONG);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.bdoc_remove_confirm_title);

            String confirmMessage = getContext().getString(R.string.file_remove_confirm_message);
            confirmMessage = String.format(confirmMessage, fileName);

            builder.setMessage(confirmMessage);

            builder.setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    container.removeDataFile(position);
                    container.save();
                    DataFile dataFile = getItem(position);
                    remove(dataFile);
                    notifyDataSetChanged();
                    bdocFilesFragment.calculateFragmentHeight();
                }
            }).setNegativeButton(R.string.cancel_button, null);

            builder.create().show();
        }
    }
}
