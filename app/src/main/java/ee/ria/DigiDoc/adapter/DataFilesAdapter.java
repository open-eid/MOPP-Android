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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.container.ContainerBuilder;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.container.DataFileFacade;
import ee.ria.DigiDoc.fragment.ContainerDataFilesFragment;
import ee.ria.DigiDoc.fragment.ContainerDetailsFragment;
import ee.ria.DigiDoc.util.FileUtils;
import ee.ria.DigiDoc.util.NotificationUtil;

public class DataFilesAdapter extends ArrayAdapter<DataFileFacade> {

    private ContainerFacade containerFacade;
    private ContainerDataFilesFragment containerDataFilesFragment;

    private NotificationUtil notificationUtil;
    private Activity activity;

    static class ViewHolder {
        @BindView(R.id.fileName) TextView fileName;
        @BindView(R.id.fileSize) TextView fileSize;
        @BindView(R.id.removeFile) ImageView removeFile;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public DataFilesAdapter(Activity activity, ContainerFacade containerFacade, ContainerDataFilesFragment containerDataFilesFragment) {
        super(activity, 0, containerFacade.getDataFiles());
        this.activity = activity;
        this.containerFacade = containerFacade;
        this.containerDataFilesFragment = containerDataFilesFragment;
    }

    public void updateContainerFile(File containerFile) {
        containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFile).build();
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        notificationUtil = new NotificationUtil(activity);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_container_datafiles, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final DataFileFacade dataFileFacade = getItem(position);
        if (dataFileFacade != null) {
            viewHolder.fileName.setText(dataFileFacade.getFileName());
            String fileSizeText = getContext().getString(R.string.file_size);
            String fileSizeWithUnit = Formatter.formatShortFileSize(getContext(), dataFileFacade.getFileSize());
            viewHolder.fileSize.setText(String.format(fileSizeText, fileSizeWithUnit));
            viewHolder.removeFile.setOnClickListener(new RemoveFileListener(position, dataFileFacade.getFileName(), parent.getContext()));
        }
        return convertView;
    }

    private class RemoveFileListener implements View.OnClickListener {

        private final int position;
        private final String fileName;
        private final Context context;

        RemoveFileListener(int position, String fileName, Context context) {
            this.position = position;
            this.fileName = fileName;
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFacade.getAbsolutePath()).build();

            if (containerFacade.isSigned()) {
                notificationUtil.showWarningMessage(getContext().getText(R.string.datafile_delete_not_allowed_signed));
                return;
            }

            if (getCount() == 1) {
                notificationUtil.showWarningMessage(getContext().getText(R.string.datafile_delete_not_allowed_empty));
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
                    containerFacade.removeDataFile(position);
                    FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
                    ContainerDetailsFragment containerDetailsFragment = (ContainerDetailsFragment) fragmentManager.findFragmentByTag(ContainerDetailsFragment.TAG);
                    containerDetailsFragment.updateFileSize();
                    notificationUtil.showSuccessMessage(getContext().getText(R.string.file_removed));
                    DataFileFacade dataFileFacade = getItem(position);
                    remove(dataFileFacade);
                    notifyDataSetChanged();
                    containerDataFilesFragment.calculateFragmentHeight();
                }
            }).setNegativeButton(R.string.cancel_button, null);

            builder.create().show();
        }
    }
}
