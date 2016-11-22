package ee.ria.EstEIDUtility.fragment;

import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.AlertItemAdapter;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.DropboxClientFactory;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.EstEIDUtility.util.UploadFileTask;
import ee.ria.libdigidocpp.Container;

import static android.content.Context.MODE_PRIVATE;

public class BdocDetailFragment extends Fragment {

    public static final String TAG = "BDOC_DETAIL_FRAGMENT";

    private TextView title;
    private TextView body;
    private TextView fileInfoTextView;
    private AlertDialog sendDialog;

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
                getActivity().startActivityForResult(
                        Intent.createChooser(intent, getResources().getString(R.string.select_file)),
                        BdocDetailActivity.CHOOSE_FILE_REQUEST);
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

        if (saveToDropboxClicked) {
            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.DROPBOX_PREFS, MODE_PRIVATE);
            String accessToken = prefs.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
            if (accessToken == null) {
                accessToken = Auth.getOAuth2Token();
                if (accessToken != null) {
                    prefs.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply();
                    DropboxClientFactory.init(accessToken);
                } else {
                    String message = getContext().getResources().getString(R.string.upload_to_dropbox_auth_failed);
                    NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.ERROR);
                    saveToDropboxClicked = false;
                }
            } else {
                DropboxClientFactory.init(accessToken);
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

        final String[] items = new String[] {getResources().getString(R.string.upload_to_dropbox)};
        final Integer[] icons = new Integer[] {R.drawable.dropbox_android};

        ListAdapter adapter = new AlertItemAdapter(getActivity(), items, icons);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = getActivity().getSharedPreferences(Constants.DROPBOX_PREFS, MODE_PRIVATE);
                String accessToken = prefs.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
                if (accessToken != null) {
                    DropboxClientFactory.init(accessToken);
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
                } else {
                    Auth.startOAuth2Authentication(getActivity(), getString(R.string.app_key));
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

    private void dropBoxUploadFail(Exception e) {
        if (e != null) {
            String message = e.getMessage();
            NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.ERROR);
        } else {
            String message = getContext().getResources().getString(R.string.upload_to_dropbox_fail);
            NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.ERROR);
        }
    }

    private void dropBoxUploadSuccess(long size) {
        String message = getContext().getResources().getString(R.string.upload_to_dropbox_success);
        message = String.format(message, fileName, FileUtils.getKilobytes(size));
        NotificationUtil.showNotification(getActivity(), message, NotificationUtil.NotificationType.SUCCESS);
    }

}
