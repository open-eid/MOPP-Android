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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Locale;

import ee.ria.EstEIDUtility.BuildConfig;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.container.AddedAdesSignatureReceiver;
import ee.ria.EstEIDUtility.container.AsyncAdesSignatureAdder;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.container.DataFileFacade;
import ee.ria.EstEIDUtility.container.SignatureFacade;
import ee.ria.EstEIDUtility.mid.CreateSignatureRequestBuilder;
import ee.ria.EstEIDUtility.mid.MobileSignProgressHelper;
import ee.ria.EstEIDUtility.preferences.accessor.AppPreferences;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.ContainerNameUtils;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.Conf;
import ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse.ProcessStatus;
import ee.ria.mopp.androidmobileid.dto.response.MobileCreateSignatureResponse;
import ee.ria.mopp.androidmobileid.dto.response.ServiceFault;
import ee.ria.mopp.androidmobileid.service.MobileSignService;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.token.tokenservice.callback.SignCallback;
import ee.ria.tokenlibrary.Token;
import ee.ria.tokenlibrary.exception.PinVerificationException;

import static android.app.Activity.RESULT_OK;
import static ee.ria.mopp.androidmobileid.dto.request.MobileCreateSignatureRequest.toJson;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.MID_BROADCAST_ACTION;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.MID_BROADCAST_TYPE_KEY;
import static ee.ria.mopp.androidmobileid.service.MobileSignConstants.SERVICE_FAULT;

public class ContainerDetailsFragment extends Fragment implements AddedAdesSignatureReceiver{

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
    private BroadcastReceiver connectivityBroadcastReceiver;
    private BroadcastReceiver mobileIdBroadcastReceiver;

    private TokenService tokenService;
    private boolean serviceBound;

