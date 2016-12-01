package ee.ria.EstEIDUtility.fragment;

import android.app.Activity;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.activity.BdocDetailActivity;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.DropboxClientFactory;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.EstEIDUtility.util.UploadFileTask;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
import ee.ria.token.tokenservice.Token;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.util.Util;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.token.tokenservice.exception.PinVerificationException;

import static android.content.Context.MODE_PRIVATE;

public class BdocDetailFragment extends Fragment {

    public static final String TAG = "BDOC_DETAIL_FRAGMENT";

    private EditText title;
    private TextView body;
    private TextView fileInfoTextView;
    private AlertDialog sendDialog;
    private AlertDialog pinDialog;
    private EditText pinText;
    private TextView enterPinText;

    private Button addFileButton;
    private Button addSignatureButton;
    private Button sendButton;
    private Button saveButton;
    private ImageView editBdoc;

    private String fileName;
    boolean saveToDropboxClicked;
    private File bdocFile;

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;

    @Override
    public void onStart() {
        super.onStart();
        connectTokenService();
    }

    private void connectTokenService() {
        ServiceCreatedCallback callback = new TokenServiceCreatedCallback();
        tokenServiceConnection = new TokenServiceConnection(getActivity(), callback);
        tokenServiceConnection.connectService();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (tokenServiceConnection != null) {
            getActivity().unbindService(tokenServiceConnection);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView, Bundle savedInstanceState) {
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
        addSignatureButton = (Button) fragLayout.findViewById(R.id.addSignature);
        sendButton = (Button) fragLayout.findViewById(R.id.sendButton);
        createSendDialog();
        createPinDialog();

        return fragLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sendDialog.setTitle(fileName);

        saveButton.setOnClickListener(new SaveButtonListener());
        addFileButton.setOnClickListener(new AddFileButtonListener());
        addSignatureButton.setOnClickListener(new AddSignatureButtonListener());
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

    class CertificateInfoCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            String pin2 = pinText.getText().toString();
            Container container = FileUtils.getContainer(getContext().getFilesDir(), fileName);
            Signature signature = container.prepareWebSignature(cert);
            byte[] dataToSign = signature.dataToSign();
            SignCallback callback = new SignTaskCallback(container, signature);
            tokenService.sign(Token.PinType.PIN2, pin2, dataToSign, callback);
        }

        @Override
        public void onCertificateError(String reason) {
            //TODO: implement behaviour
            Toast.makeText(getActivity(), reason, NotificationUtil.NotificationDuration.SHORT.duration).show();
        }
    }

    class SignTaskCallback implements SignCallback {
        Signature signature;
        Container container;

        SignTaskCallback(Container container, Signature signature) {
            this.signature = signature;
            this.container = container;
        }

        @Override
        public void onSignResponse(byte[] signatureBytes) {
            Log.d(TAG, "onSignResponse: " + Util.toHex(signatureBytes));
            signature.setSignatureValue(signatureBytes);
            container.save();
            BdocDetailFragment bdocDetailFragment = (BdocDetailFragment) getActivity().getSupportFragmentManager().findFragmentByTag(BdocDetailFragment.TAG);
            BdocSignaturesFragment bdocSignaturesFragment = (BdocSignaturesFragment) bdocDetailFragment.getChildFragmentManager().findFragmentByTag(BdocSignaturesFragment.TAG);
            bdocSignaturesFragment.addSignature(signature);
        }

        @Override
        public void onSignError(Exception e, PinVerificationException pinVerificationException) {
            if (pinVerificationException != null) {
                NotificationUtil.showNotification(getActivity(), R.string.pin_verification_failed, NotificationUtil.NotificationType.ERROR);
                pinText.setText("");
                RetryCounterCallback callback =  new RetryCounterTaskCallback();
                tokenService.readRetryCounter(Token.PinType.PIN2, callback);
            } else {
                Toast.makeText(getActivity(), e.getMessage(), NotificationUtil.NotificationDuration.SHORT.duration).show();
            }
        }
    }

    private class RetryCounterTaskCallback implements RetryCounterCallback {
        @Override
        public void onCounterRead(byte counterByte) {
            String text = enterPinText.getText().toString();
            String result = text + " (" + String.format(getResources().getString(R.string.pin_retries_left), String.valueOf(counterByte)) + ")";
            enterPinText.setText(result);
        }
    }

    private class AddSignatureButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            pinDialog.show();
            pinDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            final Button positiveButton = pinDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
            pinText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (pinText.getText().length() == 5) {
                        positiveButton.setEnabled(true);
                    } else if (positiveButton.isEnabled()) {
                        positiveButton.setEnabled(false);
                    }
                }
            });
        }
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
                    sendDialog.hide();
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
            Container container = FileUtils.getContainer(getContext().getFilesDir(), fileName);
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

    private void createPinDialog() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.enter_pin, null);

        enterPinText = (TextView) view.findViewById(R.id.enterPin);
        pinText = (EditText) view.findViewById(R.id.pin);
        pinText.setHint(Token.PinType.PIN2.name());
        InputFilter[] inputFilters = {new InputFilter.LengthFilter(5)};
        pinText.setFilters(inputFilters);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(R.string.sign_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CertCallback callback = new CertificateInfoCallback();
                tokenService.readCert(Token.CertType.CertSign, callback);
            }
        }).setNegativeButton(R.string.cancel, null);
        builder.setView(view);
        pinDialog = builder.create();
    }

    class TokenServiceCreatedCallback implements ServiceCreatedCallback {

        @Override
        public void created(Service service) {
            tokenService = (TokenService) service;
            addSignatureButton.setEnabled(true);
        }

        @Override
        public void failed() {
            Log.d(TAG, "failed to bind toke service");
            addSignatureButton.setEnabled(false);
        }

        @Override
        public void disconnected() {
            tokenService = null;
            addSignatureButton.setEnabled(false);
        }
    }

}
