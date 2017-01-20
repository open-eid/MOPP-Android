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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.BuildConfig;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.DataFilesAdapter;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.container.DataFileFacade;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.LayoutUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;

public class ContainerDataFilesFragment extends ListFragment {

    public static final String TAG = ContainerDataFilesFragment.class.getName();

    private DataFilesAdapter filesAdapter;
    private ContainerFacade containerFacade;
    private NotificationUtil notificationUtil;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String containerWorkingPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);
        containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerWorkingPath).build();
        filesAdapter = new DataFilesAdapter(getActivity(), containerFacade, this);
        setListAdapter(filesAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        notificationUtil = new NotificationUtil(getActivity());
        calculateFragmentHeight();
        setEmptyText(getText(R.string.empty_container_files));
        getListView().setOnItemLongClickListener(new FileLongClickListener());
    }

    public void calculateFragmentHeight() {
        LayoutUtils.calculateFragmentHeight(getListView());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchFileContentActivity(position);
    }

    public void addFile(DataFileFacade dataFileFacade) {
        filesAdapter.add(dataFileFacade);
        calculateFragmentHeight();
    }

    private File extractAttachment(String fileName) {
        File attachment = null;
        File containerFile = containerFacade.getContainerFile();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(containerFile))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (fileName.equals(ze.getName())) {
                    File cacheDir = FileUtils.getDataFilesCacheDirectory(getContext());
                    attachment = File.createTempFile(FilenameUtils.removeExtension(fileName), "." + FilenameUtils.getExtension(fileName), cacheDir);
                    try (FileOutputStream out = new FileOutputStream(attachment)) {
                        IOUtils.copy(zis, out);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "extractAttachment: ", e);
        }
        return attachment;
    }

    private void launchFileContentActivity(int position) {
        DataFileFacade dataFileFacade = (DataFileFacade) getListAdapter().getItem(position);
        String fileName = dataFileFacade.getFileName();

        File attachment = extractAttachment(fileName);

        if (attachment == null) {
            notificationUtil.showWarningMessage(getText(R.string.attachment_extract_failed));
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, attachment);
        getContext().grantUriPermission(BuildConfig.APPLICATION_ID, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
        intent.setDataAndType(contentUri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchFileContentActivity: no handler for this type of file ", e);
            notificationUtil.showFailMessage(getText(R.string.file_handler_error));
        }
    }

    class FileLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            DataFileFacade dataFileFacade = (DataFileFacade) getListAdapter().getItem(position);
            String fileName = dataFileFacade.getFileName();
            File attachment = extractAttachment(fileName);

            if (attachment == null) {
                notificationUtil.showWarningMessage(getText(R.string.attachment_extract_failed));
                return false;
            }

            Uri contentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, attachment);
            getContext().grantUriPermission(BuildConfig.APPLICATION_ID, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
            if (mimeType == null) {
                notificationUtil.showWarningMessage(getText(R.string.file_unsharable));
                return false;
            }

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType(mimeType);
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.upload_to)));
            return true;
        }
    }

}
