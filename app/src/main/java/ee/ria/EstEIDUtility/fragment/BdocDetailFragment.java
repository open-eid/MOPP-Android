package ee.ria.EstEIDUtility.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.AlertItemAdapter;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Container;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class BdocDetailFragment extends Fragment {

    public static final String TAG = "BDOC_DETAIL_FRAGMENT";

    private static final String APP_KEY = "APP_KEY";
    private static final String APP_SECRET = "APP_SECRET_KEY";
    private static final String DROPBOX_PREFS = "dropbox_prefs";
    private static final String DROPBOX_ACCESS_TOKEN = "dropbox_token";

    private TextView title;
    private TextView body;
    private TextView fileInfoTextView;
    private AlertDialog sendDialog;

    private DropboxAPI<AndroidAuthSession> mDBApi;

    private String fileName;
    boolean saveToDropboxClicked;
    private File bdocFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileName = getArguments().getString(Constants.BDOC_NAME);
        createFilesListFragment();
        createSignatureListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView,
                             Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_bdoc_detail, containerView, false);

        title = (TextView) fragLayout.findViewById(R.id.listDocName);
        body = (TextView) fragLayout.findViewById(R.id.listDocLocation);
        fileInfoTextView = (TextView) fragLayout.findViewById(R.id.dbocInfo);

        Button addFileButton = (Button) fragLayout.findViewById(R.id.addFile);

        bdocFile = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + fileName);

        createSendDialog();

        addFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Container container = FileUtils.getContainer(getActivity().getFilesDir().getAbsolutePath(), fileName);
                if (container.signatures().size() > 0) {
                    NotificationUtil.showNotification(getActivity(),
                            getResources().getString(R.string.add_file_remove_signatures), NotificationUtil.NotificationType.ERROR);
                    return;
                }
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                getActivity().startActivityForResult(intent.createChooser(intent, "Select File to Add"), BdocDetailActivity.CHOOSE_FILE_REQUEST);
            }
        });

        Button sendButton = (Button) fragLayout.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDialog.show();
            }
        });

        return fragLayout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);

        SharedPreferences settings = getActivity().getSharedPreferences(DROPBOX_PREFS, MODE_PRIVATE);
        String token = settings.getString(DROPBOX_ACCESS_TOKEN, null);
        if (token != null) {
            session.setOAuth2AccessToken(token);
        }

        mDBApi = new DropboxAPI<>(session);

        sendDialog.setTitle(fileName);

        String fileInfo = getContext().getResources().getString(R.string.file_info);
        fileInfo = String.format(fileInfo, FilenameUtils.getExtension(fileName).toUpperCase(), FileUtils.getKilobytes(bdocFile.length()));

        fileInfoTextView.setText(fileInfo);
        title.setText(fileName);
        body.setText(fileName);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();

                SharedPreferences.Editor editor = getActivity().getSharedPreferences(DROPBOX_PREFS, MODE_PRIVATE).edit();
                editor.putString(DROPBOX_ACCESS_TOKEN, accessToken);
                editor.commit();
            } catch (IllegalStateException e) {
                Log.i(TAG, "Error authenticating", e);
            }
        }
        if (mDBApi.getSession().authenticationSuccessful() && saveToDropboxClicked) {
            new UploadFileTask(fileName).execute();
            saveToDropboxClicked = false;
        } else if (saveToDropboxClicked) {
            String message = getContext().getResources().getString(R.string.upload_to_dropbox_auth_failed);
            NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.ERROR);
            saveToDropboxClicked = false;
        }
    }

    @Override
    public void onStop() {
        mDBApi.getSession().unlink();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.BDOC_NAME, fileName);
    }

    private void createSendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(fileName);

        final String[] items = new String[] {getResources().getString(R.string.upload_to_dropbox)};
        final Integer[] icons = new Integer[] {R.drawable.dropbox_android};

        ListAdapter adapter = new AlertItemAdapter(getActivity(), items, icons);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mDBApi.getSession().getOAuth2AccessToken() != null) {
                    new UploadFileTask(fileName).execute();
                } else {
                    mDBApi.getSession().startOAuth2Authentication(getActivity());
                    saveToDropboxClicked = true;
                }
            }
        }).setNegativeButton(R.string.close_button, null);
        sendDialog = builder.create();
    }

    private void createFilesListFragment() {
        BdocFilesFragment filesFragment = (BdocFilesFragment) getChildFragmentManager().findFragmentByTag(BdocFilesFragment.TAG);
        if (filesFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString(Constants.BDOC_NAME, fileName);

        filesFragment = new BdocFilesFragment();
        filesFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.filesListLayout, filesFragment, BdocFilesFragment.TAG);
        fragmentTransaction.commit();
    }

    private void createSignatureListFragment() {
        BdocSignaturesFragment signaturesFragment = (BdocSignaturesFragment) getChildFragmentManager().findFragmentByTag(BdocSignaturesFragment.TAG);
        if (signaturesFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString(Constants.BDOC_NAME, fileName);

        signaturesFragment = new BdocSignaturesFragment();
        signaturesFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.signaturesListLayout, signaturesFragment, BdocSignaturesFragment.TAG);
        fragmentTransaction.commit();
    }

    private class UploadFileTask extends AsyncTask<Void, Void, Boolean> {

        String fileName;

        UploadFileTask(String fileName) {
            this.fileName = fileName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try (FileInputStream inputStream = getContext().openFileInput(fileName)) {
                File file = new File(getContext().getFilesDir(), fileName);
                DropboxAPI.Entry response = mDBApi.putFile(fileName, inputStream, file.length(), null, null);
                Log.i(TAG, "The uploaded file's rev is: " + response.rev + " Size:" + response.size);
                return true;
            } catch (IOException | DropboxException e) {
                Log.e(TAG, "doInBackground: ", e);
            }
            return false;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                String message = getContext().getResources().getString(R.string.upload_to_dropbox_success);
                NotificationUtil.showNotification(getActivity(), fileName + " " + message, NotificationUtil.NotificationType.SUCCESS);
            } else {
                String message = getContext().getResources().getString(R.string.upload_to_dropbox_fail);
                NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.ERROR);
            }
        }
    }

}
