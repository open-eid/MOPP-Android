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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
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
import ee.ria.EstEIDUtility.activity.BrowseContainersActivity;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.container.DataFileFacade;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.exception.PinVerificationException;

import static android.app.Activity.RESULT_OK;

public class ContainerDetailsFragment extends Fragment {

    public static final String TAG = ContainerDetailsFragment.class.getName();
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
    private Button saveButton;
    private ImageView editBdoc;

    private ContainerFacade containerFacade;

    private BroadcastReceiver cardInsertedReceiver;
    private BroadcastReceiver cardRemovedReceiver;

    private TokenService tokenService;
    private boolean serviceBound;

    private NotificationUtil notificationUtil;
    private boolean cardPresent;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), TokenService.class);
        getActivity().bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);

        cardInsertedReceiver = new CardPresentReciever();
        cardRemovedReceiver = new CardAbsentReciever();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            getActivity().unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    private ServiceConnection tokenServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
            tokenService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView, Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_container_details, containerView, false);

        notificationUtil = new NotificationUtil(fragLayout);

        String containerWorkingPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);

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
        saveButton = (Button) fragLayout.findViewById(R.id.saveContainer);
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

                refreshContainerFacade();

                Uri uriToFile = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, containerFacade.getContainerFile());
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uriToFile);
                shareIntent.setType("application/zip");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.upload_to)));
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshContainerFacade();
                if (!containerFacade.hasDataFiles()) {
                    notificationUtil.showWarningMessage(getText(R.string.save_container_no_files));
                    return;
                }
                String fileName = title.getText().toString();
                if (fileName.isEmpty()) {
                    notificationUtil.showWarningMessage(getText(R.string.file_name_empty_message));
                    return;
                }
                File savePath = FileUtils.getContainerFile(getContext(), fileName);
                if (savePath.getAbsolutePath().equals(containerFacade.getAbsolutePath())) {
                    containerFacade.save();
                } else {
                    if (savePath.exists()) {
                        notificationUtil.showWarningMessage(getText(R.string.file_exists_message));
                        return;
                    }
                    containerFacade.save(savePath);
                }
                Intent intent = new Intent(getActivity(), BrowseContainersActivity.class);
                startActivity(intent);
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
        body.append(containerFacade.getName());
    }

    private void refreshContainerFacade() {
        containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFacade.getContainerFile()).build();
    }

    private String getFormattedFileInfo() {
        String format = getContext().getString(R.string.file_info);
        String extension = FilenameUtils.getExtension(containerFacade.getName()).toUpperCase();
        String sizeInKb = FileUtils.getKilobytes(containerFacade.fileSize());
        return String.format(format, extension, sizeInKb);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(cardInsertedReceiver, new IntentFilter(ACS.CARD_PRESENT_INTENT));
        getActivity().registerReceiver(cardRemovedReceiver, new IntentFilter(ACS.CARD_ABSENT_INTENT));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cardInsertedReceiver != null) {
            getActivity().unregisterReceiver(cardInsertedReceiver);
        }
        if (cardRemovedReceiver != null) {
            getActivity().unregisterReceiver(cardRemovedReceiver);
        }
    }

    class CardPresentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            cardPresent = true;
            if (!containerFacade.getDataFiles().isEmpty()) {
                addSignatureButton.setEnabled(true);
            }
            notificationUtil.clearMessages();
        }

    }

    class CardAbsentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            cardPresent = false;
            addSignatureButton.setEnabled(false);
            notificationUtil.showWarningMessage(getText(R.string.insert_card_wait));
        }
    }

    class SignTaskCallback implements SignCallback {

        @Override
        public void onSignResponse(byte[] signatureBytes) {
            containerFacade.setSignatureValue(signatureBytes);
            //signature.extendSignatureProfile("time-mark"); //TODO: extending doesn't work
            containerFacade.save();
            findSignaturesFragment().addSignature(containerFacade.getPreparedSignature());
            enterPinText.setText(getText(R.string.enter_pin));
            pinText.setText("");
            notificationUtil.showSuccessMessage(getText(R.string.signature_added));
        }

        @Override
        public void onSignError(Exception e, PinVerificationException pinVerificationException) {
            if (pinVerificationException != null) {
                notificationUtil.showFailMessage(getText(R.string.pin_verification_failed));
                pinText.setText("");
                tokenService.readRetryCounter(pinVerificationException.getPinType(), new RetryCounterTaskCallback());
            } else {
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class RetryCounterTaskCallback implements RetryCounterCallback {
        @Override
        public void onCounterRead(byte counterByte) {
            enterPinText.setText(String.format(getText(R.string.enter_pin_retries_left).toString(), String.valueOf(counterByte)));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_FILE_REQUEST_ID && resultCode == RESULT_OK && data != null) {
            ClipData clipData;
            Uri uriData;
            if ((clipData = data.getClipData()) != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    Uri uri = item.getUri();
                    if (uri != null) {
                        addToFileList(uri);
                    }
                }
            } else if ((uriData = data.getData()) != null) {
                addToFileList(uriData);
            }
        }
    }

    private void browseForFiles() {
        Intent intent = new Intent()
                .setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .setAction(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE);

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
            notificationUtil.showWarningMessage(getText(R.string.container_has_file_with_same_name));
            return;
        }
        DataFileFacade dataFileFacade = containerFacade.getDataFile(cachedDataFile.getName());
        findDataFilesFragment().addFile(dataFileFacade);
        if (cardPresent) {
            addSignatureButton.setEnabled(true);
        }
        fileInfoTextView.setText(getFormattedFileInfo());
    }

    private class AddFileButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            refreshContainerFacade();

            if (containerFacade.isSigned()) {
                notificationUtil.showFailMessage(getText(R.string.add_file_remove_signatures));
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

    class CertificateInfoCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            refreshContainerFacade();
            byte[] dataToSign = containerFacade.prepareWebSignature(cert);
            String pin2 = pinText.getText().toString();
            tokenService.sign(Token.PinType.PIN2, pin2, dataToSign, new SignTaskCallback());
        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    class SameSignatureCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            refreshContainerFacade();
            if (containerFacade.isSignedBy(cert)) {
                notificationUtil.showWarningMessage(getText(R.string.already_signed_by_person));
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
                    if (pinText.getText().length() >= 5) {
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
