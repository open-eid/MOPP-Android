package ee.ria.EstEIDUtility.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.AlertItemAdapter;
import ee.ria.EstEIDUtility.util.FileUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class BdocDetailFragment extends Fragment {

    private static final String TAG = "BdocDetailFragment";

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_bdoc_detail, container, false);

        title = (TextView) fragLayout.findViewById(R.id.listDocName);
        body = (TextView) fragLayout.findViewById(R.id.listDocLocation);
        fileInfoTextView = (TextView) fragLayout.findViewById(R.id.dbocInfo);

        createSendDialog();

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

        if (savedInstanceState != null) {
            fileName = savedInstanceState.getString(BrowseContainersActivity.BDOC_NAME);
        } else {
            Intent intent = getActivity().getIntent();
            fileName = intent.getExtras().getString(BrowseContainersActivity.BDOC_NAME);
        }

        File bdocFile = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + fileName);

        createFilesLstFragment();
        createSignatureLstFragment();

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
            showNotification(message, NotificationType.ERROR);
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
        outState.putString(BrowseContainersActivity.BDOC_NAME, fileName);
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

    private void createFilesLstFragment() {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        BdocFilesFragment filesFragment = new BdocFilesFragment();
        fragmentTransaction.add(R.id.filesListLayout, filesFragment, "BDOC_DETAIL_FILES_FRAGMENT");
        fragmentTransaction.commit();
    }

    private void createSignatureLstFragment() {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        BdocSignaturesFragment signaturesFragment = new BdocSignaturesFragment();
        fragmentTransaction.add(R.id.signaturesListLayout, signaturesFragment, "BDOC_DETAIL_SIGNATURES_FRAGMENT");
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
                showNotification(fileName + " " + message, NotificationType.SUCCESS);
            } else {
                String message = getContext().getResources().getString(R.string.upload_to_dropbox_fail);
                showNotification(message, NotificationType.ERROR);
            }
        }
    }

    private void showNotification(String message, NotificationType toastType) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = null;
        switch (toastType) {
            case SUCCESS:
                layout = inflater.inflate(R.layout.success_toast, (ViewGroup) getActivity().findViewById(R.id.success_toast_container));
                break;
            case ERROR:
                layout = inflater.inflate(R.layout.fail_toast, (ViewGroup) getActivity().findViewById(R.id.fail_toast_container));
                break;
        }

        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        Toast toast = new Toast(getActivity());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    private enum NotificationType {
        SUCCESS, ERROR
    }

}
