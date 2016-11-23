package ee.ria.EstEIDUtility.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.DropboxClientFactory;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.EstEIDUtility.util.UploadFileTask;
import ee.ria.libdigidocpp.Container;

import static android.content.Context.MODE_PRIVATE;

public class BdocDetailFragment extends Fragment {

    public static final String TAG = "BDOC_DETAIL_FRAGMENT";

    private EditText title;
    private TextView body;
    private TextView fileInfoTextView;
    private AlertDialog sendDialog;

    private Button addFileButton;
    private Button sendButton;
    private Button saveButton;
    private ImageView editBdoc;

    private String fileName;
    boolean saveToDropboxClicked;
    private File bdocFile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView,
                             Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_bdoc_detail, containerView, false);

        fileName = getArguments().getString(Constants.BDOC_NAME);
        bdocFile = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + fileName);

        createFilesListFragment();
        createSignatureListFragment();

        title = (EditText) fragLayout.findViewById(R.id.listDocName);
        title.setKeyListener(null);

        saveButton = (Button) fragLayout.findViewById(R.id.saveContainer);

        body = (TextView) fragLayout.findViewById(R.id.listDocLocation);
        fileInfoTextView = (TextView) fragLayout.findViewById(R.id.dbocInfo);

        editBdoc = (ImageView) fragLayout.findViewById(R.id.editBdoc);
        addFileButton = (Button) fragLayout.findViewById(R.id.addFile);
        sendButton = (Button) fragLayout.findViewById(R.id.sendButton);
        createSendDialog();

        return fragLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sendDialog.setTitle(fileName);

        saveButton.setOnClickListener(new SaveButtonListener());
        addFileButton.setOnClickListener(new AddFileButtonListener());
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDialog.show();
            }
        });
        editBdoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                InputMethodManager input = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                input.showSoftInput(title, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        String fileInfo = getContext().getResources().getString(R.string.file_info);
        fileInfo = String.format(fileInfo, FilenameUtils.getExtension(fileName).toUpperCase(), FileUtils.getKilobytes(bdocFile.length()));

        fileInfoTextView.setText(fileInfo);
        title.setText(fileName);
        body.setText(fileName);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (saveToDropboxClicked) {
            String accessToken = getTokenFromPrefs();
            if (accessToken == null) {
                accessToken = Auth.getOAuth2Token();
                if (accessToken != null) {
                    saveTokenToPrefs(accessToken);
                    DropboxClientFactory.init(accessToken);
                } else {
                    NotificationUtil.showNotification(getActivity(), R.string.upload_to_dropbox_auth_failed, NotificationUtil.NotificationType.ERROR);
                    saveToDropboxClicked = false;
                }
            } else {
                DropboxClientFactory.init(accessToken);
                uploadToDropbox();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.BDOC_NAME, fileName);
    }

    private void createSendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(fileName);

        View view = getActivity().getLayoutInflater().inflate(R.layout.send_action_row, null);
        TextView name = (TextView) view.findViewById(R.id.sendText);
        ImageView image = (ImageView) view.findViewById(R.id.sendImg);

        name.setText(getResources().getString(R.string.upload_to_dropbox));
        image.setImageResource(R.drawable.dropbox_android);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String accessToken = getTokenFromPrefs();
                if (accessToken != null) {
                    DropboxClientFactory.init(accessToken);
                    uploadToDropbox();
                } else {
                    Auth.startOAuth2Authentication(getActivity(), getString(R.string.app_key));
                    saveToDropboxClicked = true;
                }
            }
        });

        builder.setView(view);

        builder.setNegativeButton(R.string.close_button, null);
        sendDialog = builder.create();
    }

    //TODO: think through save button behaviour
    private class SaveButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String newFileName = title.getText().toString();
            if (!bdocFile.getName().equals(newFileName)) {
                File to = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + newFileName);
                if (to.exists()) {
                    NotificationUtil.showNotification(getActivity(), R.string.file_exists_message, NotificationUtil.NotificationType.WARNING);
                } else {
                    boolean renamed = bdocFile.renameTo(to);
                    NotificationUtil.showNotification(getActivity(), renamed ? R.string.file_saved : R.string.file_save_failed,
                            renamed ? NotificationUtil.NotificationType.SUCCESS : NotificationUtil.NotificationType.ERROR);
                }
            }
        }
    }

    private class AddFileButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Container container = FileUtils.getContainer(getActivity().getFilesDir().getAbsolutePath(), fileName);
            if (container.signatures().size() > 0) {
                NotificationUtil.showNotification(getActivity(), R.string.add_file_remove_signatures, NotificationUtil.NotificationType.ERROR);
                return;
            }
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            getActivity().startActivityForResult(
                    Intent.createChooser(intent, getResources().getString(R.string.select_file)),
                    BdocDetailActivity.CHOOSE_FILE_REQUEST);
        }
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

    private void dropBoxUploadFail(Exception e) {
        if (e != null) {
            NotificationUtil.showNotification(getActivity(), e.getMessage(), NotificationUtil.NotificationType.ERROR);
        } else {
            NotificationUtil.showNotification(getActivity(), R.string.upload_to_dropbox_fail, NotificationUtil.NotificationType.ERROR);
        }
    }

    private void dropBoxUploadSuccess(long size) {
        String message = getContext().getResources().getString(R.string.upload_to_dropbox_success);
        message = String.format(message, fileName, FileUtils.getKilobytes(size));
        NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.SUCCESS);
    }

    private void uploadToDropbox() {
        new UploadFileTask(getContext(), DropboxClientFactory.getClient(), fileName, new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                dropBoxUploadSuccess(result.getSize());
            }
            @Override
            public void onError(Exception e) {
                dropBoxUploadFail(e);
            }
        }).execute();
    }

    private String getTokenFromPrefs() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.DROPBOX_PREFS, MODE_PRIVATE);
        return prefs.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
    }

    private void saveTokenToPrefs(String accessToken) {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.DROPBOX_PREFS, MODE_PRIVATE);
        prefs.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply();
    }

}
