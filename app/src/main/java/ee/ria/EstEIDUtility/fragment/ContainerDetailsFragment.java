package ee.ria.EstEIDUtility.fragment;

import android.app.Activity;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
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
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.EstEIDUtility.BuildConfig;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.domain.FileItem;
import ee.ria.EstEIDUtility.domain.X509Cert;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.ContainerUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.DataFile;
import ee.ria.libdigidocpp.Signature;
import ee.ria.libdigidocpp.Signatures;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.token.tokenservice.token.PinVerificationException;
import ee.ria.token.tokenservice.token.Token;

import static android.app.Activity.RESULT_OK;

public class ContainerDetailsFragment extends Fragment {

    public static final String TAG = "CONTAINER_DETAILS_FRAG";
    private static final int CHOOSE_FILE_REQUEST_ID = 1;

    private EditText title;
    private TextView body;
    private TextView fileInfoTextView;
    private AlertDialog pinDialog;
    private EditText pinText;
    private TextView enterPinText;

    private Button addFileButton;
    private Button addSignatureButton;
    private Button sendButton;
    private ImageView editBdoc;

    private String fileName;
    private String containerWorkingPath;
    private String containerSavePath;

    private File containerFile;

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;

    private boolean tokenServiceBound;

    private void unBindTokenService() {
        addSignatureButton.setEnabled(false);
        if (tokenServiceConnection != null && tokenServiceBound) {
            getActivity().unbindService(tokenServiceConnection);
            tokenServiceBound = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        connectTokenService();
    }

    private void connectTokenService() {
        tokenServiceConnection = new TokenServiceConnection(getActivity(), new TokenServiceCreatedCallback());
        tokenServiceConnection.connectService();
        tokenServiceBound = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        unBindTokenService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView, Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_container_details, containerView, false);

        fileName = getArguments().getString(Constants.CONTAINER_NAME_KEY);
        containerWorkingPath = getArguments().getString(Constants.CONTAINER_WORKING_PATH_KEY, FileUtils.getBdocsPath(getContext().getFilesDir()).getAbsolutePath());
        containerSavePath = getArguments().getString(Constants.CONTAINER_SAVE_PATH_KEY);

        containerFile = getContainerFile();

        createFilesListFragment();
        createSignatureListFragment();

        title = (EditText) fragLayout.findViewById(R.id.listDocName);
        title.setKeyListener(null);

        body = (TextView) fragLayout.findViewById(R.id.listDocLocation);
        fileInfoTextView = (TextView) fragLayout.findViewById(R.id.dbocInfo);

        editBdoc = (ImageView) fragLayout.findViewById(R.id.editBdoc);
        addFileButton = (Button) fragLayout.findViewById(R.id.addFile);
        addSignatureButton = (Button) fragLayout.findViewById(R.id.addSignature);
        sendButton = (Button) fragLayout.findViewById(R.id.sendButton);

        createPinDialog();

        return fragLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addFileButton.setOnClickListener(new AddFileButtonListener());
        addSignatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tokenService.readCert(Token.CertType.CertSign, new SameSignatureCallback());
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri uriToFile = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, containerFile);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uriToFile);
                shareIntent.setType("application/zip");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.upload_to)));
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

        String fileInfo = getContext().getString(R.string.file_info);
        fileInfo = String.format(fileInfo, FilenameUtils.getExtension(fileName).toUpperCase(), FileUtils.getKilobytes(containerFile.length()));

        fileInfoTextView.setText(fileInfo);
        title.setText(fileName);
        body.setText(fileName);
    }

    public void addDataFile(DataFile dataFile) {
        findDataFilesFragment().addFile(dataFile);
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
            signature.setSignatureValue(signatureBytes);
            container.save();
            findSignaturesFragment().addSignature(signature);
            enterPinText.setText(getText(R.string.enter_pin));
            pinText.setText("");
        }

        @Override
        public void onSignError(Exception e, PinVerificationException pinVerificationException) {
            if (pinVerificationException != null) {
                NotificationUtil.showError(getActivity(), R.string.pin_verification_failed, null);
                pinText.setText("");
                tokenService.readRetryCounter(pinVerificationException.getPinType(), new RetryCounterTaskCallback());
            } else {
                Toast.makeText(getActivity(), e.getMessage(), NotificationUtil.NotificationDuration.SHORT.duration).show();
            }
        }
    }

    private class RetryCounterTaskCallback implements RetryCounterCallback {
        @Override
        public void onCounterRead(byte counterByte) {
            enterPinText.setText(String.format(getText(R.string.enter_pin_retries_left).toString(), String.valueOf(counterByte)));
        }

        @Override
        public void cardNotProvided() {
            //TODO: implement behaviour
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_FILE_REQUEST_ID && resultCode == RESULT_OK && data != null && data.getData() != null) {
            addToFileList(data.getData());
        }
    }

    private void browseForFiles() {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(
                Intent.createChooser(intent, getText(R.string.select_file)),
                CHOOSE_FILE_REQUEST_ID);
    }

    private void addToFileList(Uri uri) {
        File cacheDir = FileUtils.getCachePath(getContext().getCacheDir());

        FileItem fileItem = FileUtils.resolveFileItemFromUri(uri, getContext().getContentResolver(), cacheDir.getAbsolutePath());
        if (fileItem == null) {
            return;
        }

        Container container = getContainer();

        String attachedName = fileItem.getName();
        if (ContainerUtils.hasDataFile(container.dataFiles(), attachedName)) {
            NotificationUtil.showWarning(getActivity(), R.string.container_has_file_with_same_name, NotificationUtil.NotificationDuration.LONG);
            return;
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(attachedName));

        File attachedFile = new File(cacheDir, attachedName);

        container.addDataFile(attachedFile.getAbsolutePath(), mimeType);
        container.save(getContainerFile().getAbsolutePath());

        DataFile dataFile = ContainerUtils.getDataFile(container.dataFiles(), attachedName);
        if (dataFile != null) {
            addDataFile(dataFile);
        }
    }

    private Container getContainer() {
        return FileUtils.getContainer(containerWorkingPath, fileName);
    }

    private File getContainerFile() {
        return FileUtils.getFile(containerWorkingPath, fileName);
    }

    private class AddFileButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Container container = getContainer();
            if (container.signatures().size() > 0) {
                NotificationUtil.showError(getActivity(), R.string.add_file_remove_signatures, null);
                return;
            }
            browseForFiles();
        }
    }

    private void createFilesListFragment() {
        ContainerDataFilesFragment filesFragment = findDataFilesFragment();
        if (filesFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString(Constants.CONTAINER_NAME_KEY, fileName);
        bundle.putString(Constants.CONTAINER_WORKING_PATH_KEY, containerWorkingPath);

        filesFragment = new ContainerDataFilesFragment();
        filesFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.filesListLayout, filesFragment, ContainerDataFilesFragment.TAG);
        fragmentTransaction.commit();
    }

    private void createSignatureListFragment() {
        ContainerSignaturesFragment signaturesFragment = findSignaturesFragment();
        if (signaturesFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putString(Constants.CONTAINER_NAME_KEY, fileName);
        bundle.putString(Constants.CONTAINER_WORKING_PATH_KEY, containerWorkingPath);

        signaturesFragment = new ContainerSignaturesFragment();
        signaturesFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.signaturesListLayout, signaturesFragment, ContainerSignaturesFragment.TAG);
        fragmentTransaction.commit();
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
                tokenService.readCert(Token.CertType.CertSign, new CertificateInfoCallback());
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
            Log.d(TAG, "failed to bind token service");
            addSignatureButton.setEnabled(false);
        }

        @Override
        public void disconnected() {
            Log.d(TAG, "token service disconnected");
            addSignatureButton.setEnabled(false);
        }
    }

    class CertificateInfoCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            Container container = getContainer();
            Signature signature = container.prepareWebSignature(cert);
            byte[] dataToSign = signature.dataToSign();
            String pin2 = pinText.getText().toString();
            tokenService.sign(Token.PinType.PIN2, pin2, dataToSign, new SignTaskCallback(container, signature));
        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), NotificationUtil.NotificationDuration.SHORT.duration).show();
        }

    }

    class SameSignatureCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            Container container = getContainer();
            if (isSignedByPerson(container.signatures(), container, cert)) {
                NotificationUtil.showWarning(getActivity(), R.string.already_signed_by_person, null);
                return;
            }

            pinDialog.show();
            pinDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            final Button positiveButton = pinDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
            pinText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

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

        private boolean isSignedByPerson(Signatures signatures, Container container, byte[] cert) {
            Signature signature = container.prepareWebSignature(cert);
            X509Cert x509Cert = new X509Cert(signature.signingCertificateDer());
            for (int i = 0; i < signatures.size(); i++) {
                Signature s = signatures.get(i);
                X509Cert c = new X509Cert(s.signingCertificateDer());
                if (c.getCertificate().equals(x509Cert.getCertificate())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onCertificateError(Exception e) {
        }

    }

    private ContainerDataFilesFragment findDataFilesFragment() {
        return (ContainerDataFilesFragment) getChildFragmentManager().findFragmentByTag(ContainerDataFilesFragment.TAG);
    }

    private ContainerSignaturesFragment findSignaturesFragment() {
        return (ContainerSignaturesFragment) getChildFragmentManager().findFragmentByTag(ContainerSignaturesFragment.TAG);
    }

}
