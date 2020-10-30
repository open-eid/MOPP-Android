package ee.ria.DigiDoc.android.main.sharing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.Constants;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import timber.log.Timber;

import static com.jakewharton.rxbinding2.support.v7.widget.RxToolbar.navigationClicks;

public final class SharingScreenView extends CoordinatorLayout {

    private final Toolbar toolbarView;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public SharingScreenView(Context context) {
        super(context);
        inflate(context, R.layout.sharing_choose_container_file, this);
        toolbarView = findViewById(R.id.toolbar);
        navigator = Application.component(context).navigator();

        disposables = new ViewDisposables();


        showFiles(getContainerFiles(new File(getContext().getFilesDir(), Constants.DIR_SIGNATURE_CONTAINERS)));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    private ArrayList<File> getContainerFiles(File dir) {
        ArrayList<File> containerFilesList = new ArrayList<>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        getContainerFiles(file);
                    } else {
                        containerFilesList.add(file);
                    }
                }
            }
        }
        return containerFilesList;
    }

    private ArrayList<String> getContainerFileNames(ArrayList<File> containerFiles) {
        ArrayList<String> containerFileNames = new ArrayList<>();
        for (int i = 0; i < containerFiles.size(); i++) {
            containerFileNames.add(containerFiles.get(i).getName());
        }

        return containerFileNames;
    }

    private void showFiles(ArrayList<File> containerFilesList) {
        ListView listView = findViewById(R.id.sharingScreenList);

        ArrayList<String> containerFileNames = getContainerFileNames(containerFilesList);

        ArrayAdapter<String> containerFileNamesList = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_activated_1, containerFileNames);
        listView.setAdapter(containerFileNamesList);

        showFilesListener(listView, containerFilesList);
    }

    private void showFilesListener(ListView listView, ArrayList<File> containerFilesList) {
        listView.setOnItemClickListener((parent, view, position, id) -> {

            File requestFile = containerFilesList.get(position);

            try {
                Activity activity = (Activity) getContext();

                Uri fileProviderUri = FileProvider.getUriForFile(getContext(),
                        activity.getString(R.string.file_provider_authority),
                        requestFile);

                Intent returnIntent = new Intent(Intent.ACTION_VIEW);

                if (fileProviderUri != null) {
                    returnIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    returnIntent.setDataAndType(fileProviderUri, getFileExtensionFromFileUri(fileProviderUri));
                    activity.setResult(Activity.RESULT_OK, returnIntent);

                    returnToParentApplication(activity, returnIntent);
                } else {
                    returnIntent.setDataAndType(null, "");
                    activity.setResult(Activity.RESULT_CANCELED, returnIntent);
                    activity.finish();
                }
            } catch (IllegalArgumentException e) {
                Timber.e(e, "File selecting failed");
            }
        });
    }

    private void returnToParentApplication(Activity activity, Intent intent) {
        if (isIntentWithExtraReferrer(activity)) {
          restartAppWithIntent(intent);
        } else {
            activity.finish();
        }
    }

    private boolean isIntentWithExtraReferrer(Activity activity) {
        return activity.getIntent().getExtras().get(Intent.EXTRA_REFERRER) != null &&
                activity.getIntent().getExtras().get(Intent.EXTRA_REFERRER).equals(R.string.application_name);
    }

    private String getFileExtensionFromFileUri(Uri uri) {
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
        if (fileExtension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        } else {
            return "*/*";
        }
    }

    private void restartAppWithIntent(Intent intent) {
        PackageManager packageManager = getContext().getPackageManager();
        Intent packageIntent = packageManager.getLaunchIntentForPackage(getContext().getPackageName());
        ComponentName componentName = packageIntent.getComponent();
        Intent restartIntent = Intent.makeRestartActivityTask(componentName);
        restartIntent.setAction(intent.getAction());
        restartIntent.setDataAndType(intent.getData(), intent.getType());
        getContext().startActivity(restartIntent);
    }
}