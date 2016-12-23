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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import ee.ria.EstEIDUtility.BuildConfig;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.container.DataFileFacade;
import ee.ria.EstEIDUtility.service.ServiceCreatedCallback;
import ee.ria.EstEIDUtility.service.TokenServiceConnection;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Container;
import ee.ria.libdigidocpp.Signature;
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

    private String containerSavePath;

    private ContainerFacade containerFacade;

    private TokenService tokenService;
    private TokenServiceConnection tokenServiceConnection;

    private boolean tokenServiceBound;

    @Override
    public void onStart() {
        super.onStart();
        connectTokenService();
    }

    @Override
    public void onStop() {
        super.onStop();
        unBindTokenService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView, Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_container_details, containerView, false);

        String containerWorkingPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);
        containerSavePath = getArguments().getString(Constants.CONTAINER_SAVE_DIRECTORY_KEY);

        containerFacade = ContainerBuilder
                .aContainer(getContext())
                .fromExistingContainer(containerWorkingPath)
                .build();

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

                Uri uriToFile = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, containerFacade.getContainerFile());
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

        fileInfoTextView.setText(getFormattedFileInfo());
        title.setText(containerFacade.getName());
        body.setText(containerFacade.getName());
    }

    private String getFormattedFileInfo() {
        String format = getContext().getString(R.string.file_info);
        String extension = FilenameUtils.getExtension(containerFacade.getName()).toUpperCase();
        String sizeInKb = FileUtils.getKilobytes(containerFacade.fileSize());
        return String.format(format, extension, sizeInKb);
    }

    private void connectTokenService() {
        tokenServiceConnection = new TokenServiceConnection(getActivity(), new TokenServiceCreatedCallback());
        tokenServiceConnection.connectService();
        tokenServiceBound = true;
    }

    private void unBindTokenService() {
        addSignatureButton.setEnabled(false);
        if (tokenServiceConnection != null && tokenServiceBound) {
            getActivity().unbindService(tokenServiceConnection);
            tokenServiceBound = false;
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
        File cachedDataFile = FileUtils.cacheUriAsDataFile(getContext(), uri);
        if (cachedDataFile == null) {
            return;
        }
        try {
            containerFacade.addDataFile(cachedDataFile);
        } catch (ContainerFacade.DataFileWithSameNameAlreadyExistsException e) {
            NotificationUtil.showWarning(getActivity(), R.string.container_has_file_with_same_name, NotificationUtil.NotificationDuration.LONG);
            return;
        }
        DataFileFacade dataFileFacade = containerFacade.getDataFile(cachedDataFile.getName());
        findDataFilesFragment().addFile(dataFileFacade.getContainerDataFile());
    }

    private class AddFileButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Container container = containerFacade.getContainer();
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
        bundle.putString(Constants.CONTAINER_NAME_KEY, containerFacade.getName());
        bundle.putString(Constants.CONTAINER_PATH_KEY, containerFacade.getAbsolutePath());

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
        bundle.putString(Constants.CONTAINER_NAME_KEY, containerFacade.getName());
        bundle.putString(Constants.CONTAINER_PATH_KEY, containerFacade.getAbsolutePath());

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
            Container container = containerFacade.getContainer();
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
            if (containerFacade.isSignedBy(cert)) {
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
