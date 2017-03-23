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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ee.ria.DigiDoc.BuildConfig;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.adapter.DataFilesAdapter;
import ee.ria.DigiDoc.container.ContainerBuilder;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.container.DataFileFacade;
import ee.ria.DigiDoc.util.Constants;
import ee.ria.DigiDoc.util.FileUtils;
import ee.ria.DigiDoc.util.LayoutUtils;
import ee.ria.DigiDoc.util.NotificationUtil;
import timber.log.Timber;

import static ee.ria.DigiDoc.util.SharingUtils.createChooser;
import static ee.ria.DigiDoc.util.SharingUtils.createSendIntent;
import static ee.ria.DigiDoc.util.SharingUtils.createTargetedSendIntentsForResolvers;
import static ee.ria.DigiDoc.util.SharingUtils.createViewIntent;
import static ee.ria.DigiDoc.util.SharingUtils.createdTargetedViewIntentsForResolvers;
import static ee.ria.DigiDoc.util.SharingUtils.resolvePackageName;

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
        Timber.tag(TAG);
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

    public void updateContainerFile(File containerFile) {
        containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFile).build();
        filesAdapter.updateContainerFile(containerFile);
    }

    private File extractAttachment(String fileName) {
        try {
            File attachment = new File(FileUtils.getDataFilesCacheDirectory(getContext()), fileName);
            containerFacade.getDataFile(fileName).saveFile(attachment.getAbsolutePath());
            return attachment;
        } catch (Exception e) {
            Timber.e(e, "Extracting attachment from container failed");
        }
        return null;
    }

    private void launchFileContentActivity(int position) {
        DataFileFacade dataFileFacade = (DataFileFacade) getListAdapter().getItem(position);
        try {
            startActivity(createChooserOrViewIntentForDataFile(dataFileFacade));
        } catch (FailedToCreateViewIntentException e) {
            Timber.e(e, "Failed to create view intent for container datafile");
            notificationUtil.showFailMessage(e.getMessage());
        }
    }

    private Intent createChooserOrViewIntentForDataFile(DataFileFacade dataFileFacade) {
        String fileName = dataFileFacade.getFileName();
        String mediaType = FileUtils.resolveMimeType(fileName);
        Uri contentUri = createShareUri(fileName);

        if (FileUtils.isContainer(fileName)) {
            return createViewIntent(contentUri, mediaType);
        }

        Intent viewIntent = createViewIntent(contentUri, mediaType);
        PackageManager packageManager = getActivity().getPackageManager();

        List<ResolveInfo> allAvailableResolvers = packageManager.queryIntentActivities(viewIntent, 0);
        ResolveInfo defaultResolver = packageManager.resolveActivity(viewIntent, 0);

        Map<String, Intent> targetedIntents = createdTargetedViewIntentsForResolvers(allAvailableResolvers, contentUri, mediaType);

        if (targetedIntents.isEmpty()) {
            throw new FailedToCreateViewIntentException(getText(R.string.file_handler_error));
        } else if (targetedIntents.size() == 1) {
            return targetedIntents.values().iterator().next();
        } else if (targetedIntents.containsKey(resolvePackageName(defaultResolver))) {
            return targetedIntents.get(resolvePackageName(defaultResolver));
        } else {
            return createChooser(new ArrayList<>(targetedIntents.values()), getText(R.string.open_file_with));
        }
    }

    private Uri createShareUri(String dataFileName) {
        File attachment = extractAttachment(dataFileName);
        if (attachment == null) {
            throw new FailedToCreateViewIntentException(getText(R.string.attachment_extract_failed));
        }
        Uri contentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, attachment);
        getContext().grantUriPermission(BuildConfig.APPLICATION_ID, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return contentUri;
    }

    class FileLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            DataFileFacade dataFileFacade = (DataFileFacade) getListAdapter().getItem(position);
            try {
                startActivity(createChooserOrShareIntentForDataFile(dataFileFacade));
                return true;
            } catch (FailedToCreateViewIntentException e) {
                Timber.e(e, "Failed to create send intent for container datafile");
                notificationUtil.showFailMessage(e.getMessage());
                return false;
            }
        }
    }

    private Intent createChooserOrShareIntentForDataFile(DataFileFacade dataFileFacade) {
        String fileName = dataFileFacade.getFileName();
        String mediaType = FileUtils.resolveMimeType(fileName);
        if (mediaType == null) {
            mediaType = "application/octet-stream";
        }
        Uri contentUri = createShareUri(fileName);
        Intent sendIntent = createSendIntent(contentUri, mediaType);
        PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> allAvailableResolvers = packageManager.queryIntentActivities(sendIntent, 0);
        List<Intent> targetedIntents = createTargetedSendIntentsForResolvers(allAvailableResolvers, contentUri, mediaType);

        if (targetedIntents.isEmpty()) {
            throw new FailedToCreateViewIntentException(getText(R.string.file_handler_error));
        } else if (targetedIntents.size() == 1) {
            return targetedIntents.get(0);
        } else {
            return createChooser(targetedIntents, getText(R.string.upload_to));
        }
    }

    private class FailedToCreateViewIntentException extends RuntimeException {
        FailedToCreateViewIntentException(CharSequence message) {
            super(message.toString());
        }
    }

}
