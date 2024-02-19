package ee.ria.DigiDoc.android.signature.update.mobileid;

import static ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest.toJson;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PASS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.ACCESS_TOKEN_PATH;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CONFIG_URL;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_HOST;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_PASSWORD;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_PORT;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MANUAL_PROXY_USERNAME;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MID_BROADCAST_ACTION;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.MID_BROADCAST_TYPE_KEY;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.PROXY_SETTING;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SERVICE_FAULT;
import static ee.ria.DigiDoc.mobileid.service.MobileSignConstants.SIGNING_ROLE_DATA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;

import java.util.Locale;
import java.util.UUID;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdMessageException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.mobileid.dto.request.MobileCreateSignatureRequest;
import ee.ria.DigiDoc.mobileid.dto.response.MobileIdServiceResponse;
import ee.ria.DigiDoc.mobileid.dto.response.RESTServiceFault;
import ee.ria.DigiDoc.mobileid.service.MobileSignService;
import ee.ria.DigiDoc.sign.SignLib;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;

public final class MobileIdOnSubscribe implements ObservableOnSubscribe<MobileIdResponse> {

    private final Navigator navigator;
    private final SignedContainer container;
    private final Locale locale;
    private final LocalBroadcastManager broadcastManager;
    private final String uuid;
    private final String personalCode;
    private final String phoneNo;
    private final ProxySetting proxySetting;
    private final ManualProxy manualProxySettings;
    @Nullable private final RoleData roleData;
    private static final String SIGNING_TAG = "MobileId";

    public MobileIdOnSubscribe(Navigator navigator, SignedContainer container, Locale locale,
                               String uuid, String personalCode, String phoneNo,
                               ProxySetting proxySetting, ManualProxy manualProxySettings,
                               @Nullable RoleData roleData) {
        this.navigator = navigator;
        this.container = container;
        this.locale = locale;
        this.broadcastManager = LocalBroadcastManager.getInstance(navigator.activity());
        this.uuid = uuid;
        this.personalCode = personalCode;
        this.phoneNo = phoneNo;
        this.proxySetting = proxySetting;
        this.manualProxySettings = manualProxySettings;
        this.roleData = roleData;
    }

    @Override
    public void subscribe(ObservableEmitter<MobileIdResponse> emitter) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(MID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT -> {
                        RESTServiceFault fault = RESTServiceFault
                                .fromJson(intent.getStringExtra(SERVICE_FAULT));
                        Configuration configuration = context.getResources().getConfiguration();
                        configuration.setLocale(locale);
                        Context configuredContext = context.createConfigurationContext(configuration);
                        if (fault.getStatus() != null) {
                            emitter.onError(MobileIdMessageException
                                    .create(configuredContext, fault.getStatus(), fault.getDetailMessage()));
                        } else {
                            emitter.onError(MobileIdMessageException
                                    .create(configuredContext, fault.getResult(), fault.getDetailMessage()));
                        }
                    }
                    case CREATE_SIGNATURE_CHALLENGE -> {
                        String challenge =
                                intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE);
                        emitter.onNext(MobileIdResponse.challenge(challenge));
                    }
                    case CREATE_SIGNATURE_STATUS -> {
                        MobileIdServiceResponse status =
                                MobileIdServiceResponse.fromJson(
                                        intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                        switch (status.getStatus()) {
                            case USER_CANCELLED ->
                                    emitter.onNext(MobileIdResponse.status(status.getStatus()));
                            case OK -> {
                                emitter.onNext(MobileIdResponse.signature(status.getSignature()));
                                emitter.onNext(MobileIdResponse.success(container));
                                emitter.onComplete();
                            }
                            default -> emitter.onError(MobileIdMessageException
                                    .create(context, status.getStatus(), null));
                        }
                    }
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(MID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        ConfigurationProvider configurationProvider =
                ((ApplicationApp) navigator.activity().getApplication()).getConfigurationProvider();
        String displayMessage = navigator.activity()
                .getString(R.string.signature_update_mobile_id_display_message);
        MobileCreateSignatureRequest request = MobileCreateSignatureRequestHelper
                .create(container, uuid, configurationProvider.getMidRestUrl(),
                        configurationProvider.getMidSkRestUrl(), locale, personalCode, phoneNo, displayMessage);

        Gson gson = new Gson();
        String certBundleList = gson.toJson(configurationProvider.getCertBundle());

        // WorkManager has 10KB data limit. Saving certs to SharedPreferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(navigator.activity());
        sharedPreferences.edit().putString(CERTIFICATE_CERT_BUNDLE, certBundleList).commit();

        UUID uuid = UUID.randomUUID();
        Data inputData = new Data.Builder()
                .putString(CREATE_SIGNATURE_REQUEST, toJson(request))
                .putString(ACCESS_TOKEN_PASS, SignLib.accessTokenPass())
                .putString(ACCESS_TOKEN_PATH, SignLib.accessTokenPath())
                .putString(CONFIG_URL, configurationProvider.getConfigUrl())
                .putString(PROXY_SETTING, proxySetting.name())
                .putString(MANUAL_PROXY_HOST, manualProxySettings.getHost())
                .putInt(MANUAL_PROXY_PORT, manualProxySettings.getPort())
                .putString(MANUAL_PROXY_USERNAME, manualProxySettings.getUsername())
                .putString(MANUAL_PROXY_PASSWORD, manualProxySettings.getPassword())
                .putString(SIGNING_ROLE_DATA, RoleData.toJson(roleData))
                .build();


        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MobileSignService.class)
                .addTag(SIGNING_TAG + uuid)
                .setId(uuid)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(navigator.activity()).enqueueUniqueWork(uuid.toString(), ExistingWorkPolicy.REPLACE, workRequest);
    }
}