    private NotificationUtil notificationUtil;
    private MobileSignProgressHelper mobileSignProgressHelper;
    private boolean cardPresent;

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
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), TokenService.class);
        getActivity().bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);
        cardInsertedReceiver = new CardPresentReciever();
        cardRemovedReceiver = new CardAbsentReciever();
        mobileIdBroadcastReceiver = new MobileIdBroadcastReceiver();
        connectivityBroadcastReceiver = new ConnectivityBroadcastReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            getActivity().unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerBroadcastReceivers();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterBroadcastReceivers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerView, Bundle savedInstanceState) {
        View fragLayout = inflater.inflate(R.layout.fragment_container_details, containerView, false);
        notificationUtil = new NotificationUtil(fragLayout);
        mobileSignProgressHelper = new MobileSignProgressHelper(fragLayout);

        String containerWorkingPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);

        containerFacade = ContainerBuilder
                .aContainer(getContext())
                .fromExistingContainer(containerWorkingPath)
                .build();

        createFilesListFragment();
        createSignatureListFragment();

        title = (EditText) fragLayout.findViewById(R.id.docName);
        title.setKeyListener(null);

        body = (TextView) fragLayout.findViewById(R.id.listDocLocation);
        fileInfoTextView = (TextView) fragLayout.findViewById(R.id.dbocInfo);

        editBdoc = (ImageView) fragLayout.findViewById(R.id.editBdoc);
        addFileButton = (Button) fragLayout.findViewById(R.id.addFile);
        addSignatureButton = (Button) fragLayout.findViewById(R.id.addSignature);
        sendButton = (Button) fragLayout.findViewById(R.id.sendButton);
        saveButton = (Button) fragLayout.findViewById(R.id.saveContainer);
        createPinDialog();
        if (containerFacade.hasDataFiles()) {
            enableSigning();
        }
        return fragLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addFileButton.setOnClickListener(new AddFileButtonListener());
        addSignatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cardPresent) {
                    tokenService.readCert(Token.CertType.CertSign, new SameSignatureCallback());
                } else {
                    startMobileSign();
                }
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
        saveButton.setOnClickListener(new SaveFileNameListener());
        title.setOnClickListener(new ChangeFileNameListener());
        editBdoc.setOnClickListener(new ChangeFileNameListener());

        fileInfoTextView.setText(getFormattedFileInfo());
        title.setText(containerFacade.getName());
        body.append(containerFacade.getName());
        checkConnectivity();
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

    @Override
    public void onAdesSignatureAdded(ContainerFacade modifiedContainerFacade) {
        containerFacade = modifiedContainerFacade;
        containerFacade.save();
        SignatureFacade signature = containerFacade.getLastSignature();
        findSignaturesFragment().addSignature(signature);
        notificationUtil.showSuccessMessage(getText(R.string.signature_added));
        enableSigning();
    }

    public void updateFileSize() {
        fileInfoTextView.setText(getFormattedFileInfo());
    }

    private void registerBroadcastReceivers() {
        getActivity().registerReceiver(cardInsertedReceiver, new IntentFilter(ACS.CARD_PRESENT_INTENT));
        getActivity().registerReceiver(cardRemovedReceiver, new IntentFilter(ACS.CARD_ABSENT_INTENT));
        getActivity().registerReceiver(connectivityBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mobileIdBroadcastReceiver, new IntentFilter(MID_BROADCAST_ACTION));
    }

    private void unregisterBroadcastReceivers() {
        if (cardInsertedReceiver != null) {
            getActivity().unregisterReceiver(cardInsertedReceiver);
        }
        if (cardRemovedReceiver != null) {
            getActivity().unregisterReceiver(cardRemovedReceiver);
        }
        if (connectivityBroadcastReceiver != null) {
            getActivity().unregisterReceiver(connectivityBroadcastReceiver);
        }
        if (mobileIdBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mobileIdBroadcastReceiver);
        }
    }

    private void checkConnectivity() {
        CharSequence noInternetConnection = getText(R.string.no_connectivity_warning);
        if (!isInternetAccessAvailable()) {
            notificationUtil.showWarningMessage(noInternetConnection);
        } else {
            notificationUtil.clearWarning(noInternetConnection);
        }
    }

    private boolean isInternetAccessAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
            enableSigning();
        } catch (ContainerFacade.DataFileWithSameNameAlreadyExistsException e) {
            notificationUtil.showWarningMessage(getText(R.string.container_has_file_with_same_name));
            return;
        }
        DataFileFacade dataFileFacade = containerFacade.getDataFile(cachedDataFile.getName());
        findDataFilesFragment().addFile(dataFileFacade);
        fileInfoTextView.setText(getFormattedFileInfo());
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

    private void startMobileSign() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.mobile_id_dialogue, null);
        final EditText mobileNr = (EditText) view.findViewById(R.id.mobile_nr);
        final EditText personalCode = (EditText) view.findViewById(R.id.personal_code);
        final CheckBox rememberMe = (CheckBox) view.findViewById(R.id.remember_me);

        final AppPreferences preferences = AppPreferences.get(getContext());
        mobileNr.setText(preferences.getMobileNumber());
        personalCode.setText(preferences.getPersonalCode());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setPositiveButton(R.string.sign_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String phone = mobileNr.getText().toString();
                    String pCode = personalCode.getText().toString();
                    if (rememberMe.isChecked()) {
                        preferences.updateMobileNumber(phone);
                        preferences.updatePersonalCode(pCode);
                    }
                    String message = getResources().getString(R.string.action_sign) + " " + containerFacade.getName();
                    containerFacade.save();
                    MobileCreateSignatureRequest request = CreateSignatureRequestBuilder
                            .aCreateSignatureRequest()
                            .withContainer(containerFacade)
                            .withIdCode(pCode)
                            .withPhoneNr(phone)
                            .withMessageToDisplay(message)
                            .withLocale(Locale.getDefault())
                            .withLocalSigningProfile(getFutureSignatureProfile())
                            .build();
                    Intent mobileSignIntent = new Intent(getActivity(), MobileSignService.class);
                    mobileSignIntent.putExtra(CREATE_SIGNATURE_REQUEST, toJson(request));
                    mobileSignIntent.putExtra(ACCESS_TOKEN_PASS, Conf.instance().PKCS12Pass());
                    mobileSignIntent.putExtra(ACCESS_TOKEN_PATH, new File(FileUtils.getSchemaCacheDirectory(getContext()), "878252.p12").getAbsolutePath());
                    getActivity().startService(mobileSignIntent);
                    disableSigning();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .setView(view);
        notificationUtil.clearMessages();
        builder.show();
    }

    private void addAdesSignature(String adesSignature) {
        new AsyncAdesSignatureAdder(adesSignature, containerFacade, this).execute();
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

    private String getFutureSignatureProfile() {
        String profile = containerFacade.getExtendedSignatureProfile();
        if (profile == null) {
            profile = AppPreferences.get(getContext()).getSignatureProfile();
        }
        return profile;
    }

    private void enableSigning() {
        addSignatureButton.setEnabled(true);
    }

    private void disableSigning() {
        addSignatureButton.setEnabled(false);
    }

    private ContainerDataFilesFragment findDataFilesFragment() {
        return (ContainerDataFilesFragment) getChildFragmentManager().findFragmentByTag(ContainerDataFilesFragment.TAG);
    }

    private ContainerSignaturesFragment findSignaturesFragment() {
        return (ContainerSignaturesFragment) getChildFragmentManager().findFragmentByTag(ContainerSignaturesFragment.TAG);
    }

    class SaveFileNameListener implements View.OnClickListener {
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

            if (!ContainerNameUtils.hasSupportedContainerExtension(fileName)) {
                title.append(".");
                title.append(AppPreferences.get(getContext()).getContainerFormat());
                fileName = title.getText().toString();
            }

            File file = new File(FileUtils.getContainersDirectory(getContext()), fileName);
            if (file.exists()) {
                notificationUtil.showFailMessage(getText(R.string.file_exists_message));
                return;
            }

            boolean renamed = containerFacade.getContainerFile().renameTo(file);
            if (renamed) {
                notificationUtil.showSuccessMessage(getText(R.string.file_rename_success));
            }
            containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(file).build();
            containerFacade.save();
            saveButton.setVisibility(View.GONE);
            InputMethodManager input = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            input.hideSoftInputFromWindow(title.getWindowToken(), 0);
            title.setCursorVisible(false);
        }
    }

    class ChangeFileNameListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            saveButton.setVisibility(View.VISIBLE);
            title.setCursorVisible(true);
            title.setInputType(EditorInfo.TYPE_CLASS_TEXT);
            InputMethodManager input = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            input.showSoftInput(title, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    class CardPresentReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            cardPresent = true;
            notificationUtil.clearMessages();
            if (containerFacade.hasDataFiles()) {
                enableSigning();
            }
        }
    }

    class CardAbsentReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            cardPresent = false;
            notificationUtil.showWarningMessage(getText(R.string.insert_card_wait));
        }
    }

    class SignTaskCallback implements SignCallback {
        @Override
        public void onSignResponse(byte[] signatureBytes) {
            containerFacade.setSignatureValue(signatureBytes);
            SignatureFacade signatureFacade = containerFacade.getPreparedSignature();
            signatureFacade.extendSignatureProfile(getFutureSignatureProfile());
            containerFacade.save();
            findSignaturesFragment().addSignature(signatureFacade);
            enterPinText.setText(getText(R.string.enter_pin2));
            pinText.setText("");
            notificationUtil.showSuccessMessage(getText(R.string.signature_added));
        }
        @Override
        public void onSignError(Exception e, PinVerificationException pinVerificationException) {
            if (pinVerificationException != null) {
                notificationUtil.showFailMessage(getText(R.string.pin_verification_failed));
                tokenService.readRetryCounter(pinVerificationException.getPinType(), new RetryCounterTaskCallback());
            } else {
                notificationUtil.showFailMessage(getText(R.string.signing_failed));
            }
            pinText.setText("");
        }
    }

    private class RetryCounterTaskCallback implements RetryCounterCallback {
        @Override
        public void onCounterRead(byte counterByte) {
            if (counterByte > 0) {
                enterPinText.setText(String.format(getText(R.string.enter_pin2_retries_left).toString(), String.valueOf(counterByte)));
            } else {
                notificationUtil.showFailMessage(getText(R.string.pin2_blocked));
            }
        }
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

    class CertificateInfoCallback implements CertCallback {
        @Override
        public void onCertificateResponse(byte[] cert) {
            refreshContainerFacade();
            byte[] dataToSign = containerFacade.prepareWebSignature(cert, getFutureSignatureProfile());
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
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkConnectivity();
        }
    }

    class MobileIdBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String broadcastType = intent.getStringExtra(MID_BROADCAST_TYPE_KEY);
            if (isServiceFaultBroadcast(broadcastType)) {
                ServiceFault fault = ServiceFault.fromJson(intent.getStringExtra(SERVICE_FAULT));
                mobileSignProgressHelper.close();
                notificationUtil.showFailMessage(mobileSignProgressHelper.getFaultReasonMessage(fault.getReason()));
                enableSigning();
            }
            else if (isChallengeBroadcast(broadcastType)) {
                MobileCreateSignatureResponse challenge = MobileCreateSignatureResponse.fromJson(intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE));
                mobileSignProgressHelper.showMobileSignProgress(challenge.getChallengeID());
            }
            else if (isStatusBroadcast(broadcastType)) {
                GetMobileCreateSignatureStatusResponse status = GetMobileCreateSignatureStatusResponse.fromJson(intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                if (status.getStatus() == ProcessStatus.OUTSTANDING_TRANSACTION) {
                    mobileSignProgressHelper.updateStatus(status.getStatus());
                } else if (status.getStatus() == ProcessStatus.SIGNATURE) {
                    mobileSignProgressHelper.close();
                    addAdesSignature(status.getSignature());
                } else {
                    mobileSignProgressHelper.close();
                    notificationUtil.showFailMessage(mobileSignProgressHelper.getMessage(status.getStatus()));
                    enableSigning();
                }
            }
        }

        private boolean isStatusBroadcast(String broadcastType) {
            return CREATE_SIGNATURE_STATUS.equals(broadcastType);
        }
        private boolean isChallengeBroadcast(String broadcastType) {
            return CREATE_SIGNATURE_CHALLENGE.equals(broadcastType);
        }
        private boolean isServiceFaultBroadcast(String broadcastType) {
            return SERVICE_FAULT.equals(broadcastType);
        }
    }
}
